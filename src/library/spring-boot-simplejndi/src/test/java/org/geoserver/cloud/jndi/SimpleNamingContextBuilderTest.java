/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jndi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.NamingManager;

/**
 * Test suite for {@link SimpleNamingContextBuilder}
 *
 * @since 1.0
 */
class SimpleNamingContextBuilderTest {

    @Test
    void testInitialContext() throws NamingException {
        assertFalse(NamingManager.hasInitialContextFactoryBuilder());
        assertThrows(
                NoInitialContextException.class,
                () -> NamingManager.getInitialContext(new Hashtable<>()));

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
        NamingManager.setInitialContextFactoryBuilder(builder);
        assertTrue(NamingManager.hasInitialContextFactoryBuilder());
        Context context = NamingManager.getInitialContext(new Hashtable<>());
        assertThat(context).isInstanceOf(SimpleNamingContext.class);
    }

    @Test
    void testNewInitialContext() throws NamingException {
        System.setProperty(
                Context.INITIAL_CONTEXT_FACTORY, SimpleNamingContextFactory.class.getName());
        InitialContext ctx = new InitialContext();
        Context subcontext = ctx.createSubcontext("java:comp");
        assertThat(subcontext).isInstanceOf(SimpleNamingContext.class);
    }
}
