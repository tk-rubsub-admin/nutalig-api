package com.nutalig.support;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;

public final class ProductQcTestDatabase {
    private ProductQcTestDatabase() {}

    public static void loadLegacyMasters(Connection connection) {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/product-qc-legacy-fixture.sql"));
    }

    public static void migrate(Connection connection) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        var liquibase = new Liquibase("db/changelog/20261011-001-add-product-qc-masters.yaml",
                new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }
}
