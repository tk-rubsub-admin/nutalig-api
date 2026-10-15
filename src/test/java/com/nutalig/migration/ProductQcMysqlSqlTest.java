package com.nutalig.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProductQcMysqlSqlTest {
    @TempDir Path temp;

    @Test
    void generatesMysqlStoredScopeAndMatchingCollations() throws Exception {
        var resources = new ClassLoaderResourceAccessor();
        var connection = new OfflineConnection("offline:mysql?version=8.0.43&changeLogFile="
                + temp.resolve("changelog.csv"), resources);
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
        var writer = new StringWriter();
        try (var liquibase = new Liquibase("db/changelog/20261011-001-add-product-qc-masters.yaml", resources, database)) {
            liquibase.update(new Contexts(), new LabelExpression(), writer);
        }
        String sql = writer.toString();
        assertTrue(sql.contains("GENERATED ALWAYS AS (COALESCE(product_subtype1_code, '')) STORED"));
        assertEquals(5, sql.split("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci", -1).length - 1);
        assertTrue(sql.contains("fk_qc_document_current_revision"));
        assertTrue(sql.contains("uk_product_qc_rule_scope"));
        String output = System.getProperty("productQc.mysqlSqlOutput");
        if (output != null) Files.writeString(Path.of(output), sql);
    }
}
