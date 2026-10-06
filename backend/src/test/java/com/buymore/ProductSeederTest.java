package com.buymore;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductSeederTest {
    private static Double compareAt(Document product) {
        Object value = product.get("compare_at_price");
        return value == null ? null : ((Number) value).doubleValue();
    }

    @Test void generatesTwoHundredUniqueProductsForTheStorefront() {
        var products = ProductSeeder.generate(200);
        assertEquals(200, products.size());
        var skus = new HashSet<String>();
        for (var p : products) {
            var sku = (String) p.get("sku");
            assertNotNull(sku, "sku is required for the catalog table");
            assertTrue(sku.matches("BM-D\\d{3}"), "unexpected sku format: " + sku);
            assertTrue(skus.add(sku), "duplicate sku: " + sku);
            var title = (String) p.get("title");
            assertNotNull(title, "title is required by the storefront");
            assertFalse(title.isBlank(), "title must not be blank");
            assertNotNull(p.get("description"), "description is required by the storefront");
            assertNotNull(p.get("category"), "category is required for category filters");
            assertTrue(((Number) p.get("price")).doubleValue() > 0, "price must be positive");
            assertNotNull(p.get("tags"), "tags are required by the product record");
            assertNotNull(p.get("attributes"), "attributes are required by the product record");
            assertNotNull(p.get("variants"), "variants are required by checkout");
            assertEquals(Boolean.TRUE, p.get("active"), "seeded products must be visible in the shop");
        }
    }

    @Test void everyFifthProductCarriesADealAboveTheSellingPrice() {
        var products = ProductSeeder.generate(200);
        long deals = products.stream().filter(p -> p.get("compare_at_price") != null).count();
        assertEquals(40, deals, "a fifth of 200 products should be on offer");
        for (var p : products) {
            var compareAt = p.get("compare_at_price");
            if (compareAt == null) continue;
            assertTrue(((Number) compareAt).doubleValue() > ((Number) p.get("price")).doubleValue(),
                "compare-at price must be above the selling price so the discount is real");
        }
    }

    @Test void dealRuleOnlyMarksNewProductsAndNeverOverwritesAnExistingDeal() {
        var fullPrice = new Document("price", 40.0);
        ProductSeeder.applyDeal(fullPrice, 0);
        assertEquals(Double.valueOf(50.0), compareAt(fullPrice));

        var notOnOffer = new Document("price", 40.0);
        ProductSeeder.applyDeal(notOnOffer, 1);
        assertNull(compareAt(notOnOffer), "only every fifth product goes on offer");

        var alreadyMarked = new Document("price", 40.0).append("compare_at_price", 60.0);
        ProductSeeder.applyDeal(alreadyMarked, 0);
        assertEquals(Double.valueOf(60.0), compareAt(alreadyMarked), "an existing deal must never be overwritten");
    }
}
