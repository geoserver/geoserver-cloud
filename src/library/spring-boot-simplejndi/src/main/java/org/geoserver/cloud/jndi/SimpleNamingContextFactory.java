/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jndi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * Simple implementation of a JNDI naming context factory.
 *
 * @see SimpleNamingContext
 */
public class SimpleNamingContextFactory implements InitialContextFactory {

    private SimpleNamingContext initialContext = new SimpleNamingContext();

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return initialContext;
    }
}
