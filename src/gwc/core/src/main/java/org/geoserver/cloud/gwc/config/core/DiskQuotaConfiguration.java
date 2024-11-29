/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.DefaultStorageFinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {"jar:gs-gwc-[0-9]+.*!/geowebcache-diskquota-context.xml#name=^(?!DiskQuotaConfigLoader).*$"})
public class DiskQuotaConfiguration {

    static {
        /*
         * Disable disk-quota by brute force for now. We need to resolve how and where to store the
         * configuration and database.
         */
        System.setProperty(DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED, "true");
    }

    /**
     * Override {@literal DiskQuotaConfigLoader} not to depend on the excluded {@literal
     * metaStoreRemover}
     */
    @Bean(name = "DiskQuotaConfigLoader")
    org.geowebcache.diskquota.ConfigLoader diskQuotaConfigLoader( //
            @Qualifier("DiskQuotaConfigResourceProvider")
                    GeoserverXMLResourceProvider diskQuotaConfigResourceProvider, //
            @Qualifier("gwcDefaultStorageFinder") DefaultStorageFinder storageFinder, //
            TileLayerDispatcher tld)
            throws ConfigurationException {

        return new org.geowebcache.diskquota.ConfigLoader(diskQuotaConfigResourceProvider, storageFinder, tld);
    }
}
