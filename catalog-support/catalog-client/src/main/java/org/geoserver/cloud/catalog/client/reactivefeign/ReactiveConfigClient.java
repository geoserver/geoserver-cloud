/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ReactiveFeignClient( //
    name = "catalog-service", //
    url = "${geoserver.backend.catalog-service.uri:catalog-service}", //
    qualifier = "catalogClient", //
    path = "/api/v1/config"
)
public interface ReactiveConfigClient {

    /** The global geoserver configuration. */
    Mono<GeoServerInfo> getGlobal();

    /** Sets the global configuration. */
    Mono<Void> setGlobal(GeoServerInfo global);

    /** Saves the global geoserver configuration after modification. */
    Mono<Void> save(GeoServerInfo geoServer);

    /**
     * The settings configuration for the specified workspace, or <code>null</code> if non exists.
     */
    Mono<SettingsInfo> getSettingsByWorkspace(String workspaceId);

    /** Adds a settings configuration for the specified workspace. */
    Mono<Void> add(SettingsInfo settings);

    /** Saves the settings configuration for the specified workspace. */
    Mono<Void> save(SettingsInfo settings);

    /** Removes the settings configuration for the specified workspace. */
    Mono<Void> removeSettings(String settingsId);

    /** The logging configuration. */
    Mono<LoggingInfo> getLogging();

    /** Sets logging configuration. */
    Mono<Void> setLogging(LoggingInfo logging);

    /** Saves the logging configuration. */
    Mono<Void> save(LoggingInfo logging);

    /** Adds a service to the configuration. */
    Mono<Void> add(ServiceInfo service);

    /** Removes a service from the configuration. */
    Mono<Void> removeService(String serviceId);

    /** Saves a service that has been modified. */
    Mono<Void> save(ServiceInfo service);

    /** GeoServer services. */
    Flux<? extends ServiceInfo> getServices();

    /** GeoServer services specific to the specified workspace. */
    Flux<? extends ServiceInfo> getServicesByWorkspace(String workspaceId);

    /**
     * GeoServer global service filtered by class.
     *
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> Mono<T> getGlobalService(Class<T> clazz);

    /**
     * GeoServer service specific to the specified workspace and filtered by class.
     *
     * @param workspaceId The workspace the service is specific to.
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> Mono<T> getServiceByWorkspace(String workspaceId, Class<T> clazz);

    /**
     * Looks up a service by id.
     *
     * @param id The id of the service.
     * @param clazz The type of the service.
     * @return The service with the specified id, or <code>null</code> if no such service coud be
     *     found.
     */
    <T extends ServiceInfo> Mono<T> getServiceById(String id, Class<T> clazz);

    /**
     * Looks up a service by name.
     *
     * @param name The name of the service.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> Mono<T> getServiceByName(String name, Class<T> clazz);

    /**
     * Looks up a service by name, specific to the specified workspace.
     *
     * @param name The name of the service.
     * @param workspaceId The workspace the service is specific to.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> Mono<T> getServiceByNameAndWorkspace(
            String name, String workspaceId, Class<T> clazz);
}
