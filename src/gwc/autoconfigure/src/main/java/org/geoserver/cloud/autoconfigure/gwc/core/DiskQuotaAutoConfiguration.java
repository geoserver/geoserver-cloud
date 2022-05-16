/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.core;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnDiskQuotaEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.DiskQuotaAutoConfiguration.DisquotaRestAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.DiskQuotaConfiguration;
import org.geoserver.cloud.gwc.config.core.DisquotaRestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see DiskQuotaConfiguration
 * @see DisquotaRestConfiguration
 * @see ConditionalOnDiskQuotaEnabled
 * @see ConditionalOnGeoWebCacheRestConfigEnabled
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Import({DiskQuotaConfiguration.class, DisquotaRestAutoConfiguration.class})
public class DiskQuotaAutoConfiguration {

    /**
     * Enables disk quota REST API if both {@link ConditionalOnDiskQuotaEnabled disk-quota} and
     * {@link ConditionalOnGeoWebCacheRestConfigEnabled rest-config} are enabled.
     */
    @Configuration
    @ConditionalOnDiskQuotaEnabled
    @ConditionalOnGeoWebCacheRestConfigEnabled
    @Import(DisquotaRestConfiguration.class)
    static class DisquotaRestAutoConfiguration {}
}
