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
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class IdGenerator implements IdentifierGenerator,Configurable {
    private String idPrefix;
    private String length;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj)
            throws HibernateException {

        var entityPersister = session.getEntityPersister(obj.getClass().getName(), obj);
        Object assignedId = entityPersister.getIdentifier(obj, session);
        if (assignedId != null) {
            return (Serializable) assignedId;
        }

        String query = String.format("select %s from %s",
                entityPersister.getIdentifierPropertyName(),
                obj.getClass().getAnnotation(Entity.class).name());

        Pattern generatedIdPattern = Pattern.compile("^" + Pattern.quote(idPrefix) + "-(\\d+)$");
        Stream<String> ids = session.createQuery(query).stream();

        Long max = ids
                .map(generatedIdPattern::matcher)
                .filter(java.util.regex.Matcher::matches)
                .map(matcher -> matcher.group(1))
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
