/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ReactorClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Makes the gateway reach the services through a connection pool configured by {@link HttpClientPoolProperties}.
 *
 * <p>Without it, the gateway uses the global connection pool of Reactor Netty. That pool keeps idle connections with no
 * time limit, while Tomcat on the services closes a connection after 60 seconds without requests. A request sent on a
 * connection being closed by Tomcat fails, and the gateway answers HTTP 500.
 *
 * @since 3.1.0
 */
@AutoConfiguration
@ConditionalOnClass({HttpClient.class, ReactorClientHttpRequestFactoryBuilder.class})
@EnableConfigurationProperties(HttpClientPoolProperties.class)
public class GatewayHttpClientAutoConfiguration {

    /** Name of the pool, and {@code name} tag of its metrics, as in the WebFlux gateway. */
    static final String POOL_NAME = "proxy";

    @Bean(destroyMethod = "dispose")
    @ConditionalOnMissingBean
    ConnectionProvider gatewayConnectionProvider(HttpClientPoolProperties pool) {
        ConnectionProvider.Builder builder = ConnectionProvider.builder(POOL_NAME)
                .maxConnections(pool.getMaxConnections())
                .pendingAcquireTimeout(pool.getAcquireTimeout())
                .evictInBackground(pool.getEvictionInterval())
                .metrics(pool.isMetrics());
        if (pool.getMaxIdleTime() != null) {
            builder.maxIdleTime(pool.getMaxIdleTime());
        }
        return builder.build();
    }

    /** Makes the HTTP client of the gateway, built by Spring Boot, use the {@code gatewayConnectionProvider} pool. */
    @Bean
    ClientHttpRequestFactoryBuilderCustomizer<ReactorClientHttpRequestFactoryBuilder> gatewayConnectionPoolCustomizer(
            ConnectionProvider gatewayConnectionProvider) {
        return builder -> builder.withHttpClientFactory(() -> HttpClient.create(gatewayConnectionProvider));
    }
}
