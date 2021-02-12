/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.servlet;

import static org.junit.Assert.assertNotNull;

import org.geoserver.GeoserverInitStartupListener;
import org.geoserver.cloud.config.servlet.GeoServerServletInitializer;
import org.geoserver.cloud.test.TestConfiguration;
import org.geoserver.filters.FlushSafeFilter;
import org.geoserver.filters.SessionDebugFilter;
import org.geoserver.filters.SpringDelegatingFilter;
import org.geoserver.filters.ThreadLocalsCleanupFilter;
import org.geoserver.platform.AdvancedDispatchFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextListener;

/** Smoke test to load the servlet context beans with auto-configuration enabled */
@SpringBootTest(
    classes = TestConfiguration.class,
    properties = "reactive.feign.loadbalancer.enabled=false"
)
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class ServletContextEnabledSmokeTest {

    private @Autowired ApplicationContext context;

    public @Test void initStartupListener() {
        assertNotNull(context.getBean(GeoserverInitStartupListener.class));
    }

    public @Test void contextLoaderListener() {
        assertNotNull(context.getBean(GeoServerServletInitializer.class));
    }

    public @Test void requestContextListener() {
        assertNotNull(context.getBean(RequestContextListener.class));
    }

    public @Test void flushSafeFilter() {
        assertNotNull(context.getBean(FlushSafeFilter.class));
    }

    public @Test void sessionDebugFilter() {
        assertNotNull(context.getBean(SessionDebugFilter.class));
    }

    public @Test void advancedDispatchFilter() {
        assertNotNull(context.getBean(AdvancedDispatchFilter.class));
    }

    public @Test void springDelegatingFilter() {
        assertNotNull(context.getBean(SpringDelegatingFilter.class));
    }

    public @Test void threadLocalsCleanupFilter() {
        assertNotNull(context.getBean(ThreadLocalsCleanupFilter.class));
    }
}
