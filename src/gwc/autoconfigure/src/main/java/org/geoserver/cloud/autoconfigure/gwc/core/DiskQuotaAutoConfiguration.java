/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.core;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnDiskQuotaEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.DiskQuotaAutoConfiguration.DisquotaRestAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.core.DiskQuotaAutoConfiguration.DisquotaWebUIAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.DiskQuotaConfiguration;
import org.geoserver.cloud.gwc.config.core.DisquotaRestConfiguration;
import org.geoserver.cloud.gwc.config.core.DisquotaWebUIConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Disk-quota auto-configuration.
 *
 * <p>
 * {@link DiskQuotaConfiguration} is imported unconditionally - it contributes
 * the {@code
 * DiskQuotaMonitor} bean that {@code gwcFacade} (from
 * {@code geowebcache-geoserver-context.xml}) keeps a hard reference to, so the
 * bean must always exist. Whether the monitor runs at startup is controlled by
 * the {@code GWC_DISKQUOTA_DISABLED} system property, set conditionally by
 * {@code DiskQuotaConfiguration#diskQuotaDisabledPropertySetter} based on
 * {@code
 * gwc.disk-quota.enabled}.
 *
 * <p>
 * The REST controller is enabled separately by
 * {@link DisquotaRestAutoConfiguration} when both
 * {@link ConditionalOnDiskQuotaEnabled disk-quota} and
 * {@link ConditionalOnGeoWebCacheRestConfigEnabled rest-config} are enabled.
 *
 * <p>
 * A backend-specific auto-configuration (e.g.
 * {@code PgconfigDiskQuotaAutoConfiguration}) is required to override the
 * default {@code DiskQuotaStoreProvider} with one that points at a cluster-safe
 * quota store. Without that override and with
 * {@code gwc.disk-quota.enabled=true}, the upstream
 * {@code ConfigurableQuotaStoreProvider} would try to load a BDB-backed store,
 * which isn't viable in a multi-pod deployment.
 *
 * @see DiskQuotaConfiguration
 * @see DisquotaRestConfiguration
 * @see ConditionalOnDiskQuotaEnabled
 * @see ConditionalOnGeoWebCacheRestConfigEnabled
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Import({DiskQuotaConfiguration.class, DisquotaRestAutoConfiguration.class, DisquotaWebUIAutoConfiguration.class})
@SuppressWarnings("java:S1118")
public class DiskQuotaAutoConfiguration {

    /**
     * Enables disk quota REST API if both {@link ConditionalOnDiskQuotaEnabled
     * disk-quota} and {@link ConditionalOnGeoWebCacheRestConfigEnabled rest-config}
     * are enabled.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnDiskQuotaEnabled
    @ConditionalOnGeoWebCacheRestConfigEnabled
    @Import(DisquotaRestConfiguration.class)
    static class DisquotaRestAutoConfiguration {}

    /**
     * Enables disk quota Wicket components if both
     * {@link ConditionalOnDiskQuotaEnabled} and
     * {@link ConditionalOnGeoServerWebUIEnabled} are enabled.
     * @see DisquotaWebUIConfiguration
     * @since 2.28.3.1
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnDiskQuotaEnabled
    @ConditionalOnGeoServerWebUIEnabled
    @Import(DisquotaWebUIConfiguration.class)
    static class DisquotaWebUIAutoConfiguration {}
}
