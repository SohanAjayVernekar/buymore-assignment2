package com.buymore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductSeederCountTest {
    @Test void plansExactlyTheMissingProducts() {
        assertEquals(2276, ProductSeeder.missing(2500, 224), "should top the catalog up to the target");
        assertEquals(2500, ProductSeeder.missing(2500, 0));
    }

    @Test void neverSeedsPastTheTargetOrBelowZero() {
        assertEquals(0, ProductSeeder.missing(2500, 2500), "a full catalog must be left alone");
        assertEquals(0, ProductSeeder.missing(2500, 3000), "an over-target catalog must be left alone");
        assertEquals(0, ProductSeeder.missing(0, 0), "a zero target disables seeding");
    }

    @Test void topUpSkusContinueAfterTheExistingRange() {
        var existing = java.util.List.of(new org.bson.Document("sku", "BM-001"), new org.bson.Document("sku", "BM-D200"));
        assertEquals(201, ProductSeeder.nextIndex(existing), "must not reuse BM-D001..BM-D200");
        var batch = ProductSeeder.generate(3, ProductSeeder.nextIndex(existing));
        assertEquals(java.util.List.of("BM-D201", "BM-D202", "BM-D203"),
            batch.stream().map(d -> (String) d.get("sku")).toList());
    }

    @Test void targetComesFromSeedProductsUnlessOnlyTheLegacyFlagIsSet() {
        assertEquals(2500, ProductSeeder.target(2500, false));
        assertEquals(200, ProductSeeder.target(0, true), "the legacy SEED_200 flag still means 200");
        assertEquals(0, ProductSeeder.target(0, false), "no flag means no seeding");
    }
}
