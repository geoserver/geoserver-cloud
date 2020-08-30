/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerContextLoaderListener;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.DataDirectoryResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;

/**
 * Replaces the {@link GeoServerContextLoaderListener} listener in web.xml that otherwise would
 * prevent the spring boot app from loading with a "Cannot initialize context because there is
 * already a root application context present - check whether you have multiple ContextLoader*
 * definitions in your web.xml!" error.
 *
 * <p>Sets the servlet context to {@link GeoServerResourceLoader} and {@link
 * DataDirectoryResourceStore}, for some reason they will not being set automatically, and hence the
 * data directory won't be initialized
 */
public class GeoServerServletInitializer implements ServletContextInitializer {

    private @Autowired ApplicationContext context;

    private @Autowired GeoServerResourceLoader resourceLoader;

    private @Autowired DataDirectoryResourceStore resourceStore;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        resourceStore.setServletContext(servletContext);
        resourceLoader.setServletContext(servletContext);
        context.publishEvent(new ContextLoadedEvent(context));
    }
}
