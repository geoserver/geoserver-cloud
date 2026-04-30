/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.core;

import static org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties.CACHE_DIRECTORY;
import static org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties.CONFIG_DIRECTORY;
import static org.geowebcache.storage.DefaultStorageFinder.GWC_CACHE_DIR;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties;
import org.geoserver.cloud.gwc.config.core.GwcRequestPathInfoFilter;
import org.geoserver.cloud.gwc.repository.CloudDefaultStorageFinder;
import org.geoserver.cloud.gwc.repository.CloudGwcXmlConfiguration;
import org.geoserver.cloud.gwc.repository.CloudXMLResourceProvider;
import org.geoserver.configuration.gwc.GwcCoreContextConfiguration;
import org.geoserver.gwc.GeoServerLockProvider;
import org.geoserver.gwc.config.AbstractGwcInitializer;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.config.GridSetConfiguration;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.XMLFileResourceProvider;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.locks.LockProvider;
import org.geowebcache.stats.RuntimeStats;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.DefaultStorageBroker;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.storage.TransientCache;
import org.geowebcache.util.ApplicationContextProvider;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * This auto-configuration only integrates the minimal components to have gwc integrated with GeoServer, while allowing
 * to disable certain components through {@link GeoWebCacheConfigurationProperties configuration properties}.
 *
 * @see GwcCoreConfiguration
 * @see DiskQuotaAutoConfiguration
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnGeoWebCacheEnabled
@EnableConfigurationProperties(GeoWebCacheConfigurationProperties.class)
@AutoConfigureAfter(CacheSeedingWebMapServiceAutoConfiguration.class)
@Import({ //
    GwcCoreContextConfiguration.class, //
    DiskQuotaAutoConfiguration.class, //
    DisquotaRestAutoConfiguration.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.core")
public class GeoWebCacheCoreAutoConfiguration {

    @PostConstruct
    public void log() {
        log.info("GeoWebCache core integration enabled");
    }

    /**
     * Revisit, excluded in GwcConfigurationTranspilerAggregator because it transpiles badly
     *
     * <pre>
     * {@snippet lang=xml :
     * <bean autowire="default" class="org.geowebcache.grid.GridSetBroker" id="gwcGridSetBroker" lazy-init="default">
     *     <constructor-arg>
     *         <list merge="default">
     *             <bean autowire="default" class="org.geowebcache.config.DefaultGridsets" id="defaultGridsets" lazy-init="default">
     *                 <!-- Should we used EPSG:900913 instead of EPSG:3857 ? -->
     *                 <constructor-arg type="boolean" value="TRUE"/>
     *                 <!--
     *             Should the default grids be named EPSG:4326 and EPSG:900913 (TRUE),
     *             or (FALSE) use the new names similar to what WMTS suggests,
     *             GlobalCRS84Geometric and GoogleMapsCompatible ?
     *            -->
     *                 <constructor-arg type="boolean" value="TRUE"/>
     *             </bean>
     *         </list>
     *     </constructor-arg>
     * </bean>
     * }
     * </pre>
     */
    @Bean
    GridSetBroker gwcGridSetBroker() {
        List<GridSetConfiguration> defaultGridsets = List.of(new DefaultGridsets(true, true));
        return new GridSetBroker(defaultGridsets);
    }

    /**
     * Revisit, excluded in GwcConfigurationTranspilerAggregator because it transpiles badly
     *
     * <pre>
     * {@snippet lang=xml :
     * <bean autowire="default" class="org.geowebcache.stats.RuntimeStats" destroy-method="destroy" id="gwcRuntimeStats" lazy-init="default">
     *     <!-- The poll interval, the number of seconds until counters are aggregated -->
     *     <constructor-arg type="int" value="3"/>
     *     <!-- The intervals (in seconds) for which aggregates are reported.
     *          Each interval must be a multiple of the poll interval above and
     *          listed in ascending order. For example, for a maximum interval
     *          of 60 seconds and 3 second poll interval from above, the amount
     *          of memory consumed is (60 / 3) * 8 = 160 bytes
     *     -->
     *     <constructor-arg>
     *         <list merge="default">
     *             <value>3</value>
     *             <value>15</value>
     *             <value>60</value>
     *         </list>
     *     </constructor-arg>
     *     <!-- Descriptive texts for each of the intervals above -->
     *     <constructor-arg>
     *         <list merge="default">
     *             <value>3 seconds</value>
     *             <value>15 seconds</value>
     *             <value>60 seconds</value>
     *         </list>
     *     </constructor-arg>
     * </bean>
     * }
     * </pre>
     */
    @Bean
    RuntimeStats gwcRuntimeStats() {
        int pollInterval = 3;
        List<Integer> intervals = List.of(3, 15, 60);
        List<String> intervalDescs = List.of("3 seconds", "15 seconds", "60 seconds");
        return new RuntimeStats(pollInterval, intervals, intervalDescs);
    }

    /**
     * Revisit, excluded in GwcConfigurationTranspilerAggregator because it transpiles badly
     *
     * <pre>
     * {@snippet lang=xml :
     * <bean autowire="default" class="org.geowebcache.storage.DefaultStorageBroker" depends-on="gwcSynchEnv" destroy-method="destroy" id="gwcStorageBroker" lazy-init="default">
     *     <constructor-arg ref="gwcBlobStore"/>
     *     <constructor-arg>
     *         <bean autowire="default" class="org.geowebcache.storage.TransientCache" lazy-init="default">
     *             <constructor-arg index="0" value="100"/>
     *             <constructor-arg index="1" value="1024"/>
     *             <constructor-arg index="2" value="2000"/>
     *         </bean>
     *     </constructor-arg>
     * </bean>
     * }
     * </pre>
     */
    @Bean
    @DependsOn("gwcSynchEnv")
    DefaultStorageBroker gwcStorageBroker(@Qualifier("gwcBlobStore") BlobStore gwcBlobStore) {
        TransientCache transientCache = new TransientCache(100, 1024, 2000);
        return new DefaultStorageBroker(gwcBlobStore, transientCache);
    }

    /**
     * @return a {@link GeoServerLockProvider} delegating the the {@link LockProvider}, which is known to be safe for
     *     distributed locking
     */
    @Bean(name = AbstractGwcInitializer.GWC_LOCK_PROVIDER_BEAN_NAME)
    org.geowebcache.locks.LockProvider gwcLockProvider(org.geoserver.platform.resource.LockProvider lockProvider) {
        GeoServerLockProvider provider = new GeoServerLockProvider();
        provider.setDelegate(lockProvider);
        return provider;
    }

    @Bean
    GwcRequestPathInfoFilter setRequestPathInfoFilter() {
        return new GwcRequestPathInfoFilter();
    }

    /**
     * There's only one way to set the default cache directory, through the {@code gwc.cache-directory} config property,
     * following standard spring-boot externalized configuration rules.
     *
     * <p>The directory will be validated to be writable, or an attempt to create it will be made if it doesn't exist.
     *
     * <p>The {@literal GEOWEBCACHE_CACHE_DIR} System Property will be forced to the cache directory once validated, for
     * interoperability with upstream's geowebcache lookup mechanism.
     *
     * @return
     * @throws FatalBeanException if the {@code gwc.cache-directory} is not provided, is not a writable directory, or
     *     can't be created
     */
    @Bean
    Path gwcDefaultCacheDirectory(GeoWebCacheConfigurationProperties config) {
        final Path directory = config.getCacheDirectory();
        log.debug("resolving default cache directory from configuration property {}={}", CACHE_DIRECTORY, directory);

        if (null == directory) {
            throw new InvalidPropertyException(
                    GeoWebCacheConfigurationProperties.class,
                    "cacheDirectory",
                    "%s is not set. The default cache directory MUST be provided.".formatted(CACHE_DIRECTORY));
        }
        validateDirectory(directory, CACHE_DIRECTORY);

        String path = directory.toAbsolutePath().toString();
        log.info("forcing System Property {}={}", GWC_CACHE_DIR, path);
        System.setProperty(GWC_CACHE_DIR, path);
        return directory;
    }

    /**
     * Resolves the location of the global {@literal geowebcache.xml} configuration file by checking the
     * {@literal gwc.config-directory} spring-boot configuration property from
     * {@link GeoWebCacheConfigurationProperties}.
     *
     * <p>This config setting is optional, and if unset defaults to the {@link ResourceStore}'s {@literal gwc/}
     * directory.
     *
     * <p>The {@literal GEOWEBCACHE_CONFIG_DIR} environment variable has no effect, as it's only used by upstream's
     * {@link XMLFileResourceProvider}, which we replace by {@link #gwcXmlConfigResourceProvider}.
     *
     * @throws BeanInitializationException if the directory supplied through the {@literal gwc.config-directory} config
     *     property is invalid
     */
    private Supplier<Resource> gwcDefaultConfigDirectory(
            GeoWebCacheConfigurationProperties config, @Qualifier("resourceStoreImpl") ResourceStore resourceStore)
            throws FatalBeanException {

        final Path directory = config.getConfigDirectory();
        final String propName = CONFIG_DIRECTORY;
        final Supplier<Resource> resource;
        if (null == directory) {
            log.debug(
                    "no {} config property found, geowebcache.xml will be loaded from the resource store's gwc/ directory",
                    propName);
            resource = () -> resourceStore.get("gwc");
        } else {
            log.debug(
                    "resolving global geowebcache.xml parent directory from configured property {}={}",
                    propName,
                    directory);
            validateDirectory(directory, propName);
            log.info("geowebcache.xml will be loaded from {} as per {}", directory, propName);
            final Resource res = Resources.fromPath(directory.toAbsolutePath().toString());
            resource = () -> res;
        }
        return resource;
    }

    /**
     * Replaces the upstream bean:
     *
     * <pre>{@code
     * <bean id="gwcXmlConfigResourceProvider" class=
     *     "org.geoserver.gwc.config.GeoserverXMLResourceProvider">
     * <constructor-arg value="geowebcache.xml" />
     * <constructor-arg ref="resourceStore" />
     * </bean>
     * }</pre>
     *
     * With one that resolves the default {@literal geowebcache.xml} file from {@link #gwcDefaultConfigDirectory}
     */
    @Bean
    ConfigurationResourceProvider gwcXmlConfigResourceProvider(
            GeoWebCacheConfigurationProperties config, @Qualifier("resourceStoreImpl") ResourceStore resourceStore) {

        Supplier<Resource> configDirSupplier = this.gwcDefaultConfigDirectory(config, resourceStore);
        return new CloudXMLResourceProvider(configDirSupplier);
    }

    /**
     *
     *
     * <pre>{@code
     * <bean id="gwcXmlConfig" class="org.geowebcache.config.XMLConfiguration">
     *   <constructor-arg ref="gwcAppCtx" />
     *   <constructor-arg ref="gwcXmlConfigResourceProvider" />
     *   <property name="template" value="/geowebcache_empty.xml">
     *     <description>Create an empty geoebcache.xml in data_dir/gwc as template</description>
     *   </property>
     * </bean>
     * }</pre>
     *
     * @param appCtx
     * @param inFac
     */
    @Bean(name = "gwcXmlConfig")
    XMLConfiguration gwcXmlConfig( //
            ApplicationContextProvider appCtx, //
            @Qualifier("gwcXmlConfigResourceProvider") ConfigurationResourceProvider inFac) {
        return new CloudGwcXmlConfiguration(appCtx, inFac);
    }

    /**
     * Define {@code DefaultStorageFinder} in code, excluded from {@literal geowebcache-servlet.xml} in the
     * {@code @ImportResource} declaration above, to make sure the cache directory environment variable or system
     * property is set up beforehand (GWC doesn't look it up in the spring application context).
     *
     * @param defaultCacheDirectory
     * @param environment
     */
    @Bean
    DefaultStorageFinder gwcDefaultStorageFinder(
            @Qualifier("gwcDefaultCacheDirectory") Path defaultCacheDirectory, Environment environment) {
        return new CloudDefaultStorageFinder(defaultCacheDirectory, environment);
    }

    /** @param directory */
    private void validateDirectory(Path directory, String configPropertyName) {
        if (!directory.isAbsolute()) {
            throw new BeanInitializationException(
                    "%s must be an absolute path: %s".formatted(configPropertyName, directory));
        }
        if (!Files.exists(directory)) {
            try {
                Path created = Files.createDirectory(directory);
                log.info("Created directory from config property {}: {}", configPropertyName, created);
            } catch (FileAlreadyExistsException _) {
                // continue
            } catch (IOException e) {
                throw new BeanInitializationException(
                        "%s does not exist and can't be created: %s".formatted(configPropertyName, directory), e);
            }
        }
        if (!Files.isDirectory(directory)) {
            throw new BeanInitializationException("%s is not a directory: %s".formatted(configPropertyName, directory));
        }
        if (!Files.isWritable(directory)) {
            throw new BeanInitializationException("%s is not writable: %s".formatted(configPropertyName, directory));
        }
    }
}
