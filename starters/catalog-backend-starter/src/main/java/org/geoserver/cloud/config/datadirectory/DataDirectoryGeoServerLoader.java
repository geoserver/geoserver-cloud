/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import java.util.List;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource.Lock;

/** */
public class DataDirectoryGeoServerLoader extends DefaultGeoServerLoader {

    public DataDirectoryGeoServerLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    @Override
    protected void loadGeoServer(final GeoServer geoServer, XStreamPersister xp) throws Exception {
        super.loadGeoServer(geoServer, xp);
        initializeEmptyConfig(geoServer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void initializeEmptyConfig(final GeoServer geoServer) {
        if (geoServer.getGlobal() != null && geoServer.getLogging() != null) {
            return;
        }
        // TODO: this needs to be pushed upstream
        final Lock lock = resourceLoader.getLockProvider().acquire("GLOBAL");
        try {
            if (geoServer.getGlobal() == null) {
                geoServer.setGlobal(geoServer.getFactory().createGlobal());
            }
            if (geoServer.getLogging() == null) {
                geoServer.setLogging(geoServer.getFactory().createLogging());
            }
            // ensure we have a service configuration for every service we know about
            final List<XStreamServiceLoader> loaders =
                    GeoServerExtensions.extensions(XStreamServiceLoader.class);
            for (XStreamServiceLoader l : loaders) {
                ServiceInfo s = geoServer.getService(l.getServiceClass());
                if (s == null) {
                    geoServer.add(l.create(geoServer));
                }
            }
        } finally {
            lock.release();
        }
    }
}
