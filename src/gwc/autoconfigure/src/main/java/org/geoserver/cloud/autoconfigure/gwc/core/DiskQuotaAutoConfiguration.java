/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.core;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnDiskQuotaEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties;
import org.geoserver.cloud.autoconfigure.gwc.core.DiskQuotaAutoConfiguration.DisquotaWebUIAutoConfiguration;
import org.geoserver.configuration.gwc.GwcDiskQuotaContextConfiguration;
import org.geoserver.configuration.gwc.GwcDiskQuotaRestConfiguration;
import org.geoserver.configuration.gwc.GwcDiskQuotaWebUIConfiguration;
import org.geoserver.gwc.ConfigurableQuotaStoreProvider;
import org.geoserver.gwc.JDBCConfigurationStorage;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.DefaultStorageFinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Imports the transpiled {@link GwcDiskQuotaContextConfiguration}, contributing the {@code DiskQuotaMonitor} and
 * friends that {@code gwcFacade} (in {@link org.geoserver.configuration.gwc.GwcGeoServerContextConfiguration}) and the
 * GeoServer Wicket UI hold hard references to.
 *
 * <p>Whether the monitor actually runs at startup is controlled at runtime by the {@code GWC_DISKQUOTA_DISABLED}
 * environment variable, which the upstream {@code DiskQuotaMonitor} and {@code ConfigurableQuotaStoreProvider} both
 * read in their constructors. {@link #diskQuotaDisabledPropertySetter(Environment)} sets it to {@code true} when
 * {@code gwc.disk-quota.enabled} is unset or {@code false} (the default) and clears it otherwise.
 *
 * <p>{@code DiskQuotaStoreProvider} is excluded from {@link GwcDiskQuotaContextConfiguration}: a backend-specific
 * auto-configuration supplies a {@link ConfigurableQuotaStoreProvider} subclass that knows where the cluster-safe quota
 * store lives (e.g. {@code PgconfigQuotaStoreProvider} for the pgconfig backend). When no backend supplies one,
 * {@link #fallbackDiskQuotaStoreProvider} below registers the upstream class directly - harmless in the
 * disabled-by-default case because the system-property gate keeps it from doing any work.
 *
 * @see GwcDiskQuotaContextConfiguration
 * @see GwcDiskQuotaRestConfiguration
 * @see ConditionalOnDiskQuotaEnabled
 * @see ConditionalOnGeoWebCacheRestConfigEnabled
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Import({GwcDiskQuotaContextConfiguration.class, DisquotaWebUIAutoConfiguration.class})
@SuppressWarnings({"java:S1118", "java:S6830"})
public class DiskQuotaAutoConfiguration {

    /**
     * Sets the {@code GWC_DISKQUOTA_DISABLED} system property based on the {@code gwc.disk-quota.enabled} configuration
     * property, before any beans are instantiated.
     *
     * <p>{@code DiskQuotaMonitor} and {@code ConfigurableQuotaStoreProvider} both read {@code GWC_DISKQUOTA_DISABLED}
     * in their constructors and lock in {@code diskQuotaEnabled = !disabled}. By then it is too late for a regular bean
     * post-processor to flip the switch - so this {@code BeanFactoryPostProcessor} runs during bean factory
     * post-processing, well before any singleton is instantiated.
     *
     * <p>Both branches are explicit so a previous context's setting (e.g. an earlier test in the same JVM) cannot leak
     * into the current bean topology.
     *
     * <p>The method is intentionally {@code static}: a non-static {@code @Bean} factory method for a
     * {@code BeanFactoryPostProcessor} would force its declaring class to be instantiated eagerly, ahead of
     * post-processing.
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
     * Override {@literal DiskQuotaConfigLoader}, {@code GwcConfigurationTranspilerAggregator} chooses the wrong
     * constructor so it's excluded there
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

    /**
     * Fallback {@code DiskQuotaStoreProvider} bean used when no backend-specific auto-configuration registers one (e.g.
     * when {@code gwc.disk-quota.enabled=false} or no compatible backend is active).
     *
     * <p>Uses the upstream {@link ConfigurableQuotaStoreProvider} directly so the GeoServer Wicket UI's
     * {@code DiskQuotaWarningPanel} can resolve it by type. Its {@code reloadQuotaStore()} is a no-op when
     * {@code GWC_DISKQUOTA_DISABLED=true} - the gate that {@link #diskQuotaDisabledPropertySetter} sets in the
     * disabled-by-default case.
     */
    @Bean(name = "DiskQuotaStoreProvider")
    @ConditionalOnMissingBean(name = "DiskQuotaStoreProvider")
    ConfigurableQuotaStoreProvider fallbackDiskQuotaStoreProvider(
            @Qualifier("DiskQuotaConfigLoader") ConfigLoader loader,
            @Qualifier("gwcTilePageCalculator") TilePageCalculator calculator,
            @Qualifier("gwcJdbcConfigurationStorage") JDBCConfigurationStorage jdbcConfigStorage) {
        return new ConfigurableQuotaStoreProvider(loader, calculator, jdbcConfigStorage);
    }

    /**
     * Contributes the Server -> DiskQuota Wicket admin page ({@code diskQuotaMenuPage}) when both
     * {@link ConditionalOnDiskQuotaEnabled disk-quota} and {@link ConditionalOnGeoServerWebUIEnabled the GeoServer
     * web-ui} are enabled.
     *
     * <p>The page bean is excluded from the main {@code GwcGeoServerWebUIConfiguration} and emitted separately as
     * {@link GwcDiskQuotaWebUIConfiguration} so it can be gated at runtime here rather than always wired by the
     * compile-time transpiler.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnDiskQuotaEnabled
    @ConditionalOnGeoServerWebUIEnabled
    @Import(GwcDiskQuotaWebUIConfiguration.class)
    static class DisquotaWebUIAutoConfiguration {}
}
