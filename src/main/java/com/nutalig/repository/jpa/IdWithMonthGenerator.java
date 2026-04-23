package com.nutalig.repository.jpa;

import com.nutalig.utils.DateUtil;
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
import java.time.LocalDate;
import java.util.Date;
import java.util.Properties;
import java.util.stream.Stream;

public class IdWithMonthGenerator implements IdentifierGenerator,Configurable {
    private String idPrefix;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj)
            throws HibernateException {

        String query = String.format("select %s from %s",
                session.getEntityPersister(obj.getClass().getName(), obj)
                        .getIdentifierPropertyName(),
                obj.getClass().getAnnotation(Entity.class).name());

        Stream<String> ids = session.createQuery(query).stream();

        LocalDate localDate = LocalDate.now();
        String monthStr = DateUtil.YYYY_MM.format(new Date());
        Long max = ids.map(o -> o.replace(idPrefix + monthStr , ""))
                .map(o -> {
                    try {
                        return Long.parseLong(o); // Parse the remaining numeric part
                    } catch (NumberFormatException e) {
                        return 0L; // In case of invalid format, return 0
                    }
                })
                .max(Long::compareTo) // Get the max number
                .orElse(0L);
        if (max == 0L && localDate.isEqual(localDate.withDayOfMonth(1))) {
            return idPrefix + monthStr +  String.format("%06d",(0L + 1));
        }
        return idPrefix + monthStr +  String.format("%06d",(max + 1));
    }

    @Override
    public void configure(Type type, Properties properties, ServiceRegistry serviceRegistry) throws MappingException {
        this.idPrefix = ConfigurationHelper.getString("prefix", properties, "prefix");
    }
}
