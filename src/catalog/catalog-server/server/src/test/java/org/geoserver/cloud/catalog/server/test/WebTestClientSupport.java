/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.test;

import lombok.NonNull;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.catalog.server.api.v1.ReactiveCatalogController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.function.Supplier;

import javax.annotation.PostConstruct;

/**
 * Configures the {@link WebTestClient} to be able of encoding and decoding {@link CatalogInfo}
 * objects
 */
public class WebTestClientSupport implements Supplier<WebTestClient> {

    protected @Autowired WebTestClient client;

    protected @PostConstruct void setup() {
        // client =
        // client.mutate()
        // .codecs(
        // configurer -> {
        // configurer.customCodecs().registerWithDefaultConfig(encoder);
        // configurer.customCodecs().registerWithDefaultConfig(decoder);
        // })
        // .build();
    }

    @Override public  WebTestClient get() {
        return client;
    }

    public <C extends CatalogInfo> CatalogTestClient<C> clientFor(@NonNull Class<C> infoType) {
        return new CatalogTestClient<C>(client, infoType, ReactiveCatalogController.BASE_URI);
    }

    public CatalogTestClient<WorkspaceInfo> workspaces() {
        return clientFor(WorkspaceInfo.class);
    }

    public CatalogTestClient<NamespaceInfo> namespaces() {
        return clientFor(NamespaceInfo.class);
    }

    public CatalogTestClient<StoreInfo> stores() {
        return clientFor(StoreInfo.class);
    }

    public CatalogTestClient<ResourceInfo> resources() {
        return clientFor(ResourceInfo.class);
    }

    public CatalogTestClient<LayerInfo> layers() {
        return clientFor(LayerInfo.class);
    }

    public CatalogTestClient<LayerGroupInfo> layerGroups() {
        return clientFor(LayerGroupInfo.class);
    }

    public CatalogTestClient<StyleInfo> styles() {
        return clientFor(StyleInfo.class);
    }
}
