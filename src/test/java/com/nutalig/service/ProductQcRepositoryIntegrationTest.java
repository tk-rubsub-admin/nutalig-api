package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.constant.QcTypes.MatchLevel;
import com.nutalig.entity.*;
import com.nutalig.repository.*;
import com.nutalig.support.ProductQcTestDatabase;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductQcRepositoryIntegrationTest {
    @Test
    void resolvesLegacyMastersUsingRealJpaRepositoriesAndReleasedRevision() throws Exception {
        String url = "jdbc:h2:mem:qc_jpa_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url)) {
            ProductQcTestDatabase.loadLegacyMasters(connection);
            ProductQcTestDatabase.migrate(connection);
            try (var sql = connection.createStatement()) {
                sql.execute("INSERT INTO qc_document (id,document_no,qc_route_code,name_th,name_en,document_type) VALUES (1,'QC-PLASTIC','MAT-QC-02','พลาสติก','Plastic','MATERIAL_QC')");
                sql.execute("INSERT INTO qc_document_revision (id,qc_document_id,revision_no,file_name,original_file_name,content_type,file_size,sha256,status) VALUES (1,1,'Rev.00','qc/old.pdf','old.pdf','application/pdf',100,REPEAT('a',64),'RETIRED'),(2,1,'Rev.01','qc/current.pdf','current.pdf','application/pdf',200,REPEAT('b',64),'RELEASED')");
                sql.execute("UPDATE qc_document SET current_revision_id=2 WHERE id=1");
            }
            connection.commit();
            Configuration config = new Configuration();
            for (Class<?> type : new Class<?>[]{ProductFamilyEntity.class, ProductMaterialEntity.class,
                    ProductSubtype1Entity.class, QcRouteEntity.class, QcDocumentEntity.class,
                    QcDocumentRevisionEntity.class, ProductMaterialQcRuleEntity.class, ProductFamilyQcPolicyEntity.class}) {
                config.addAnnotatedClass(type);
            }
            config.setProperty("hibernate.connection.url", url);
            config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            config.setProperty("hibernate.hbm2ddl.auto", "none");
            try (var sessionFactory = config.buildSessionFactory(); var session = sessionFactory.openSession()) {
                var repositories = new JpaRepositoryFactory(session);
                var storedDocuments = repositories.getRepository(QcDocumentRepository.class)
                        .findAllByQcRouteCodeInAndIsActiveTrueOrderByIdAsc(java.util.List.of("MAT-QC-02"));
                assertEquals(1, storedDocuments.size());
                assertNotNull(storedDocuments.getFirst().getCurrentRevision());
                assertEquals(1L, storedDocuments.getFirst().getCurrentRevision().getQcDocumentId());
                assertEquals(com.nutalig.constant.QcTypes.RevisionStatus.RELEASED,
                        storedDocuments.getFirst().getCurrentRevision().getStatus());
                var service = new ProductQcService(new AppProperties(),
                        repositories.getRepository(ProductFamilyRepository.class),
                        repositories.getRepository(ProductMaterialRepository.class),
                        repositories.getRepository(ProductSubtype1Repository.class),
                        repositories.getRepository(ProductMaterialQcRuleRepository.class),
                        repositories.getRepository(ProductFamilyQcPolicyRepository.class),
                        repositories.getRepository(QcRouteRepository.class),
                        repositories.getRepository(QcDocumentRepository.class));
                var bottle = service.resolve("PF001", "PMAT0004", "PST0001");
                assertEquals("MAT-QC-02", bottle.route().code());
                assertEquals(MatchLevel.SPECIFIC, bottle.matchLevel());
                assertEquals("qc/current.pdf", bottle.documents().getFirst().fileName());
                assertEquals(2L, bottle.documents().getFirst().revisionId());
                assertTrue(bottle.missingDocumentRouteCodes().isEmpty());
                assertEquals(MatchLevel.DEFAULT, service.resolve("PF001", "PMAT0004", null).matchLevel());
                var bag = service.resolve("PF013", "PMAT0069", "PST0092");
                assertEquals("MAT-QC-05", bag.route().code());
                var hydrogel = service.resolve("PF012", "PMAT0031", null);
                assertEquals("SPECIAL-F13", hydrogel.route().code());
                assertTrue(hydrogel.specialInspectionPlanRequired());
                assertEquals(1, hydrogel.missingDocumentRouteCodes().size());
                var pending = service.resolve("PF012", "PMAT0030", null);
                assertEquals("QC-HOLD", pending.route().code());
                assertTrue(pending.specialInspectionPlanRequired());
            }
        }
    }
}
