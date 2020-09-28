/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ReactiveFeignClient( //
    name = "config-service-client", //
    url = "${geoserver.backend.catalog-service.uri:catalog-service}", //
    path = "/api/v1/config"
)
public interface ReactiveConfigClient {

    /** The global geoserver configuration. */
    @GetMapping("/global")
    Mono<GeoServerInfo> getGlobal();

    /** Sets the global configuration. */
    @PutMapping("/global")
    Mono<Void> setGlobal(GeoServerInfo global);

    /**
     * The settings configuration for the specified workspace, or <code>null</code> if non exists.
     */
    @GetMapping("/workspaces/{workspaceId}/settings")
    Mono<SettingsInfo> getSettingsByWorkspace(@PathVariable("workspaceId") String workspaceId);

    /** Adds a settings configuration for the specified workspace. */
    @PostMapping("/workspaces/{workspaceId}/settings")
    Mono<Void> createSettings(
            @PathVariable("workspaceId") String workspaceId, SettingsInfo settings);

    /** Saves the settings configuration for the specified workspace. */
    @PutMapping("/workspaces/{workspaceId}/settings")
    Mono<Void> saveSettings(@PathVariable("workspaceId") String workspaceId, SettingsInfo settings);

    /** Removes the settings configuration for the specified workspace. */
    @DeleteMapping("/workspaces/{workspaceId}/settings")
    Mono<Void> deleteSettings(@PathVariable("workspaceId") String workspaceId);

    /** The logging configuration. */
    @GetMapping("/logging")
    Mono<LoggingInfo> getLogging();

    /** Sets logging configuration. */
    @PutMapping("/logging")
    Mono<Void> setLogging(LoggingInfo logging);

    /** Adds a service to the configuration. */
    @PostMapping("/services")
    Mono<Void> createService(ServiceInfo service);

    @PostMapping("/workspaces/{workspaceId}/services")
    Mono<Void> createService(@PathVariable("workspaceId") String workspaceId, ServiceInfo service);

    /** Removes a service from the configuration. */
    @DeleteMapping("/services/{serviceId}")
    Mono<Void> deleteService(@PathVariable("serviceId") String serviceId);

    /** Looks up a global service by id. */
    @GetMapping("/services/{serviceId}")
    <T extends ServiceInfo> Mono<T> getServiceById(@PathVariable("serviceId") String id);

    /** Saves a service that has been modified. */
    @PutMapping("/services")
    Mono<Void> updateService(ServiceInfo service);

    @PutMapping("/workspaces/{workspaceId}/services")
    Mono<Void> updateService(@PathVariable("workspaceId") String workspaceId, ServiceInfo service);

    /** GeoServer services. */
    @GetMapping("/services")
    Flux<? extends ServiceInfo> getGlobalServices();

    /** GeoServer services specific to the specified workspace. */
    @GetMapping("/workspaces/{workspaceId}/services")
    Flux<? extends ServiceInfo> getServicesByWorkspace(
            @PathVariable("workspaceId") String workspaceId);

    /** GeoServer global service filtered by class. */
    @GetMapping("/services/type/{type}")
    <T extends ServiceInfo> Mono<T> getGlobalService(@PathVariable("type") Class<T> clazz);

    /** GeoServer service specific to the specified workspace and filtered by class. */
    @GetMapping("/workspaces/{workspaceId}/services/type/{type}")
    <T extends ServiceInfo> Mono<T> getServiceByWorkspaceAndType(
            @PathVariable("workspaceId") String workspaceId, @PathVariable("type") Class<T> clazz);

    /** Looks up a service by name. */
    @GetMapping("/services/name/{name}")
    <T extends ServiceInfo> Mono<T> getServiceByName(@PathVariable("name") String name);

    /** Looks up a service by name, specific to the specified workspace. */
    @GetMapping("/workspaces/{workspaceId}/services/name/{name}")
    <T extends ServiceInfo> Mono<T> getServiceByWorkspaceAndName(
            @PathVariable("workspaceId") String workspaceId, @PathVariable("name") String name);
}
