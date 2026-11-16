/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;

/**
 * Connection pool of the HTTP client used by the gateway to proxy requests to the services.
 *
 * <p>Spring Cloud Gateway Server MVC has no settings for this pool. This class binds the {@code httpclient.pool}
 * properties of the WebFlux gateway under {@code spring.cloud.gateway.server.webmvc.httpclient.pool}, with defaults
 * chosen for the GeoServer services. The gateway keeps one pool for each upstream address.
 *
 * <pre>{@code
 * spring.cloud.gateway.server.webmvc.httpclient.pool:
 *   max-connections: 500
 *   acquire-timeout: 10s
 *   max-idle-time: 30s
 *   eviction-interval: 15s
 *   metrics: true
 * }</pre>
 *
 * @see GatewayHttpClientAutoConfiguration
 * @since 3.1.0
 */
@ConfigurationProperties(GatewayMvcProperties.PREFIX + ".httpclient.pool")
@Data
public class HttpClientPoolProperties {

    /**
     * Maximum number of connections to each upstream address. Further requests wait for a connection. Behind a
     * Kubernetes Service, the pods of a service share the address of the Service, and these connections.
     */
    private int maxConnections = 500;

    /**
     * Maximum time a request waits for a connection, then it fails. Keep it no longer than the queue timeout of the
     * control-flow extension: requests should wait in the services, where control-flow sees them, rather than in the
     * gateway.
     */
    private Duration acquireTimeout = Duration.ofSeconds(10);

    /**
     * Idle time after which a connection is not reused. Must be shorter than the keep-alive timeout of the services, 60
     * seconds in Tomcat. Otherwise the gateway can send a request on a connection being closed by the service, and the
     * request fails. Unset means no limit.
     */
    private Duration maxIdleTime = Duration.ofSeconds(30);

    /**
     * Interval of the background task closing the connections idle for longer than the max idle time. Zero disables it.
     */
    private Duration evictionInterval = Duration.ofSeconds(15);

    /** Whether to publish the pool metrics to Micrometer, named {@code reactor.netty.connection.provider.*}. */
    private boolean metrics = true;
}
