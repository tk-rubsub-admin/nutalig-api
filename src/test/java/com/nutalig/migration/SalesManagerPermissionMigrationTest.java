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

class SalesManagerPermissionMigrationTest {
    @Test
    void copiesMissingSalesPermissionsAndPreservesManagerPermissionsWithoutDuplicates() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:sales_manager_permissions;MODE=MySQL")) {
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE user_role (role_code VARCHAR(50) PRIMARY KEY, role_name_th VARCHAR(100), role_name_en VARCHAR(100))");
                sql.execute("INSERT INTO user_role VALUES ('SALES_MANAGER', 'ชื่อเดิม', 'Existing Manager')");
                sql.execute("CREATE TABLE role_permission (role_code VARCHAR(50), permission_code VARCHAR(100), PRIMARY KEY (role_code, permission_code))");
                sql.execute("INSERT INTO role_permission VALUES ('SALES', 'PERM_RFQ_CREATE'), ('SALES', 'PERM_QUOTATION_CREATE'), ('SALES_MANAGER', 'PERM_RFQ_CREATE'), ('SALES_MANAGER', 'PERM_PO_PROOF_OVERRIDE')");
            }
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase("db/changelog/20261008-001-backfill-sales-manager-permissions.yaml", new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
                liquibase.update(new Contexts(), new LabelExpression());
                try (var sql = connection.createStatement()) {
                    try (var rows = sql.executeQuery("SELECT permission_code FROM role_permission WHERE role_code='SALES_MANAGER' ORDER BY permission_code")) {
                        assertTrue(rows.next());
                        assertEquals("PERM_PO_PROOF_OVERRIDE", rows.getString(1));
                        assertTrue(rows.next());
                        assertEquals("PERM_QUOTATION_CREATE", rows.getString(1));
                        assertTrue(rows.next());
                        assertEquals("PERM_RFQ_CREATE", rows.getString(1));
                        assertFalse(rows.next());
                    }
                    try (var rows = sql.executeQuery("SELECT role_name_en FROM user_role WHERE role_code='SALES_MANAGER'")) {
                        assertTrue(rows.next());
                        assertEquals("Existing Manager", rows.getString(1));
                    }
                }
            }
        }
    }
}
