package com.buymore;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductDealsTest {
    private static Double compareAt(Document product) {
        Object value = product.get("compare_at_price");
        return value == null ? null : ((Number) value).doubleValue();
    }

    private static ProductController.Product product(String compareAt) {
        return new ProductController.Product("BM-TEST-1", "Desk Lamp", "A lamp for the desk",
            new BigDecimal("39.00"), "office", List.of("everyday"),
            Map.of("brand", "BuyMore"),
            List.of(Map.of("sku", "BM-TEST-1-G", "color", "graphite", "stock", 10)),
            true, compareAt == null ? null : new BigDecimal(compareAt));
    }

    @Test void productApiIncludesTheCompareAtPriceWhenPresent() {
        var doc = ProductController.document(product("49.00"));
        assertEquals(Double.valueOf(49.0), compareAt(doc), "the storefront needs compare_at_price to render the strike-through price");
    }

    @Test void productWithoutADealHasNoCompareAtPrice() {
        var doc = ProductController.document(product(null));
        assertNull(compareAt(doc), "full-price products must not carry a deal field");
    }

    @Test void adminUpdateKeepsTheExistingDealWhenTheFormOmitsIt() {
        var old = new Document("price", 39.0).append("compare_at_price", 49.0);
        var replacement = ProductController.document(product(null));
        ProductController.preserveCompareAt(replacement, old);
        assertEquals(Double.valueOf(49.0), compareAt(replacement), "editing a product in the admin catalog must not wipe its deal");
    }

    @Test void adminUpdateAcceptsANewDealWhenProvided() {
        var old = new Document("price", 39.0).append("compare_at_price", 49.0);
        var replacement = ProductController.document(product("59.00"));
        ProductController.preserveCompareAt(replacement, old);
        assertEquals(Double.valueOf(59.0), compareAt(replacement));
    }
}
