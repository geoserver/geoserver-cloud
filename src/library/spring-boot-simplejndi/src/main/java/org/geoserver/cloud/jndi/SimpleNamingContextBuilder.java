/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jndi;

import org.springframework.lang.Nullable;

import java.util.Hashtable;

import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

/**
 * Simple implementation of a JNDI naming context builder.
 *
 * <p>Sample usage:
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
 * NamingManager.setInitialContextFactoryBuilder(builder);
 * ...
 * Context initialContext = NamingManager.getInitialContext(new HashTable<>());
 * DataSource ds = new DriverManagerDataSource(...);
 * initialContext.bind("java:comp/env/jdbc/myds", ds);
 * </pre>
 *
 * @see SimpleNamingContext
 */
public class SimpleNamingContextBuilder implements InitialContextFactoryBuilder {

    private final SimpleNamingContextFactory FACTORY = new SimpleNamingContextFactory();

    @Override
    public InitialContextFactory createInitialContextFactory(
            @Nullable Hashtable<?, ?> environment) {
        return FACTORY;
    }
}
