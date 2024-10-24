/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.jndi;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.jndi.SimpleNamingContextBuilder;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

/**
 * {@link ApplicationContextInitializer} setting {@link SimpleNamingContextBuilder} as the JNDI
 * implementation.
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.config.jndi")
public class SimpleJNDIStaticContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static boolean initialized;

    private static synchronized boolean initialize() {
        if (initialized) {
            return false;
        }
        if (NamingManager.hasInitialContextFactoryBuilder()) {
            log.info("JNDI InitialContextFactoryBuilder already set");
            return false;
        }
        initialized = true;
        return true;
    }

    /**
     * Register the context builder by registering it with the JNDI NamingManager. Note that once
     * this has been done, {@code new InitialContext()} will always return a context from this
     * factory.
     *
     * @throws ApplicationContextException builder cannot be installed for a non-security-related
     *     reason.
     * @throws IllegalStateException If a builder was previous installed (as of {@link
     *     NamingManager#setInitialContextFactoryBuilder}.
     */
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (initialize()) {
            log.info("Initializing JNDI InitialContextFactoryBuilder");
            final String builderClassName = SimpleNamingContextBuilder.class.getName();
            try {
                NamingManager.setInitialContextFactoryBuilder(new SimpleNamingContextBuilder());
                log.info("Registered JNDI implementation using " + builderClassName);
            } catch (NamingException e) {
                throw new ApplicationContextException(
                        "Unexpected error installing JNDI " + builderClassName, e);
            }
        }
    }
}
