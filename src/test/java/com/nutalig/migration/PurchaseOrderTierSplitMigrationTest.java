package com.nutalig.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderTierSplitMigrationTest {
    @Test
    void addsNullableSplitReferenceAndStoresCompleteSeaMethodsWithoutChangingExistingRows() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:po_split;MODE=MySQL")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE rfq_tier_split (id BIGINT PRIMARY KEY)");
                sql.execute("INSERT INTO rfq_tier_split VALUES (10)");
                sql.execute("CREATE TABLE purchase_order_detail (id BIGINT PRIMARY KEY, shipping_method VARCHAR(10))");
                sql.execute("INSERT INTO purchase_order_detail VALUES (1, 'LAND')");
            }
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase("db/changelog/20261007-001-add-purchase-order-tier-split-reference.yaml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
                liquibase.update(new Contexts(), new LabelExpression());
                try (var sql = connection.createStatement()) {
                    sql.execute("INSERT INTO purchase_order_detail VALUES (2, 'SEA_SHARE_FCL_40HQ', 10)");
                    try (var rows = sql.executeQuery("SELECT shipping_method, rfq_tier_split_id FROM purchase_order_detail WHERE id=1")) {
                        assertTrue(rows.next());
                        assertEquals("LAND", rows.getString(1));
                        assertNull(rows.getObject(2));
                    }
                    sql.execute("DELETE FROM rfq_tier_split WHERE id=10");
                    try (var rows = sql.executeQuery("SELECT rfq_tier_split_id FROM purchase_order_detail WHERE id=2")) {
                        assertTrue(rows.next());
                        assertNull(rows.getObject(1));
                    }
                }
            }
        }
    }
}
