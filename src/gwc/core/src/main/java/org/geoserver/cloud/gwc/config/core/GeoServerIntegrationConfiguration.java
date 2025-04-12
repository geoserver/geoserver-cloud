/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.gwc.event.ConfigChangeEvent;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.gwc.config.CloudGwcConfigPersister;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 * @see DefaultTileLayerCatalogConfiguration
 */
@Configuration(proxyBeanMethods = true)
@ImportFilteredResource(GeoServerIntegrationConfiguration.GS_INTEGRATION_INCLUDES)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.core")
public class GeoServerIntegrationConfiguration {

    private static final String EXCLUDED_BEANS =
            """
            ^(?!\
            GeoSeverTileLayerCatalog\
            |gwcCatalogConfiguration\
            |wmsCapabilitiesXmlReader\
            |gwcTransactionListener\
            |gwcWMSExtendedCapabilitiesProvider\
            |gwcInitializer\
            |gwcGeoServervConfigPersister\
            ).*$\
            """;

    static final String GS_INTEGRATION_INCLUDES =
            "jar:gs-gwc-[0-9]+.*!/geowebcache-geoserver-context.xml#name=" + EXCLUDED_BEANS;

    @PostConstruct
    public void log() {
        log.info("GeoWebCache core GeoServer integration enabled");
    }

    /**
     * Overrides {@code gwcGeoServervConfigPersister} with a cluster-aware {@link
     * GWCConfigPersister} that sends {@link ConfigChangeEvent}s upon {@link
     * GWCConfigPersister#save(org.geoserver.gwc.config.GWCConfig)}
     *
     * @param xsfp
     * @param resourceLoader
     */
    @Bean
    GWCConfigPersister gwcGeoServervConfigPersister(
            XStreamPersisterFactory xsfp, GeoServerResourceLoader resourceLoader, ApplicationEventPublisher publisher) {
        return new CloudGwcConfigPersister(xsfp, resourceLoader, publisher::publishEvent);
    }
}
