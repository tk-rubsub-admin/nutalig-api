package com.nutalig.repository.jpa;

import jakarta.persistence.Entity;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;
import java.util.stream.Stream;

public class IdGenerator implements IdentifierGenerator,Configurable {
    private String idPrefix;
    private String length;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj)
            throws HibernateException {

        String query = String.format("select %s from %s",
                session.getEntityPersister(obj.getClass().getName(), obj)
                        .getIdentifierPropertyName(),
                obj.getClass().getAnnotation(Entity.class).name());

        Stream<String> ids = session.createQuery(query).stream();

        Long max = ids.map(o -> o.replace(idPrefix + "-", ""))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);

        return idPrefix + "-" +  String.format(length,(max + 1));
    }

    @Override
    public void configure(Type type, Properties properties, ServiceRegistry serviceRegistry) throws MappingException {
        this.idPrefix = ConfigurationHelper.getString("prefix", properties, "prefix");
        this.length = ConfigurationHelper.getString("length", properties, "length");
    }
}
