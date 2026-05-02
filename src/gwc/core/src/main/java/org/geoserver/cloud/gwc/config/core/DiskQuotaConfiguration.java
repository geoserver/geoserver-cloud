/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.DefaultStorageFinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Imports the upstream {@code geowebcache-diskquota-context.xml}, contributing the {@code
 * DiskQuotaMonitor}, {@code DiskQuotaStoreProvider} and friends that {@code gwcFacade} (in
 * {@code geowebcache-geoserver-context.xml}) and the GeoServer Wicket UI hold hard references to.
 *
 * <p>Whether the monitor actually runs at startup is controlled at runtime by the {@code
 * GWC_DISKQUOTA_DISABLED} environment variable, which the upstream {@code DiskQuotaMonitor} and
 * {@code ConfigurableQuotaStoreProvider} both read in their constructors. {@link
 * #diskQuotaDisabledPropertySetter(Environment)} sets it to {@code true} when {@code
 * gwc.disk-quota.enabled} is unset or {@code false} (the default) and clears it otherwise. The
 * bean topology stays the same in both modes; only runtime behavior changes.
 *
 * <p>{@code DiskQuotaConfigLoader} is replaced below to drop the upstream {@code metaStoreRemover}
 * dependency that doesn't exist in the cloud topology.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ImportFilteredResource("jar:gs-gwc-[0-9]+.*!/geowebcache-diskquota-context.xml#name=^(?!DiskQuotaConfigLoader).*$")
public class DiskQuotaConfiguration {

    /**
     * Sets the {@code GWC_DISKQUOTA_DISABLED} system property based on the {@code
     * gwc.disk-quota.enabled} configuration property, before any beans are instantiated.
     *
     * <p>{@code DiskQuotaMonitor} and {@code ConfigurableQuotaStoreProvider} both read
     * {@code GWC_DISKQUOTA_DISABLED} in their constructors and lock in
     * {@code diskQuotaEnabled = !disabled}. By then it is too late for a regular bean
     * post-processor to flip the switch - so this {@code BeanFactoryPostProcessor} runs during
     * bean factory post-processing, well before any singleton is instantiated.
     *
     * <p>Both branches are explicit so a previous context's setting (e.g. an earlier test in the
     * same JVM) cannot leak into the current bean topology.
     *
     * <p>The method is intentionally {@code static}: a non-static {@code @Bean} factory method
     * for a {@code BeanFactoryPostProcessor} would force its declaring class to be instantiated
     * eagerly, ahead of post-processing, with subtle side effects.
     */
    @Bean
    static BeanFactoryPostProcessor diskQuotaDisabledPropertySetter(Environment environment) {
        boolean enabled = environment.getProperty(
                GeoWebCacheConfigurationProperties.DISKQUOTA_ENABLED, Boolean.class, Boolean.FALSE);
        if (enabled) {
            System.clearProperty(DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED);
        } else {
            System.setProperty(DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED, "true");
        }
        return beanFactory -> {
            // no-op: the side effect of setting the system property happened before this lambda
        };
    }

    /**
     * Override {@literal DiskQuotaConfigLoader} not to depend on the excluded {@literal
     * metaStoreRemover}
     */
    @Bean(name = "DiskQuotaConfigLoader")
    ConfigLoader diskQuotaConfigLoader( //
            @Qualifier("DiskQuotaConfigResourceProvider")
                    GeoserverXMLResourceProvider diskQuotaConfigResourceProvider, //
            @Qualifier("gwcDefaultStorageFinder") DefaultStorageFinder storageFinder, //
            TileLayerDispatcher tld)
            throws ConfigurationException {

        return new ConfigLoader(diskQuotaConfigResourceProvider, storageFinder, tld);
    }
}
