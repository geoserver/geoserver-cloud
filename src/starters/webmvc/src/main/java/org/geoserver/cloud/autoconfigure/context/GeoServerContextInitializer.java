/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.context;

import org.geoserver.GeoserverInitStartupListener;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * {@link ApplicationContextInitializer} replacing upstream's {@link GeoserverInitStartupListener},
 * which is a servlet context listener instead, and hence runs too late in spring-boot.
 *
 * <p>With an {@code ApplicationContextInitializer} we make sure required initializations run before
 * even loading the spring beans.
 *
 * @since 1.0
 */
public class GeoServerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // run once for the webapp context, ignore the actuator context
        if (!(applicationContext instanceof GenericWebApplicationContext)) {
            return;
        }

        // tell geoserver not to control logging, spring-boot will do
        System.setProperty("RELINQUISH_LOG4J_CONTROL", "true");
        // and tell geotools not to redirect to Log4J nor any other framework, we'll use
        // spring-boot's logging redirection. Use Logback redirection policy, JavaLogging
        // will make GeoserverInitStartupListener's call to GeoTools.init() heuristically set
        // Logging.ALL.setLoggerFactory("org.geotools.util.logging.LogbackLoggerFactory")
        // with the caveat that it wrongly maps logging levels and hence if, for example, a logger
        // with level FINE is called with INFO, it doesn't log at all.
        // Log4J2 redirection policy will fail when calling java.util.logging.Logger.setLevel(..)
        // because our version of Log4j2 is newer than GeoTools' and the API changed
        System.setProperty("GT2_LOGGING_REDIRECTION", "CommonsLogging");
        ServletContext source = mockServletContext();
        ServletContextEvent sce = new ServletContextEvent(source);
        GeoserverInitStartupListener startupInitializer = new GeoserverInitStartupListener();
        startupInitializer.contextInitialized(sce);
    }

    protected ServletContext mockServletContext() {
        InvocationHandler handler =
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {
                        return null;
                    }
                };
        return (ServletContext)
                Proxy.newProxyInstance(
                        getClass().getClassLoader(), new Class[] {ServletContext.class}, handler);
    }
}
