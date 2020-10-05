/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON_VALUE;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.catalog.service.ReactiveCatalog;
import org.geoserver.cloud.catalog.service.ReactiveGeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(path = ReactiveConfigController.BASE_URI)
public class ReactiveConfigController {

    public static final String BASE_URI = "/api/v1/config";

    private @Autowired ReactiveCatalog catalog;
    private @Autowired ReactiveGeoServer config;

    /** The global geoserver configuration. */
    @GetMapping("/global")
    public Mono<GeoServerInfo> getGlobal() {
        return config.getGlobal().switchIfEmpty(noContent());
    }

    /** Sets the global configuration. */
    @PutMapping("/global")
    public Mono<GeoServerInfo> setGlobal(@RequestBody GeoServerInfo global) {
        return config.setGlobal(global);
    }

    /**
     * The settings configuration for the specified workspace, or <code>null</code> if non exists.
     */
    @GetMapping("/workspaces/{workspaceId}/settings")
    public Mono<SettingsInfo> getSettingsByWorkspace(
            @PathVariable("workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(
                        badRequest("getSettingsByWorkspace: workspace %s not found", workspaceId))
                .flatMap(config::getSettingsByWorkspace)
                .switchIfEmpty(noContent());
    }

    /** Adds a settings configuration for the specified workspace. */
    @PostMapping("/workspaces/{workspaceId}/settings")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> createSettings(
            @PathVariable("workspaceId") String workspaceId, @RequestBody SettingsInfo settings) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(badRequest("createSettings: workspace %s not found", workspaceId))
                .flatMap(
                        ws -> {
                            settings.setWorkspace(ws);
                            return config.add(settings);
                        });
    }

    /** Saves the settings configuration for the specified workspace. */
    @PatchMapping("/workspaces/{workspaceId}/settings")
    Mono<SettingsInfo> updateSettings(
            @PathVariable("workspaceId") String workspaceId, @RequestBody Patch patch) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(badRequest("updateSettings: workspace %s not found", workspaceId))
                .flatMap(config::getSettingsByWorkspace)
                .switchIfEmpty(badRequest("Workspace has no settings"))
                .flatMap(settings -> config.update(settings, patch));
    }

    /** Removes the settings configuration for the specified workspace. */
    @DeleteMapping("/workspaces/{workspaceId}/settings")
    public Mono<Void> deleteSettings(@PathVariable("workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(badRequest("deleteSettings: workspace %s not found", workspaceId))
                .flatMap(config::getSettings)
                .switchIfEmpty(noContent())
                .flatMap(config::remove);
    }

    /** The logging configuration. */
    @GetMapping("/logging")
    public Mono<LoggingInfo> getLogging() {
        return config.getLogging().switchIfEmpty(noContent());
    }

    /** Sets logging configuration. */
    @PutMapping("/logging")
    public Mono<LoggingInfo> setLogging(@RequestBody LoggingInfo logging) {
        return config.setLogging(logging);
    }

    /** Adds a service to the configuration. */
    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> createService(@RequestBody ServiceInfo service) {
        if (service.getWorkspace() != null)
            throw new IllegalArgumentException(
                    "Service has workspace, use /workspaces/{workspaceId}/services instead");

        return config.add(service);
    }

    @PostMapping("/workspaces/{workspaceId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> createService(
            @PathVariable("workspaceId") String workspaceId, @RequestBody ServiceInfo service) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(badRequest("createService: workspace %s not found", workspaceId))
                .flatMap(
                        ws -> {
                            service.setWorkspace(ws);
                            return config.add(service);
                        });
    }

    /** Removes a service from the configuration. */
    @DeleteMapping("/services/{serviceId}")
    public Mono<Void> deleteService(@PathVariable("serviceId") String serviceId) {
        return config.getServiceById(serviceId).switchIfEmpty(noContent()).flatMap(config::remove);
    }

    /** Looks up a global service by id. */
    @GetMapping("/services/{serviceId}")
    public Mono<ServiceInfo> getServiceById(@PathVariable("serviceId") String id) {
        return config.getServiceById(id);
    }

    /** Saves a service that has been modified. */
    @PatchMapping("/services/{serviceId}")
    Mono<ServiceInfo> updateService(
            @PathVariable("serviceId") String serviceId, @RequestBody Patch patch) {

        return config.getServiceById(serviceId)
                .switchIfEmpty(badRequest("Service %s not found", serviceId))
                .flatMap(s -> config.update(s, patch));
    }

    /** GeoServer services specific to the specified workspace. */
    @GetMapping(
        path = "/workspaces/{workspaceId}/services",
        produces = APPLICATION_STREAM_JSON_VALUE
    )
    public Flux<ServiceInfo> getServicesByWorkspace(
            @PathVariable("workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(
                        badRequest("getServicesByWorkspace: Workspace %s not found", workspaceId))
                .flatMapMany(config::getServicesByWorkspace);
    }

    /** Global (no-workspace) services. */
    @GetMapping(path = "/services", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<? extends ServiceInfo> getGlobalServices() {
        return config.getGlobalServices();
    }

    /** GeoServer global service filtered by class. */
    @GetMapping("/services/type/{type}")
    public Mono<? extends ServiceInfo> getGlobalServiceByType(@PathVariable("type") String clazz) {

        Class<? extends ServiceInfo> type = asSericeType(clazz);
        return config.getGlobalService(type).switchIfEmpty(noContent());
    }

    @SuppressWarnings("unchecked")
    private <S extends ServiceInfo> Class<S> asSericeType(String clazz) {
        try {
            Class<?> type = Class.forName(clazz);
            if (!ServiceInfo.class.isAssignableFrom(type))
                throw new IllegalArgumentException("Not a subclass of ServiceInfo: " + clazz);
            if (!type.isInterface())
                throw new IllegalArgumentException("Not an interface: " + clazz);
            return (Class<S>) type;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found: " + clazz);
        }
    }

    /** GeoServer service specific to the specified workspace and filtered by class. */
    @GetMapping("/workspaces/{workspaceId}/services/type/{type}")
    public <T extends ServiceInfo> Mono<T> getServiceByWorkspaceAndType(
            @PathVariable("workspaceId") String workspaceId, @PathVariable("type") String clazz) {

        final Class<T> type = this.asSericeType(clazz);

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(
                        badRequest(
                                "getServiceByWorkspaceAndType: Workspace %s not found",
                                workspaceId))
                .flatMap(w -> config.getServiceByWorkspaceAndType(w, type))
                .switchIfEmpty(noContent());
    }

    /** Looks up a service by name. */
    @GetMapping("/services/name/{name}")
    public Mono<ServiceInfo> getGlobalServiceByName(@PathVariable("name") String name) {

        return config.getGlobalServiceByName(name).switchIfEmpty(noContent());
    }

    /** Looks up a service by name, specific to the specified workspace. */
    @GetMapping("/workspaces/{workspaceId}/services/name/{name}")
    public Mono<ServiceInfo> getServiceByWorkspaceAndName(
            @PathVariable("workspaceId") String workspaceId, @PathVariable("name") String name) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(
                        badRequest(
                                "getServiceByWorkspaceAndName: Workspace %s not found",
                                workspaceId))
                .flatMap(ws -> config.getServiceByWorkspaceAndName(ws, name))
                .switchIfEmpty(noContent());
    }

    protected <T> Mono<T> noContent() {
        return Mono.defer(
                () -> Mono.error(() -> new ResponseStatusException(HttpStatus.NO_CONTENT)));
    }

    protected <T> Mono<T> badRequest(String messageFormat, Object... messageArgs) {
        return Mono.error(
                () -> {
                    String msg = String.format(messageFormat, messageArgs);
                    log.warn(msg);
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
                });
    }
}
