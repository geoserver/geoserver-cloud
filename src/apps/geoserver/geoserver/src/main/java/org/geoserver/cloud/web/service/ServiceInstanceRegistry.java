/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web.service;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulServiceInstance;

/** @since 1.0 */
@RequiredArgsConstructor
public class ServiceInstanceRegistry {

    private final @NonNull DiscoveryClient client;

    /** @return All known service IDs */
    public List<String> getServiceNames() {
        return client.getServices();
    }

    public Stream<ServiceInstance> getServices() {
        return client.getServices().stream()
                .map(client::getInstances)
                .map(List::stream)
                .flatMap(Function.identity())
                .map(this::toService);
    }

    private ServiceInstance toService(org.springframework.cloud.client.ServiceInstance i) {
        ServiceInstance s = new ServiceInstance();
        s.setName(i.getServiceId());
        s.setInstanceId(i.getInstanceId());
        s.setUri(buildUrl(i));
        s.setStatus(getStatus(i));
        return s;
    }

    private String getStatus(org.springframework.cloud.client.ServiceInstance i) {
        if (i instanceof ConsulServiceInstance) {
            // ConsulServiceInstance doesn't expose a direct "status" string like Consul,
            // but we can assume UP if it's returned by the discovery client.
            return "UP";
        }
        return "UNKNOWN";
    }

    private String buildUrl(org.springframework.cloud.client.ServiceInstance i) {
        URI uri = i.getUri();
        if (uri == null) {
            uri = URI.create("%s://%s:%s".formatted(i.getScheme(), i.getHost(), i.getPort()));
        }
        return uri.toString();
    }
}
