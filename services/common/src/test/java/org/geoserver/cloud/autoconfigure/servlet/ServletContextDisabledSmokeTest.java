package org.geoserver.cloud.autoconfigure.servlet;

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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextListener;

/**
 * Smoke test to check geoserver servlet context related spring beans are not loaded if the
 * auto-configuration is disabled throgh {@code geoserver.servlet.enabled=false}
 */
@SpringBootTest(
    classes = TestConfiguration.class,
    properties = {"geoserver.servlet.enabled=false"}
)
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class ServletContextDisabledSmokeTest {

    private @Autowired ApplicationContext context;

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void initStartupListener() {
        context.getBean(GeoserverInitStartupListener.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void contextLoaderListener() {
        context.getBean(GeoServerServletInitializer.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void requestContextListener() {
        context.getBean(RequestContextListener.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void flushSafeFilter() {
        context.getBean(FlushSafeFilter.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void sessionDebugFilter() {
        context.getBean(SessionDebugFilter.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void advancedDispatchFilter() {
        context.getBean(AdvancedDispatchFilter.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void springDelegatingFilter() {
        context.getBean(SpringDelegatingFilter.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void threadLocalsCleanupFilter() {
        context.getBean(ThreadLocalsCleanupFilter.class);
    }
}
