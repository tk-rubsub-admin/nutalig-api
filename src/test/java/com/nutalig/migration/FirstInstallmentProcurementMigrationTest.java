package com.nutalig.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FirstInstallmentProcurementMigrationTest {
    @Test
    void backfillsOnlyOrdersWithAnApprovedFullyPaidFirstInstallmentAndRunsOnce() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:first_installment_procurement;MODE=MySQL")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE sales_order (sales_order_no VARCHAR(50) PRIMARY KEY, status VARCHAR(30), procurement_status VARCHAR(30), updated_date TIMESTAMP)");
                sql.execute("CREATE TABLE invoice (invoice_no VARCHAR(50) PRIMARY KEY, sales_order_no VARCHAR(50), status VARCHAR(30), subtotal DECIMAL(18,5), discount DECIMAL(18,5), amount DECIMAL(18,5), grand_total DECIMAL(18,5), customer_payment_term VARCHAR(50), created_date TIMESTAMP)");
                sql.execute("CREATE TABLE invoice_payment (invoice_no VARCHAR(50), status VARCHAR(30), amount DECIMAL(18,5))");
            }
            seed(connection, "NTL-SO2026090029", "DEP30_BBS", "6210.00", "NOT_READY", "CREATED");
            seed(connection, "SO-50", "DEP50", "10350.00", "NOT_READY", "CREATED");
            seed(connection, "SO-35", "DEP35_35_30_BBS", "7245.00", "NOT_READY", "CREATED");
            seed(connection, "SO-100", "AFS100", "20700.00", "NOT_READY", "CREATED");
            seed(connection, "SO-WRONG", "DEP30_BBS", "10350.00", "NOT_READY", "CREATED");
            seed(connection, "SO-PENDING", "DEP30_BBS", "6210.00", "NOT_READY", "CREATED");
            seed(connection, "SO-PARTIAL", "DEP30_BBS", "6210.00", "NOT_READY", "CREATED");
            seed(connection, "SO-LATER", "DEP35_35_30_BBS", "7245.00", "NOT_READY", "CREATED");
            seed(connection, "SO-OVERRIDE", "DEP30_BBS", "6210.00", "READY_FOR_PO_OVERRIDE", "CREATED");
            seed(connection, "SO-PO", "DEP30_BBS", "6210.00", "PO_CREATED", "CREATED");
            seed(connection, "SO-CANCELLED", "DEP30_BBS", "6210.00", "NOT_READY", "CANCELLED");
            seed(connection, "SO-IGNORED", "DEP30_BBS", "6210.00", "NOT_READY", "CREATED");
            seed(connection, "SO-VAT", "DEP30_BBS", "6210.00", "NOT_READY", "CREATED");
            try (var sql = connection.createStatement()) {
                sql.execute("UPDATE invoice_payment SET status='PENDING' WHERE invoice_no='INV-SO-PENDING'");
                sql.execute("UPDATE invoice_payment SET amount=3000 WHERE invoice_no='INV-SO-PARTIAL'");
                sql.execute("INSERT INTO invoice VALUES ('EARLIER-LATER', 'SO-LATER', 'ISSUED', 20700, 0, 7245, 7245, 'DEP35_35_30_BBS', '2026-10-01 00:00:00')");
                sql.execute("INSERT INTO invoice VALUES ('EARLIER-IGNORED', 'SO-IGNORED', 'VOID', 20700, 0, 6210, 6210, 'DEP30_BBS', '2026-10-01 00:00:00')");
                sql.execute("UPDATE invoice SET grand_total=6644.70 WHERE invoice_no='INV-SO-VAT'");
                sql.execute("UPDATE invoice_payment SET amount=6644.70 WHERE invoice_no='INV-SO-VAT'");
            }
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase(
                    "db/changelog/20261009-001-backfill-first-installment-ready-for-po.yaml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
                Map<String, String> expected = Map.ofEntries(
                        Map.entry("NTL-SO2026090029", "READY_FOR_PO"),
                        Map.entry("SO-50", "READY_FOR_PO"), Map.entry("SO-35", "READY_FOR_PO"),
                        Map.entry("SO-100", "READY_FOR_PO"), Map.entry("SO-WRONG", "NOT_READY"),
                        Map.entry("SO-PENDING", "NOT_READY"), Map.entry("SO-PARTIAL", "NOT_READY"),
                        Map.entry("SO-LATER", "NOT_READY"), Map.entry("SO-OVERRIDE", "READY_FOR_PO_OVERRIDE"),
                        Map.entry("SO-PO", "PO_CREATED"), Map.entry("SO-CANCELLED", "NOT_READY"),
                        Map.entry("SO-IGNORED", "READY_FOR_PO"), Map.entry("SO-VAT", "READY_FOR_PO"));
                try (var sql = connection.createStatement(); var rows = sql.executeQuery("SELECT sales_order_no, procurement_status FROM sales_order")) {
                    int count = 0;
                    while (rows.next()) {
                        assertEquals(expected.get(rows.getString(1)), rows.getString(2), rows.getString(1));
                        count++;
                    }
                    assertEquals(expected.size(), count);
                }
                try (var sql = connection.createStatement()) {
                    sql.execute("UPDATE sales_order SET procurement_status='NOT_READY' WHERE sales_order_no='NTL-SO2026090029'");
                }
                liquibase.update(new Contexts(), new LabelExpression());
                try (var sql = connection.createStatement(); var rows = sql.executeQuery("SELECT procurement_status FROM sales_order WHERE sales_order_no='NTL-SO2026090029'")) {
                    assertTrue(rows.next());
                    assertEquals("NOT_READY", rows.getString(1), "The backfill must not run again");
                }
            }
        }
    }

    private void seed(Connection connection, String so, String term, String amount, String procurement, String status) throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO sales_order VALUES (?, ?, ?, NULL)")) {
            statement.setString(1, so);
            statement.setString(2, status);
            statement.setString(3, procurement);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("INSERT INTO invoice VALUES (?, ?, 'PAID', 20700, 0, ?, ?, ?, '2026-10-09 00:00:00')")) {
            statement.setString(1, "INV-" + so);
            statement.setString(2, so);
            statement.setString(3, amount);
            statement.setString(4, amount);
            statement.setString(5, term);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("INSERT INTO invoice_payment VALUES (?, 'APPROVE', ?)")) {
            statement.setString(1, "INV-" + so);
            statement.setString(2, amount);
            statement.executeUpdate();
        }
    }
}
