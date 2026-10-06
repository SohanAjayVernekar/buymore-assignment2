package com.buymore;

import java.util.*;
import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class Startup implements ApplicationRunner {
    final MongoTemplate mongo; final SearchService search; final boolean seed;
    Startup(MongoTemplate mongo,SearchService search,@Value("${buymore.seed}") boolean seed){this.mongo=mongo;this.search=search;this.seed=seed;}
    @Override public void run(ApplicationArguments args) {
        mongo.indexOps("products").ensureIndex(new Index().on("sku",Sort.Direction.ASC).unique());
        if(seed && mongo.getCollection("products").countDocuments()==0) {
            String[][] products={{"Wireless Keyboard","peripherals","79"},{"Wireless Mouse Pro","peripherals","49"},{"Noise Cancelling Headphones","audio","129"},{"Wireless Earbuds","audio","59"},{"USB-C Hub 7-in-1","cables","45"},{"Desk Lamp LED","office","39"},{"USB-C Cable","cables","15"},{"Portable Speaker","audio","69"},{"Desk Organizer","office","25"},{"HD Webcam","peripherals","89"},{"Mechanical Keyboard","peripherals","99"},{"Monitor Stand","office","55"}};
            for(int i=0;i<products.length;i++) {
                String[] p=products[i];String sku=String.format("BM-J%03d",i+1);
                mongo.insert(new Document("sku",sku).append("title",p[0]).append("description","Thoughtfully designed for your everyday setup.").append("price",Double.valueOf(p[2])).append("category",p[1]).append("tags",List.of("everyday","essentials")).append("active",true).append("attributes",Map.of("color","graphite","brand","BuyMore")).append("variants",List.of(Map.of("sku",sku+"-G","color","graphite","stock",100))),"products");
            }
        }
        search.initialize();
    }
}
