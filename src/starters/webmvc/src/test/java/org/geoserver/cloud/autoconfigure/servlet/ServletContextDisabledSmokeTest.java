/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.servlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.geoserver.cloud.config.servlet.GeoServerServletInitializer;
import org.geoserver.cloud.test.TestConfiguration;
import org.geoserver.filters.FlushSafeFilter;
import org.geoserver.filters.SessionDebugFilter;
import org.geoserver.filters.SpringDelegatingFilter;
import org.geoserver.filters.ThreadLocalsCleanupFilter;
import org.geoserver.platform.AdvancedDispatchFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextListener;

/**
 * Smoke test to check geoserver servlet context related spring beans are not loaded if the
 * auto-configuration is disabled through {@code geoserver.servlet.enabled=false}
 */
@SpringBootTest(
        classes = TestConfiguration.class,
        properties = {"geoserver.servlet.enabled=false"})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@ActiveProfiles("test")
class ServletContextDisabledSmokeTest extends DataDirectoryTempSupport {

    private @Autowired ApplicationContext context;

    @Test
    void contextLoaderListener() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(GeoServerServletInitializer.class));
    }

    public void requestContextListener() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(RequestContextListener.class));
    }

    public void flushSafeFilter() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(FlushSafeFilter.class));
    }

    public void sessionDebugFilter() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(SessionDebugFilter.class));
    }

    public void advancedDispatchFilter() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(AdvancedDispatchFilter.class));
    }

    public void springDelegatingFilter() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(SpringDelegatingFilter.class));
    }

    public void threadLocalsCleanupFilter() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(ThreadLocalsCleanupFilter.class));
    }
}
