package com.nutalig.migration;

import com.nutalig.support.ProductQcTestDatabase;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductQcMigrationTest {
    @Test
    void seedsGuardedRulesAndEnforcesFamilyScopeAndDocumentOwnership() throws Exception {
        try (var connection = database()) {
            ProductQcTestDatabase.loadLegacyMasters(connection);
            ProductQcTestDatabase.migrate(connection);
            ProductQcTestDatabase.migrate(connection);
            assertEquals(23, count(connection, "SELECT COUNT(*) FROM product_family"));
            assertEquals(112, count(connection, "SELECT COUNT(*) FROM product_subtype1"));
            assertEquals(168, count(connection, "SELECT COUNT(*) FROM product_material"));
            assertEquals(11, count(connection, "SELECT COUNT(*) FROM qc_route"));
            assertEquals(598, count(connection, "SELECT COUNT(*) FROM product_material_qc_rule WHERE status='ACTIVE'"));
            assertEquals(165, count(connection, "SELECT COUNT(*) FROM product_material_qc_rule WHERE status='DRAFT'"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM qc_document"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM product_family_qc_policy WHERE product_family_code='PF012'"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM product_family_qc_policy WHERE product_family_code='PF013'"));
            assertEquals(598, count(connection, "SELECT COUNT(*) FROM v_product_material_qc_rule"));

            String insertRule = "INSERT INTO product_material_qc_rule (taxonomy_revision,product_family_code,product_material_code,product_subtype1_code,qc_route_code,purchasing_confirmation_policy,status) VALUES ";
            assertThrows(SQLException.class, () -> execute(connection, insertRule + "('REV10','PF001','PMAT0004',NULL,'MAT-QC-02','ALWAYS','ACTIVE')"));
            assertThrows(SQLException.class, () -> execute(connection, insertRule + "('REV10','PF001','PMAT0004','PST0001','MAT-QC-02','ALWAYS','ACTIVE')"));
            assertThrows(SQLException.class, () -> execute(connection, insertRule + "('REV11','PF013','PMAT0004',NULL,'MAT-QC-02','ALWAYS','ACTIVE')"));
            assertThrows(SQLException.class, () -> execute(connection, insertRule + "('REV11','PF001','PMAT0004','PST0092','MAT-QC-02','ALWAYS','ACTIVE')"));
            assertThrows(SQLException.class, () -> execute(connection, insertRule + "('REV11','PF001','PMAT0004','','MAT-QC-02','ALWAYS','ACTIVE')"));

            String insertDoc = "INSERT INTO qc_document (id,document_no,qc_route_code,name_th,name_en,document_type) VALUES ";
            assertThrows(SQLException.class, () -> execute(connection, insertDoc + "(99,'HOLD-DOC','QC-HOLD','รอ','Hold','MATERIAL_QC')"));
            execute(connection, insertDoc + "(1,'QC-PLASTIC','MAT-QC-02','พลาสติก','Plastic','MATERIAL_QC'),(2,'QC-GLASS','MAT-QC-01','แก้ว','Glass','MATERIAL_QC')");
            String insertRevision = "INSERT INTO qc_document_revision (id,qc_document_id,revision_no,file_name,original_file_name,content_type,file_size,sha256,status) VALUES ";
            execute(connection, insertRevision + "(1,1,'Rev.00','old.pdf','old.pdf','application/pdf',100,REPEAT('a',64),'RELEASED'),(2,1,'Rev.01','new.pdf','new.pdf','application/pdf',200,REPEAT('b',64),'RELEASED'),(3,2,'Rev.00','glass.pdf','glass.pdf','application/pdf',100,REPEAT('c',64),'RELEASED')");
            execute(connection, "UPDATE qc_document SET current_revision_id=1 WHERE id=1");
            execute(connection, "UPDATE qc_document SET current_revision_id=2 WHERE id=1");
            assertEquals("old.pdf", scalar(connection, "SELECT file_name FROM qc_document_revision WHERE id=1"));
            assertThrows(SQLException.class, () -> execute(connection, "UPDATE qc_document SET current_revision_id=3 WHERE id=1"));
            assertThrows(SQLException.class, () -> execute(connection, insertRevision + "(4,1,'Rev.01','duplicate.pdf','duplicate.pdf','application/pdf',100,REPEAT('d',64),'DRAFT')"));
            assertThrows(SQLException.class, () -> execute(connection, "DELETE FROM qc_document_revision WHERE id=2"));
            execute(connection, insertRevision + "(5,1,'Rev.02','draft.pdf','draft.pdf','application/pdf',100,REPEAT('e',64),'DRAFT')");
            assertThrows(SQLException.class, () -> execute(connection, "UPDATE qc_document SET current_revision_id=5 WHERE id=1"));
            assertThrows(SQLException.class, () -> execute(connection, "UPDATE qc_document SET current_revision_status='DRAFT' WHERE id=1"));
            assertThrows(SQLException.class, () -> execute(connection, "UPDATE qc_document_revision SET status='RETIRED' WHERE id=2"));
            execute(connection, "UPDATE qc_route SET is_active=FALSE WHERE code='MAT-QC-02'");
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM v_product_material_qc_rule WHERE qc_attachment_code='MAT-QC-02'"));
        }
    }

    @Test
    void doesNotActivateMappingsWhenExistingMaterialIdentityHasChanged() throws Exception {
        try (var connection = database()) {
            ProductQcTestDatabase.loadLegacyMasters(connection);
            execute(connection, "UPDATE product_material SET name_en='Unknown polymer' WHERE code='PMAT0002' AND product_family_code='PF001'");
            ProductQcTestDatabase.migrate(connection);
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM product_material_qc_rule WHERE product_family_code='PF001' AND product_material_code='PMAT0002' AND status='ACTIVE'"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM product_material_qc_rule WHERE product_family_code='PF001' AND product_material_code='PMAT0002' AND status='DRAFT' AND qc_route_code='QC-HOLD'"));
        }
    }

    private Connection database() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:qc_" + UUID.randomUUID() + ";MODE=MySQL");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
    }

    private int count(Connection connection, String sql) throws SQLException {
        return Integer.parseInt(scalar(connection, sql));
    }

    private String scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
