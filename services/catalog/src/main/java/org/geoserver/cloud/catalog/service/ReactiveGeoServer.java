/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.service;

import java.util.concurrent.Callable;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wps.WPSInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/** */
@Service
public class ReactiveGeoServer {
    private Scheduler catalogScheduler;

    private GeoServer blockingConfig;

    public ReactiveGeoServer( //
            GeoServer blockingConfig, //
            @Qualifier("catalogScheduler") Scheduler catalogScheduler) {

        this.blockingConfig = blockingConfig;
        this.catalogScheduler = catalogScheduler;
    }

    private <T> Mono<T> async(Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(catalogScheduler);
    }

    private Mono<Void> async(Runnable runnable) {
        return Mono.fromRunnable(runnable).subscribeOn(catalogScheduler).then();
    }

    public Mono<GeoServerInfo> getGlobal() {
        return async(() -> blockingConfig.getGlobal());
    }

    public Mono<LoggingInfo> getLogging() {
        return async(blockingConfig::getLogging);
    }

    public Mono<GeoServerInfo> setGlobal(GeoServerInfo global) {
        return async(
                () -> {
                    GeoServerInfo local = blockingConfig.getGlobal();
                    if (local == null) {
                        blockingConfig.setGlobal(global);
                    } else {
                        OwsUtils.copy(global, local, GeoServerInfo.class);
                        blockingConfig.save(local);
                    }
                    return blockingConfig.getGlobal();
                });
    }

    public Mono<LoggingInfo> setLogging(LoggingInfo logging) {
        return async(
                () -> {
                    LoggingInfo local = blockingConfig.getLogging();
                    if (local == null) {
                        blockingConfig.setLogging(logging);
                    } else {
                        OwsUtils.copy(logging, local, LoggingInfo.class);
                        blockingConfig.save(local);
                    }
                    return blockingConfig.getLogging();
                });
    }

    public Mono<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace) {
        return async(() -> blockingConfig.getSettings(workspace));
    }

    public Mono<Void> add(SettingsInfo settings) {
        return async(() -> blockingConfig.add(settings));
    }

    public Mono<Void> add(ServiceInfo service) {
        return async(() -> blockingConfig.add(service));
    }

    public Mono<SettingsInfo> update(SettingsInfo settings, Patch patch) {
        return async(
                () -> {
                    patch.applyTo(settings, SettingsInfo.class);
                    blockingConfig.save(settings);
                    return settings;
                });
    }

    public Mono<Void> remove(SettingsInfo settings) {
        return async(
                () -> {
                    blockingConfig.remove(settings);
                });
    }

    public Mono<Void> remove(ServiceInfo service) {
        return async(
                () -> {
                    blockingConfig.remove(service);
                });
    }

    public Mono<Void> save(ServiceInfo service) {
        return async(
                () -> {
                    ServiceInfo proxied =
                            blockingConfig.getService(service.getId(), ServiceInfo.class);
                    @SuppressWarnings("unchecked")
                    Class<ServiceInfo> type = (Class<ServiceInfo>) resolveClass(service.getClass());
                    OwsUtils.copy(service, proxied, type);
                    blockingConfig.save(proxied);
                });
    }

    public Mono<ServiceInfo> update(ServiceInfo service, Patch patch) {
        return async(
                () -> {
                    Class<? extends ServiceInfo> serviceType = resolveClass(service.getClass());
                    patch.applyTo(service, serviceType);
                    blockingConfig.save(service);
                    return service;
                });
    }

    public Mono<SettingsInfo> getSettings(WorkspaceInfo workspace) {
        return async(() -> blockingConfig.getSettings(workspace));
    }

    private Class<? extends ServiceInfo> resolveClass(Class<? extends ServiceInfo> serviceType) {
        if (WMSInfo.class.isAssignableFrom(serviceType)) return WMSInfo.class;
        if (WFSInfo.class.isAssignableFrom(serviceType)) return WFSInfo.class;
        if (WCSInfo.class.isAssignableFrom(serviceType)) return WCSInfo.class;
        if (WPSInfo.class.isAssignableFrom(serviceType)) return WPSInfo.class;

        return ServiceInfo.class;
    }

    public Mono<ServiceInfo> getServiceById(String id) {
        return async(() -> blockingConfig.getService(id, ServiceInfo.class));
    }

    public <S extends ServiceInfo> Mono<S> getGlobalService(Class<S> type) {
        return async(() -> blockingConfig.getService(type));
    }

    public Mono<ServiceInfo> getGlobalServiceByName(String name) {
        return async(() -> blockingConfig.getServiceByName(name, ServiceInfo.class));
    }

    public Flux<? extends ServiceInfo> getGlobalServices() {
        return Flux.fromStream(() -> blockingConfig.getServices().stream())
                .subscribeOn(catalogScheduler);
    }

    public Flux<? extends ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace) {
        return Flux.fromStream(() -> blockingConfig.getServices(workspace).stream())
                .subscribeOn(catalogScheduler);
    }

    public Mono<ServiceInfo> getServiceByWorkspaceAndName(
            WorkspaceInfo workspace, String serviceName) {
        return async(
                () -> blockingConfig.getServiceByName(workspace, serviceName, ServiceInfo.class));
    }

    public <S extends ServiceInfo> Mono<S> getServiceByWorkspaceAndType(
            WorkspaceInfo workspace, Class<S> type) {
        return async(() -> blockingConfig.getService(workspace, type));
    }
}
