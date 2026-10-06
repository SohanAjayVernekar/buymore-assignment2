package com.buymore;

import java.util.*;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/** Seeds a deep demo catalog (200 products, some on offer) behind the SEED_200 flag. */
@Component
public class ProductSeeder implements ApplicationRunner {
    static final int TARGET = 200;
    static final String[][] CATALOG = {
        {"peripherals","Wireless Mouse","Mechanical Keyboard","HD Webcam","Monitor Stand","Compact Keyboard"},
        {"audio","Noise Cancelling Headphones","Wireless Earbuds","Portable Speaker","Studio Microphone","Desk Soundbar"},
        {"cables","USB-C Cable","Braided HDMI Cable","Charging Dock","Ethernet Cable","Cable Organizer"},
        {"office","Desk Organizer","Notebook Set","Ergonomic Footrest","Whiteboard Small","Pen Cup"},
        {"lighting","Desk Lamp LED","Monitor Light Bar","Reading Lamp","Mood Light Strip","Clip Lamp"},
        {"accessories","Laptop Sleeve","Travel Pouch","Mouse Pad XL","Water Bottle Steel","Backpack Day"},
        {"storage","Flash Drive 128GB","External SSD 1TB","Card Reader","Hard Case 2TB","Memory Stick"},
        {"kitchen","Coffee Mug Steel","Insulated Tumbler","Snack Box","Bottle Opener","Tea Kettle Mini"}
    };
    static final String[] MODIFIERS = {"", "Pro", "Mini", "Lite", "Essential", "Compact", "Series 2", "Max"};
    static final String[] COLORS = {"graphite","sand","mint","midnight","clay","sky"};

    final MongoTemplate mongo; final int seedProducts; final boolean legacyFlag;
    ProductSeeder(MongoTemplate mongo, @Value("${buymore.seed-products:0}") int seedProducts, @Value("${buymore.seed200:false}") boolean legacyFlag) {
        this.mongo = mongo; this.seedProducts = seedProducts; this.legacyFlag = legacyFlag;
    }

    @Override public void run(ApplicationArguments args) {
        var target = target(seedProducts, legacyFlag);
        if (target <= 0) return;
        var existing = mongo.find(new Query(), Document.class, "products");
        var need = missing(target, existing.size());
        if (need == 0) return;
        int position = 0;
        for (var doc : existing) {
            if (applyDeal(doc, position++)) mongo.save(doc, "products");
        }
        var start = nextIndex(existing);
        for (var doc : generate(need, start)) mongo.insert(doc, "products");
    }

    static int target(int seedProducts, boolean legacyFlag) {
        return seedProducts > 0 ? seedProducts : (legacyFlag ? 200 : 0);
    }

    static int missing(int target, long existing) {
        return (int) Math.max(0L, target - existing);
    }

    /** First free BM-D index, so a top-up never collides with a previous batch. */
    static int nextIndex(java.util.Collection<Document> existing) {
        int max = 0;
        for (var doc : existing) {
            var sku = doc.get("sku");
            if (sku == null || !sku.toString().startsWith("BM-D")) continue;
            try { max = Math.max(max, Integer.parseInt(sku.toString().substring(4))); }
            catch (NumberFormatException ignored) { /* not a numbered batch sku */ }
        }
        return max + 1;
    }

    static List<Document> generate(int count) { return generate(count, 1); }

    static List<Document> generate(int count, int startIndex) {
        var products = new ArrayList<Document>(count);
        for (int i = 0; i < count; i++) {
            int index = startIndex + i;
            String sku = String.format("BM-D%03d", index);
            String[] entry = CATALOG[i % CATALOG.length];
            String category = entry[0];
            String modifier = MODIFIERS[(i / CATALOG.length) % MODIFIERS.length];
            String title = (modifier + " " + entry[1 + (i / (CATALOG.length * MODIFIERS.length)) % (entry.length - 1)]).trim();
            String color = COLORS[i % COLORS.length];
            double price = Math.round((14 + (i * 37) % 170) * 100) / 100.0 + 0.99;
            var doc = new Document("sku", sku)
                .append("title", title)
                .append("description", "Thoughtfully designed for your everyday setup. " + title + " in " + color + ".")
                .append("price", price)
                .append("category", category)
                .append("tags", List.of("everyday", "essentials", category))
                .append("active", true)
                .append("attributes", Map.of("color", color, "brand", "BuyMore"))
                .append("variants", List.of(Map.of("sku", sku + "-G", "color", color, "stock", 100)));
            applyDeal(doc, index - 1);
            products.add(doc);
        }
        return products;
    }

    /** Every fifth product goes on offer; an existing deal is never overwritten. */
    static boolean applyDeal(Document product, int position) {
        if (position % 5 != 0 || product.get("compare_at_price") != null) return false;
        double price = ((Number) product.get("price")).doubleValue();
        product.put("compare_at_price", Math.round(price * 1.25 * 100) / 100.0);
        return true;
    }
}
