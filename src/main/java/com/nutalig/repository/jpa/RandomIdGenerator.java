package com.nutalig.repository.jpa;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.time.Clock;
import java.util.Properties;

public class RandomIdGenerator implements IdentifierGenerator, Configurable {

    private final transient IdentificationGenerator idGen;

    public static final String PREFIX = "prefix";
    public static final String LENGTH = "length";

    private String idPrefix;
    private int idLength;

    public RandomIdGenerator(final Clock clock) {
        this.idGen = new IdentificationGenerator(clock);
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o) {
        return idGen.generate(idPrefix, idLength);
    }

    @Override
    public void configure(
            Type type,
            Properties params,
            ServiceRegistry serviceRegistry)
            throws MappingException {

        idPrefix = ConfigurationHelper
                .getString(
                        PREFIX,
                        params,
                        PREFIX
                );

        idLength = ConfigurationHelper
                .getInt(
                        LENGTH,
                        params,
                        6
                );
    }

}