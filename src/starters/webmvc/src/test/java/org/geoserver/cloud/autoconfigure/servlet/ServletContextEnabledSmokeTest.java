/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.servlet;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.geoserver.cloud.config.servlet.GeoServerServletInitializer;
import org.geoserver.cloud.test.TestConfiguration;
import org.geoserver.filters.FlushSafeFilter;
import org.geoserver.filters.SessionDebugFilter;
import org.geoserver.filters.SpringDelegatingFilter;
import org.geoserver.filters.ThreadLocalsCleanupFilter;
import org.geoserver.platform.AdvancedDispatchFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextListener;

/** Smoke test to load the servlet context beans with auto-configuration enabled */
@SpringBootTest(classes = TestConfiguration.class)
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@ActiveProfiles("test")
class ServletContextEnabledSmokeTest extends DataDirectoryTempSupport {

    private @Autowired ApplicationContext context;

    @Test
    void contextLoaderListener() {
        assertNotNull(context.getBean(GeoServerServletInitializer.class));
    }

    @Test
    void requestContextListener() {
        assertNotNull(context.getBean(RequestContextListener.class));
    }

    @Test
    void flushSafeFilter() {
        assertNotNull(context.getBean(FlushSafeFilter.class));
    }

    @Test
    void sessionDebugFilter() {
        assertNotNull(context.getBean(SessionDebugFilter.class));
    }

    @Test
    void advancedDispatchFilter() {
        assertNotNull(context.getBean(AdvancedDispatchFilter.class));
    }

    @Test
    void springDelegatingFilter() {
        assertNotNull(context.getBean(SpringDelegatingFilter.class));
    }

    @Test
    void threadLocalsCleanupFilter() {
        assertNotNull(context.getBean(ThreadLocalsCleanupFilter.class));
    }
}
