/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.service;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.ProxyUtils;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** */
@Service
public class ProxyResolver {

    private ProxyUtils blockingResolver;

    public ProxyResolver(Catalog catalog, GeoServer config) {
        this.blockingResolver = new ProxyUtils(catalog, config);
    }

    public <C extends Info> Mono<C> resolve(C info) {
        return Mono.just(info).subscribeOn(Schedulers.parallel()).map(blockingResolver::resolve);
    }

    public Mono<Patch> resolve(Patch patch) {
        return Mono.just(patch).subscribeOn(Schedulers.parallel()).map(blockingResolver::resolve);
    }
}
