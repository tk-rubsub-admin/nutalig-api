package com.nutalig.service;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class ShippingContainerMigrationTest {
    @Test void migrationEnforcesSourceAndActiveReservationConstraints()throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:container_migration_"+UUID.randomUUID()+";MODE=MySQL")) {
            try(var sql=connection.createStatement()) {
                for(String table:new String[]{"supplier_shipping","supplier_shipping_destination","purchase_order_delivery","purchase_order_delivery_detail","purchase_order_delivery_scrap_box"})sql.execute("CREATE TABLE "+table+" (id BIGINT PRIMARY KEY)");
                sql.execute("ALTER TABLE purchase_order_delivery ADD shipping_method VARCHAR(50)");
                sql.execute("ALTER TABLE purchase_order_delivery_detail ADD purchase_order_delivery_id BIGINT");
                sql.execute("ALTER TABLE purchase_order_delivery_detail ADD scrap_box_id BIGINT");
                sql.execute("ALTER TABLE purchase_order_delivery_detail ADD carton_count DECIMAL(18,6)");
                sql.execute("ALTER TABLE purchase_order_delivery_scrap_box ADD purchase_order_delivery_id BIGINT");
                sql.execute("INSERT INTO purchase_order_delivery(id,shipping_method) VALUES(1,'SEA_FCL_20GP'),(2,'SEA_FCL_40HQ'),(3,'LAND'),(4,'SEA')");sql.execute("INSERT INTO purchase_order_delivery_detail(id,purchase_order_delivery_id,carton_count) VALUES(1,1,9)");sql.execute("INSERT INTO purchase_order_delivery_scrap_box(id,purchase_order_delivery_id) VALUES(2,1)");
            }
            var db=DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/20261024-002-add-shipping-containers.yaml",new ClassLoaderResourceAccessor(),db).update("");
            try(var sql=connection.createStatement()) {
                sql.execute("INSERT INTO shipping_container(id,container_job_no,container_size,shipping_method,status) VALUES(1,'C1','20GP','SEA_FCL_20GP','DRAFT'),(2,'C2','20GP','SEA_FCL_20GP','DRAFT')");
                sql.execute("UPDATE shipping_container SET active_container_no='ABC' WHERE id=1");
                assertThrows(java.sql.SQLException.class,()->sql.execute("UPDATE shipping_container SET active_container_no='ABC' WHERE id=2"));
                sql.execute("UPDATE shipping_container SET active_container_no=NULL WHERE id=1");sql.execute("UPDATE shipping_container SET active_container_no='ABC' WHERE id=2");
                String insert="INSERT INTO shipping_container_load(container_id,delivery_id,delivery_detail_id,scrap_box_id,active_scrap_box_id,carton_count,quantity,contents_json) VALUES ";
                assertThrows(java.sql.SQLException.class,()->sql.execute(insert+"(1,1,NULL,NULL,NULL,1,1,'[]')"));
                assertThrows(java.sql.SQLException.class,()->sql.execute(insert+"(1,1,1,2,2,1,1,'[]')"));
                assertThrows(java.sql.SQLException.class,()->sql.execute(insert+"(1,1,NULL,2,2,2,1,'[]')"));
                sql.execute(insert+"(1,1,NULL,2,2,1,10,'[]')");
                assertThrows(java.sql.SQLException.class,()->sql.execute(insert+"(2,1,NULL,2,2,1,10,'[]')"));
                sql.execute("UPDATE shipping_container_load SET active=FALSE,active_scrap_box_id=NULL WHERE container_id=1");
                sql.execute(insert+"(2,1,NULL,2,2,1,10,'[]')");
                new Liquibase("db/changelog/20261024-004-add-delivery-container-assignment.yaml",new ClassLoaderResourceAccessor(),db).update("");
                try(var result=sql.executeQuery("SELECT container_status FROM purchase_order_delivery ORDER BY id")) {
                    assertTrue(result.next());assertEquals("PARTIALLY_ASSIGNED",result.getString(1));
                    assertTrue(result.next());assertEquals("NOT_ASSIGNED",result.getString(1));
                    assertTrue(result.next());assertNull(result.getString(1));assertTrue(result.next());assertNull(result.getString(1));
                }
                try(var result=sql.executeQuery("SELECT container_id FROM purchase_order_delivery_container WHERE delivery_id=1")) {
                    assertTrue(result.next());assertEquals(2,result.getLong(1));assertFalse(result.next());
                }
                assertThrows(java.sql.SQLException.class,()->sql.execute("INSERT INTO purchase_order_delivery_container VALUES(99,1)"));
                assertThrows(java.sql.SQLException.class,()->sql.execute("INSERT INTO purchase_order_delivery_container VALUES(1,99)"));
            }
        }
    }
}
