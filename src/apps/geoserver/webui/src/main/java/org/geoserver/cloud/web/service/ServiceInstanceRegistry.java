/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class ServiceInstanceRegistry {

    private final @NonNull DiscoveryClient client;

    /**
     * @return All known service IDs
     */
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
        if (i instanceof EurekaServiceInstance e) {
            InstanceInfo instanceInfo = e.getInstanceInfo();
            InstanceStatus status = instanceInfo.getStatus();
            return status.toString();
        }
        // Map<String, String> metadata = i.getMetadata();
        return "UNKNOWN";
    }

    private String buildUrl(org.springframework.cloud.client.ServiceInstance i) {
        URI uri = i.getUri();
        if (uri == null) {
            uri = URI.create(String.format("%s://%s:%s", i.getScheme(), i.getHost(), i.getPort()));
        }
        return uri.toString();
    }
}
