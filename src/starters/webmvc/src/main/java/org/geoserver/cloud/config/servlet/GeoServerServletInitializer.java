/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.servlet;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerContextLoaderListener;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Replaces the {@link GeoServerContextLoaderListener} listener in web.xml that otherwise would
 * prevent the spring boot app from loading with a "Cannot initialize context because there is
 * already a root application context present - check whether you have multiple ContextLoader*
 * definitions in your web.xml!" error.
 *
 * <p>Instead of implementing {@link ServletContextInitializer}, listens to {@link
 * ContextRefreshedEvent}, since servlet context initialization happens too early during application
 * context initialization and some things like the event bus may not be ready.
 */
@RequiredArgsConstructor
public class GeoServerServletInitializer implements ApplicationListener<ContextRefreshedEvent> {

    /**
     * Actual application context, held to check whether the context being refreshed is this one and
     * avoid sending multiple geoserver-specific {@link ContextLoadedEvent}s
     */
    private final @NonNull ApplicationContext appContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext eventContext = event.getApplicationContext();
        if (appContext == eventContext) {
            eventContext.publishEvent(new ContextLoadedEvent(eventContext));
        }
    }
}
