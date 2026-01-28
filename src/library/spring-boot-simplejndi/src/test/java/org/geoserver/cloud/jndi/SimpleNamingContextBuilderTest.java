/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.jndi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.directory.DirContext;
import javax.naming.spi.NamingManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test suite for {@link SimpleNamingContextBuilder}
 *
 * @since 1.0
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Use per-class lifecycle to initialize container once
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleNamingContextBuilderTest {

    @Container
    private static final GeorchestraLdapContainer LDAP_CONTAINER = new GeorchestraLdapContainer();

    @BeforeAll
    static void beforeAll() {
        LDAP_CONTAINER.start();
        System.setProperty("ldapPort", String.valueOf(LDAP_CONTAINER.getMappedLdapPort()));
        System.setProperty("ldapHost", LDAP_CONTAINER.getHost());
    }

    @Order(1)
    @Test
    void testInitialContext() throws NamingException {
        assertFalse(NamingManager.hasInitialContextFactoryBuilder());
        assertThrows(NoInitialContextException.class, () -> NamingManager.getInitialContext(new Hashtable<>()));

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
        NamingManager.setInitialContextFactoryBuilder(builder);
        assertTrue(NamingManager.hasInitialContextFactoryBuilder());
        Context context = NamingManager.getInitialContext(new Hashtable<>());
        assertThat(context).isInstanceOf(SimpleNamingContext.class);
    }

    @Order(2)
    @Test
    void testNewInitialContext() throws NamingException {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, SimpleNamingContextFactory.class.getName());
        InitialContext ctx = new InitialContext();
        Context subcontext = ctx.createSubcontext("java:comp");
        assertThat(subcontext).isInstanceOf(SimpleNamingContext.class);
    }

    /**
     * Simulates an LDAP authentication request as performed by GeoServer
     */
    @Order(3)
    @Test
    void testParameterizedLDAPInitialContextFactory() throws NamingException, ClassNotFoundException {
        Hashtable<Object, Object> env = new Hashtable<>();
        // should work if java.naming.factory.initial is a String
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.factory.object", "org.springframework.ldap.core.support.DefaultDirObjectFactory");
        env.put("java.naming.ldap.version", "3");

        final int ldapPort = LDAP_CONTAINER.getMappedLdapPort();
        env.put("org.springframework.ldap.base.path", "dc=georchestra,dc=org");
        env.put("java.naming.provider.url", "ldap://localhost:%d/dc=georchestra,dc=org".formatted(ldapPort));
        env.put("java.naming.security.principal", "uid=testadmin,ou=users,dc=georchestra,dc=org");
        env.put("java.naming.security.authentication", "simple");
        env.put("java.naming.security.credentials", "testadmin");

        Context context = NamingManager.getInitialContext(env);
        assertThat(context).isInstanceOf(DirContext.class);

        // and should work if java.naming.factory.initial is a Class
        env.put(Context.INITIAL_CONTEXT_FACTORY, Class.forName("com.sun.jndi.ldap.LdapCtxFactory"));
        context = NamingManager.getInitialContext(env);
        assertThat(context).isInstanceOf(DirContext.class);
    }

    @Order(4)
    @Test
    void testInvalidInitialContextFactoryParams() {

        Hashtable<Object, Object> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, java.lang.Integer.class);
        assertThrows(IllegalArgumentException.class, () -> NamingManager.getInitialContext(env));

        env.put(Context.INITIAL_CONTEXT_FACTORY, "java.lang.Integer");
        assertThrows(IllegalArgumentException.class, () -> NamingManager.getInitialContext(env));

        env.put(Context.INITIAL_CONTEXT_FACTORY, Integer.valueOf(1));
        assertThrows(IllegalArgumentException.class, () -> NamingManager.getInitialContext(env));

        env.put(Context.INITIAL_CONTEXT_FACTORY, "no.such.Class");
        assertThrows(IllegalArgumentException.class, () -> NamingManager.getInitialContext(env));
    }
}
