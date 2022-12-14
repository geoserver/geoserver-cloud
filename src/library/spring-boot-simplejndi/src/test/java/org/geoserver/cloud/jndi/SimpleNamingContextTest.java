/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jndi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Collections;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Test suite for {@link SimpleNamingContext}
 *
 * @since 1.0
 */
class SimpleNamingContextTest {

    private SimpleNamingContext root;

    @BeforeEach
    void setup() throws NamingException {
        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
        root =
                (SimpleNamingContext)
                        builder.createInitialContextFactory(null).getInitialContext(null);
        assertNotNull(root);
    }

    @Test
    void testBindAndLookup() throws NamingException {
        assertThrows(NameNotFoundException.class, () -> root.lookup("java:comp/env/jdbc"));

        String dsname = "java:comp/env/jdbc/testds";
        DataSource ds = new DriverManagerDataSource("jdbc:h2:mem:mydb");
        root.bind(dsname, ds);

        assertSame(ds, root.lookup(dsname));
    }

    @Test
    void testBindAndLookupSubContext() throws NamingException {
        assertThrows(NameNotFoundException.class, () -> root.lookup("java:comp/env/jdbc"));

        Context jdbcSubcontext = root.createSubcontext("java:comp/env/jdbc");

        String dsname = "java:comp/env/jdbc/testds";
        DataSource ds = new DriverManagerDataSource("jdbc:h2:mem:mydb");
        jdbcSubcontext.bind(dsname, ds);

        assertSame(ds, jdbcSubcontext.lookup(dsname));
        assertThrows(NameNotFoundException.class, () -> root.lookup(dsname));
    }

    @Test
    void testList() throws NamingException {
        String dsname = "java:comp/env/jdbc/testds";
        DataSource ds = new DriverManagerDataSource("jdbc:h2:mem:mydb");
        root.bind(dsname, ds);
        root.bind("java:comp/test/obj1", new Object());
        root.bind("java:comp/test/obj2", new Object());

        NamingEnumeration<NameClassPair> enumeration = root.list("java:comp/env");
        assertNotNull(enumeration);
        Set<NameClassPair> list = Set.copyOf(Collections.list(enumeration));
        Set<NameClassPair> expected = Set.of(ncp("jdbc", SimpleNamingContext.class));
        assertEquals(expected, list);

        list = Set.copyOf(Collections.list(root.list("java:comp")));
        expected =
                Set.of(
                        ncp("test", SimpleNamingContext.class),
                        ncp("env", SimpleNamingContext.class));
        assertEquals(expected, list);

        list = Set.copyOf(Collections.list(root.list("java:comp/test")));
        expected = Set.of(ncp("obj1", Object.class), ncp("obj2", Object.class));
        assertEquals(expected, list);

        list = Set.copyOf(Collections.list(root.list("java:comp/env/jdbc")));
        expected = Set.of(ncp("testds", DriverManagerDataSource.class));
        assertEquals(expected, list);
    }

    private NameClassPair ncp(String name, Class<?> type) {
        return new org.geoserver.cloud.jndi.NameClassPair(name, type.getName());
    }
}
