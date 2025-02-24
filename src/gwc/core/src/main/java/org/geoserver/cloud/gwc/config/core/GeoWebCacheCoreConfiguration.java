/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import static org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties.CACHE_DIRECTORY;
import static org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties.CONFIG_DIRECTORY;
import static org.geowebcache.storage.DefaultStorageFinder.GWC_CACHE_DIR;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.gwc.repository.CloudDefaultStorageFinder;
import org.geoserver.cloud.gwc.repository.CloudGwcXmlConfiguration;
import org.geoserver.cloud.gwc.repository.CloudXMLResourceProvider;
import org.geoserver.gwc.GeoServerLockProvider;
import org.geoserver.gwc.config.AbstractGwcInitializer;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.XMLFileResourceProvider;
import org.geowebcache.locks.LockProvider;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.util.ApplicationContextProvider;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties(GeoWebCacheConfigurationProperties.class)
@ImportFilteredResource(
        "jar:gs-gwc-[0-9]+.*!/geowebcache-core-context.xml#name=^(?!gwcXmlConfig|gwcDefaultStorageFinder|metastoreRemover).*$")
@Slf4j(topic = "org.geoserver.cloud.gwc.config.core")
public class GeoWebCacheCoreConfiguration {

    /**
     * @return a {@link GeoServerLockProvider} delegating the the {@link ResourceStore}, whose
     *     {@link ResourceStore#getLockProvider()} is known to be cluster-capable
     */
    @Bean(name = AbstractGwcInitializer.GWC_LOCK_PROVIDER_BEAN_NAME)
    LockProvider gwcLockProvider(@Qualifier("resourceStoreImpl") ResourceStore resourceStore) {
        var provider = new GeoServerLockProvider();
        provider.setDelegate(resourceStore);
        return provider;
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
     * There's only one way to set the default cache directory, through the {@code
     * gwc.cache-directory} config property, following standard spring-boot externalized
     * configuration rules.
     *
     * <p>The directory will be validated to be writable, or an attempt to create it will be made if
     * it doesn't exist.
     *
     * <p>The {@literal GEOWEBCACHE_CACHE_DIR} System Property will be forced to the cache directory
     * once validated, for interoperability with upstream's geowebcache lookup mechanism.
     *
     * @return
     * @throws FatalBeanException if the {@code gwc.cache-directory} is not provided, is not a
     *     writable directory, or can't be created
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
     * Resolves the location of the global {@literal geowebcache.xml} configuration file by checking
     * the {@literal gwc.config-directory} spring-boot configuration property from {@link
     * GeoWebCacheConfigurationProperties}.
     *
     * <p>This config setting is optional, and if unset defaults to the {@link ResourceStore}'s
     * {@literal gwc/} directory.
     *
     * <p>The {@literal GEOWEBCACHE_CONFIG_DIR} environment variable has no effect, as it's only
     * used by upstream's {@link XMLFileResourceProvider}, which we replace by {@link
     * #gwcXmlConfigResourceProvider}.
     *
     * @throws BeanInitializationException if the directory supplied through the {@literal
     *     gwc.config-directory} config property is invalid
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
     * With one that resolves the default {@literal geowebcache.xml} file from {@link
     * #gwcDefaultConfigDirectory}
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
     * Define {@code DefaultStorageFinder} in code, excluded from {@literal geowebcache-servlet.xml}
     * in the {@code @ImportResource} declaration above, to make sure the cache directory
     * environment variable or system property is set up beforehand (GWC doesn't look it up in the
     * spring application context).
     *
     * @param defaultCacheDirectory
     * @param environment
     */
    @Bean
    DefaultStorageFinder gwcDefaultStorageFinder(
            @Qualifier("gwcDefaultCacheDirectory") Path defaultCacheDirectory, Environment environment) {
        return new CloudDefaultStorageFinder(defaultCacheDirectory, environment);
    }

    /**
     * @param directory
     */
    private void validateDirectory(Path directory, String configPropertyName) {
        if (!directory.isAbsolute()) {
            throw new BeanInitializationException(
                    "%s must be an absolute path: %s".formatted(configPropertyName, directory));
        }
        if (!Files.exists(directory)) {
            try {
                Path created = Files.createDirectory(directory);
                log.info("Created directory from config property {}: {}", configPropertyName, created);
            } catch (FileAlreadyExistsException beatenByOtherInstance) {
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

        /**
         *
         *
         * <ul>
         *   <li>{@code contextPath} is usually empty, but it'll match the gateways
         *       ${geoserver.base-path} if such is set, thanks to the
         *       server.forward-headers-strategy=framework in the application's bootstrap.yml
         *   <li>{@code pathInfo} is computed beforehand to avoid decorating the HttpServletRequest
         *       if the request is not for gwc (e.g. an actuator endpoint)
         *   <li>{@code suffix} is used to strip it out of requestURI and fake the pathInfo gwc
         *       expects as it assumes the request is being handled by a Dispatcher mapped to /**
         *   <li>yes, this is odd, the alternative is re-writing GWC without weird assumptions
         * </ul>
         */
        protected ServletRequest adaptRequest(HttpServletRequest request) {
            // full request URI (e.g. '/geoserver/cloud/{workspace}/gwc/service/tms/1.0.0',
            // where
            // '/geoserver/cloud' is the context path as given by the gateway's base uri,
            // and
            // '/{workspace}/gwc' the suffix after which comes the pathInfo
            // '/service/tms/1.0.0')
            final String requestURI = request.getRequestURI();

            final String gwc = "/gwc";
            final int gwcIdx = requestURI.indexOf(gwc);
            if (gwcIdx > -1) {
                final String pathToGwc = requestURI.substring(0, gwcIdx + gwc.length());
                final String pathInfo = requestURI.substring(pathToGwc.length());

                return new HttpServletRequestWrapper(request) {
                    @Override
                    public String getPathInfo() {
                        return pathInfo;
                    }
                };
            }
            return request;
        }
    }
}
