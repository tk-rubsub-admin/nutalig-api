package com.nutalig.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SupplierShippingModeMigrationTest {
    @Test
    void upgradesLegacyMastersAndFreezesExistingPurchaseOrderCodes() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:shipping_modes;MODE=MySQL")) {
            try (Statement sql = connection.createStatement()) {
                sql.execute("CREATE TABLE supplier_shipping (id BIGINT PRIMARY KEY, shipping_method VARCHAR(20), car_code VARCHAR(255))");
                sql.execute("CREATE TABLE purchase_order (purchase_order_no VARCHAR(50) PRIMARY KEY, supplier_shipping_id BIGINT)");
                sql.execute("INSERT INTO supplier_shipping VALUES (1, 'SEA', 'TB001'), (2, 'LAND', 'TR001')");
                sql.execute("INSERT INTO purchase_order VALUES ('PO-SEA', 1), ('PO-LAND', 2), ('PO-NONE', NULL)");
            }
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/20261006-002-add-supplier-shipping-mode-and-po-car-code-snapshot.yaml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
                try (Statement sql = connection.createStatement()) {
                    try (ResultSet masters = sql.executeQuery("SELECT shipping_mode FROM supplier_shipping")) {
                        while (masters.next()) {
                            assertEquals("STANDARD", masters.getString(1));
                        }
                    }
                    sql.execute("UPDATE supplier_shipping SET car_code = 'TB999' WHERE id = 1");
                    try (ResultSet po = sql.executeQuery("SELECT car_code_snapshot FROM purchase_order WHERE purchase_order_no = 'PO-SEA'")) {
                        assertTrue(po.next());
                        assertEquals("TB001", po.getString(1));
                    }
                    try (ResultSet po = sql.executeQuery("SELECT car_code_snapshot FROM purchase_order WHERE purchase_order_no = 'PO-NONE'")) {
                        assertTrue(po.next());
                        assertNull(po.getString(1));
                    }
                    sql.execute("INSERT INTO supplier_shipping (id, shipping_method, car_code, shipping_mode) VALUES (3, 'SEA', 'TZ001', 'FCL')");
                    sql.execute("INSERT INTO supplier_shipping (id, shipping_method, car_code) VALUES (4, 'SEA', 'TB002')");
                    try (ResultSet master = sql.executeQuery("SELECT shipping_mode FROM supplier_shipping WHERE id = 4")) {
                        assertTrue(master.next());
                        assertEquals("STANDARD", master.getString(1));
                    }
                }
                liquibase.update(new Contexts(), new LabelExpression());
                try (Statement sql = connection.createStatement();
                     ResultSet po = sql.executeQuery("SELECT car_code_snapshot FROM purchase_order WHERE purchase_order_no = 'PO-SEA'")) {
                    assertTrue(po.next());
                    assertEquals("TB001", po.getString(1));
                }
            }
        }
    }
}
