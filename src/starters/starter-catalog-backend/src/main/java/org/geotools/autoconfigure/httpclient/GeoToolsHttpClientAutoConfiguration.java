/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.autoconfigure.httpclient;

import lombok.extern.slf4j.Slf4j;

import org.geotools.autoconfigure.httpclient.ProxyConfig.ProxyHostConfig;
import org.geotools.http.HTTPClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} auto configuration for a GeoTools {@link
 * HTTPClientFactory} that can be configured through spring-boot externalized properties and only
 * affects GeoTools http clients instead of the whole JVM.
 *
 * <p>The usual way to set an http proxy is through the {@literal http.proxyHost}, {@literal
 * http.proxyPort}, {@literal http.proxyUser}, {@literal http.proxyPassword} Java System Properties.
 *
 * <p>In the context of Cloud Native GeoServer containerized applications, this has a number of
 * drawbacks:
 *
 * <ul>
 *   <li>Standard java proxy parameters only work with System properties, not env variables (at
 *       least with the apache http client), and setting system properties is more cumbersome than
 *       env variables (you have to modify the container run command)
 *   <li>{@literal http.proxyUser/Password} are not standard properties, though commonly used, it's
 *       kind of JDK implementation dependent.
 *   <li>Setting {@literal -Dhtt.proxy*} System properties affects all HTTP clients in the
 *       container, meaning requests to the {@literal config-service}, {@literal discovery-service},
 *       etc., will also try to go through the proxy, or you need to go through the extra burden of
 *       figuring out how to ignore them.
 *   <li>If the proxy is secured, and since the http client used may not respect the {@literal
 *       http.proxyUser/Password} parameters, the apps won't start since they'll get HTTP 407 "Proxy
 *       Authentication Required".
 * </ul>
 *
 * <p>The following externalized configuration properties apply:
 *
 * <pre>
 * <code>
 * geotools:
 *   httpclient:
 *     proxy:
 *       # defaults to true, false disables the autoconfiguration and falls back to standard GeoServer behavior
 *       enabled: true
 *       http:
 *         host:
 *         port:
 *         user:
 *         password:
 *         nonProxyHosts:
 *         # comma separated list of Java regular expressions, e.g.: nonProxyHosts: localhost, example.*
 *       https:
 *         host:
 *         port:
 *         user:
 *         password:
 *         nonProxyHosts:
 * </code>
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnProperty(
        prefix = "geotools.httpclient.proxy",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j(topic = "org.geotools.autoconfigure.httpclient")
public class GeoToolsHttpClientAutoConfiguration {

    @ConfigurationProperties(prefix = "geotools.httpclient.proxy")
    public @Bean ProxyConfig geoToolsHttpProxyConfiguration() {
        System.setProperty(
                "HTTP_CLIENT_FACTORY",
                SpringEnvironmentAwareGeoToolsHttpClientFactory.class.getCanonicalName());
        return new ProxyConfig();
    }

    public @Bean SpringEnvironmentAwareGeoToolsHttpClientFactory
            springEnvironmentAwareGeoToolsHttpClientFactory(@Autowired ProxyConfig proxyConfig) {

        log.info("Using spring environment aware GeoTools HTTPClientFactory");
        log(proxyConfig.getHttp(), "HTTP");
        log(proxyConfig.getHttps(), "HTTPS");
        SpringEnvironmentAwareGeoToolsHttpClientFactory.setProxyConfig(proxyConfig);

        return new SpringEnvironmentAwareGeoToolsHttpClientFactory();
    }

    private void log(ProxyHostConfig config, String protocol) {
        config.host()
                .ifPresentOrElse(
                        host ->
                                log.info(
                                        "{} proxy configured for GeoTools cascaded OWS stores: {}:{}, secured: {}",
                                        protocol,
                                        host,
                                        config.port(),
                                        config.isSecured()),
                        () ->
                                log.info(
                                        "No {} proxy configured for GeoTools cascaded OWS stores",
                                        protocol));
    }
}
