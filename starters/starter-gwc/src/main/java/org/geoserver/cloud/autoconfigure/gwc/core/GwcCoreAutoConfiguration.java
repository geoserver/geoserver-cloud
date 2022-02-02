/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.core;

import static org.geowebcache.config.XMLFileResourceProvider.GWC_CONFIG_DIR_VAR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.integration.SeedingWMSAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.gwc.controller.GeoWebCacheController;
import org.geoserver.cloud.gwc.repository.CloudGwcXmlConfiguration;
import org.geoserver.cloud.gwc.repository.CloudXMLResourceProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.util.ApplicationContextProvider;
import org.geowebcache.util.GWCVars;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Replaces upstream's {@literal geowebcache-servlet.xml} which is an aggregate of several xml
 * files.
 * <p>
 * The original {@literal geowebcache-servlet.xml}:
 *
 * <pre>
 * {@code
 *   <import resource="geowebcache-core-context.xml"/>
 *   <import resource="geowebcache-georss-context.xml"/>
 *   <import resource="geowebcache-gmaps-context.xml"/>
 *   <import resource="geowebcache-kmlservice-context.xml"/>
 *   <import resource="geowebcache-rest-context.xml"/>
 *   <import resource="geowebcache-tmsservice-context.xml"/>
 *   <import resource="geowebcache-virtualearth-context.xml"/>
 *   <import resource="geowebcache-wmsservice-context.xml"/>
 *   <import resource="geowebcache-wmtsservice-context.xml"/>
 *   <import resource="geowebcache-diskquota-context.xml"/>
 *   <!--
 *     This mappings are different from the standalone gwc ones in that they prepend the /gwc prefix to the context so it
 *     ends up being, for example, /geoserver/gwc/*
 *   -->
 *   <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
 *     <property name="ignoreUnresolvablePlaceholders" value="true" />
 *     <property name="location">
 *       <value>classpath:application.properties</value>
 *     </property>
 *   </bean>
 *   <context:component-scan base-package="org.geoserver.gwc.dispatch"/>
 * }
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = true)
@AutoConfigureAfter(SeedingWMSAutoConfiguration.class)
@ImportResource(
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = {
        "jar:gs-gwc-.*!/geowebcache-servlet.xml#name=^(?!gwcXmlConfig|gwcDefaultStorageFinder).*$"
    }
)
@Slf4j(topic = "org.geoserver.cloud.gwc.autoconfigure")
public class GwcCoreAutoConfiguration {

    private @Autowired ApplicationContext appContext;
    private @Value("${gwc.cache-directory:}") Path cacheDirectory;

    static {
        /*
         * Disable disk-quota by brute force for now. We need to resolve how and where to store the
         * configuration and database.
         */
        System.setProperty(DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED, "true");
    }

    @Bean
    GeoWebCacheController gwcController() {
        return new GeoWebCacheController();
    }

    @Bean
    SetRequestPathInfoFilter setRequestPathInfoFilter() {
        return new SetRequestPathInfoFilter();
    }

    @Bean
    @ConditionalOnMissingBean(RequestMappingHandlerMapping.class)
    RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping();
    }

    /**
     *
     *
     * <pre>{@code
     * <bean id="gwcXmlConfigResourceProvider" class=
     *     "org.geoserver.gwc.config.GeoserverXMLResourceProvider">
     * <constructor-arg value="geowebcache.xml" />
     * <constructor-arg ref="resourceStore" />
     * </bean>
     * }</pre>
     *
     * @param resourceStore
     * @throws ConfigurationException
     */
    public @Bean ConfigurationResourceProvider gwcXmlConfigResourceProvider(
            @Qualifier("resourceStoreImpl") ResourceStore resourceStore)
            throws ConfigurationException {

        Supplier<Resource> configDirectory = findConfigDirectory(resourceStore);
        return new CloudXMLResourceProvider(configDirectory);
    }

    /**
     * Only resolves the {@literal GEOWEBCACHE_CONFIG_DIR} environment variable, {@literal
     * GEOWEBCACHE_CACHE_DIR} is not considered, for separation of concerns (i.e. keep config in
     * {@link ResourceStore}, separate from cached tile images). If not provided, deafults to the
     * resource store's {@literal /gwc} directory.
     *
     * @throws BeanInitializationException if the directory supplied through the {@literal
     *     GEOWEBCACHE_CONFIG_DIR} is invalid
     */
    private Supplier<Resource> findConfigDirectory(ResourceStore resourceStore)
            throws FatalBeanException {
        Environment env = appContext.getEnvironment();
        final String dirLocationEnvVar = GWC_CONFIG_DIR_VAR;
        final String dirLocation = env.getProperty(dirLocationEnvVar);
        if (null == dirLocation) {
            log.debug(
                    "no {} env variable found, gwc configuration will be loaded from the resource store's gwc/ directory",
                    dirLocationEnvVar);
            return () -> resourceStore.get("gwc");
        }
        log.info(
                "found {} env variable, gwc configuration will be loaded from {}",
                dirLocationEnvVar,
                dirLocation);
        File configurationDirectory = new File(dirLocation);
        if (!configurationDirectory.isAbsolute()) {
            throw new BeanInitializationException(
                    "GEOWEBCACHE_CONFIG_DIR must point to an absolute path: " + dirLocation);
        }
        if (!configurationDirectory.isDirectory() || !configurationDirectory.canWrite()) {
            throw new BeanInitializationException(
                    dirLocation + " is not a directory or is not writable");
        }
        Resource resource = Resources.fromPath(configurationDirectory.getAbsolutePath());
        return () -> resource;
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
    public XMLConfiguration gwcXmlConfig( //
            ApplicationContextProvider appCtx, //
            @Qualifier("gwcXmlConfigResourceProvider") ConfigurationResourceProvider inFac) {
        return new CloudGwcXmlConfiguration(appCtx, inFac);
    }

    /**
     * Define {@code DefaultStorageFinder} in code, excluded from {@literal geowebcache-servlet.xml}
     * in the {@code @ImportResource} declaration above, to make sure the cache directory
     * environment variable or system property is set up beforehand (GWC doesn't look it up in the
     * spring application context).
     */
    public @Bean DefaultStorageFinder gwcDefaultStorageFinder( //
            ApplicationContextProvider provider) {
        initGeowebCacheDirEnvVariable();
        return new DefaultStorageFinder(provider);
    }

    public void initGeowebCacheDirEnvVariable() {

        final String cacheDirVar = DefaultStorageFinder.GWC_CACHE_DIR;
        final String foundAsEnvOrSysProperty = findCacheDirAsEnvOrSysProperty();

        if (null != foundAsEnvOrSysProperty) {
            if (null != cacheDirectory) {
                log.warn(
                        "Ignoring gwc.cache-directory property, cache directory found as System Property or environment variable");
            }
            return;
        }
        if (null == cacheDirectory) {
            throw new ApplicationContextException(
                    "GeoWebCache cache directory not found as system property, "
                            + "environment variable, or gwc.cache-directory "
                            + "spring environment property");
        }
        String path = cacheDirectory.toAbsolutePath().toString();
        log.info("Forcing System Property {}={}", cacheDirVar, path);
        System.setProperty(cacheDirVar, path);
    }

    private String findCacheDirAsEnvOrSysProperty() {
        return Optional.ofNullable(
                        GWCVars.findEnvVar(appContext, DefaultStorageFinder.GWC_CACHE_DIR))
                .orElseGet(() -> GWCVars.findEnvVar(appContext, DefaultStorageFinder.GS_DATA_DIR));
    }

    /**
     * Servlet filter that proceeds with an {@link HttpServletRequestWrapper} decorator to return
     * {@link HttpServletRequestWrapper#getPathInfo() getPathInfo()} built from {@link
     * HttpServletRequestWrapper#getRequestURI() getRequestURI()}.
     *
     * <p>GWC makes heavy use of {@link HttpServletRequestWrapper#getPathInfo()}, but it returns
     * {@code null} in a spring-boot application.
     *
     * @since 1.0
     */
    static class SetRequestPathInfoFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            request = adaptRequest((HttpServletRequest) request);
            chain.doFilter(request, response);
        }

        protected ServletRequest adaptRequest(HttpServletRequest request) {
            final String suffix = "/gwc";
            final String requestURI = request.getRequestURI();
            if (requestURI.startsWith(suffix)) {
                return new HttpServletRequestWrapper(request) {
                    public @Override String getPathInfo() {
                        String requestURI = request.getRequestURI();
                        return requestURI.substring(suffix.length());
                    }
                };
            }
            return request;
        }
    }
}
