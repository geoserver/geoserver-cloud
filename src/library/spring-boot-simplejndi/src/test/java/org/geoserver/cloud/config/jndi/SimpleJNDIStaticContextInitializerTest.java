/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jndi;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.jndi.SimpleNamingContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

/**
 * @since 1.0
 */
class SimpleJNDIStaticContextInitializerTest {

    private ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withInitializer(new SimpleJNDIStaticContextInitializer());

    @Test
    void test() throws NamingException {
        runner.run(
                context -> {
                    InitialContext initialContext = new InitialContext();
                    assertThat(initialContext).isNotNull();
                    Context ctx = NamingManager.getInitialContext(new Hashtable<>());
                    assertThat(ctx).isInstanceOf(SimpleNamingContext.class);
                });
    }
}
