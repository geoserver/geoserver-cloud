/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.pgconfig;

import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServiceLoader;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.core.Ordered;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
@Slf4j
public class GeoServerConfigInitializer implements GeoServerInitializer, Ordered, ExtensionPriority {

    private final @NonNull GeoServerConfigurationLock configLock;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        configLock.lock(LockType.READ);
        try {
            if (geoServer.getGlobal() != null) {
                return;
            }
            log.info("initializing geoserver global config");
            geoServer.setGlobal(geoServer.getFactory().createGlobal());

            if (geoServer.getLogging() == null) {
                log.info("initializing geoserver logging config");
                geoServer.setLogging(geoServer.getFactory().createLogging());
            }
            // also ensure we have a service configuration for every service we know about
            @SuppressWarnings("rawtypes")
            final List<XStreamServiceLoader> loaders = GeoServerExtensions.extensions(XStreamServiceLoader.class);
            for (ServiceLoader<?> l : loaders) {
                ServiceInfo s = geoServer.getService(l.getServiceClass());
                if (s == null) {
                    log.info(
                            "creating default service config for {}",
                            l.getServiceClass().getSimpleName());
                    geoServer.add(l.create(geoServer));
                }
            }
        } finally {
            configLock.unlock();
        }
    }
}
