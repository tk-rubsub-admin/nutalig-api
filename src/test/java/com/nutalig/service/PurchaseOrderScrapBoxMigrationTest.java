package com.nutalig.service;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderScrapBoxMigrationTest {
    @Test void migratesLegacyDetailsAndStoresOneSharedBoxWithMultipleContents() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:scrap_" + UUID.randomUUID() + ";MODE=MySQL")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE purchase_order_delivery (id BIGINT PRIMARY KEY)");
                sql.execute("CREATE TABLE purchase_order_delivery_detail (id BIGINT PRIMARY KEY, purchase_order_delivery_id BIGINT NOT NULL, quantity DECIMAL(18,5), FOREIGN KEY (purchase_order_delivery_id) REFERENCES purchase_order_delivery(id) ON DELETE CASCADE)");
                sql.execute("INSERT INTO purchase_order_delivery VALUES (1)");
                sql.execute("INSERT INTO purchase_order_delivery_detail VALUES (1,1,10)");
            }
            var db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            var liquibase = new Liquibase("db/changelog/20261024-001-add-delivery-scrap-boxes.yaml", new ClassLoaderResourceAccessor(), db);
            liquibase.update("");
            try (var sql = connection.createStatement()) {
                try (var legacy = sql.executeQuery("SELECT scrap_box_id FROM purchase_order_delivery_detail WHERE id=1")) {
                    assertTrue(legacy.next()); assertNull(legacy.getObject(1));
                }
                sql.execute("INSERT INTO purchase_order_delivery_scrap_box (id,purchase_order_delivery_id,net_weight_kg,cbm_m3) VALUES (1,1,5,0.024)");
                sql.execute("INSERT INTO purchase_order_delivery_detail (id,purchase_order_delivery_id,quantity,scrap_box_id) VALUES (2,1,5,1),(3,1,20,1)");
                try (var box = sql.executeQuery("SELECT cbm_m3 FROM purchase_order_delivery_scrap_box")) {
                    assertTrue(box.next()); assertEquals(0, new BigDecimal("0.024").compareTo(box.getBigDecimal(1))); assertFalse(box.next());
                }
                try (var count = sql.executeQuery("SELECT COUNT(*) FROM purchase_order_delivery_detail WHERE scrap_box_id=1")) {
                    assertTrue(count.next()); assertEquals(2, count.getInt(1));
                }
                assertThrows(java.sql.SQLException.class, () -> sql.execute("UPDATE purchase_order_delivery_detail SET scrap_box_id=999 WHERE id=2"));
                sql.execute("DELETE FROM purchase_order_delivery WHERE id=1");
                try (var count = sql.executeQuery("SELECT COUNT(*) FROM purchase_order_delivery_scrap_box")) {
                    assertTrue(count.next()); assertEquals(0, count.getInt(1));
                }
            }
        }
    }
}
