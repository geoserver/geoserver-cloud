/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.core;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnDiskQuotaEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.DiskQuotaAutoConfiguration.DisquotaRestAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.DefaultStorageFinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/**
 * Conditionals: see {@link ConditionalOnDiskQuotaEnabled}
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = true)
@Import(DisquotaRestAutoConfiguration.class)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {
            "jar:gs-gwc-.*!/geowebcache-diskquota-context.xml#name=^(?!DiskQuotaConfigLoader).*$"
        })
public class DiskQuotaAutoConfiguration {

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

        return new org.geowebcache.diskquota.ConfigLoader(
                diskQuotaConfigResourceProvider, storageFinder, tld);
    }

    /**
     * Enables disk quota REST API if both {@link ConditionalOnDiskQuotaEnabled disk-quota} and
     * {@link ConditionalOnGeoWebCacheRestConfigEnabled res-config} are enabled.
     */
    @Configuration
    @ConditionalOnDiskQuotaEnabled
    @ConditionalOnGeoWebCacheRestConfigEnabled
    @ComponentScan(basePackages = "org.geowebcache.diskquota.rest.controller")
    static class DisquotaRestAutoConfiguration {}
}
