/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.test;

import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.catalog.api.v1.LayerController;
import org.geoserver.cloud.catalog.api.v1.LayerGroupController;
import org.geoserver.cloud.catalog.api.v1.NamespaceController;
import org.geoserver.cloud.catalog.api.v1.ResourceController;
import org.geoserver.cloud.catalog.api.v1.StoreController;
import org.geoserver.cloud.catalog.api.v1.StyleController;
import org.geoserver.cloud.catalog.api.v1.WorkspaceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Configures the {@link WebTestClient} to be able of encoding and decoding {@link CatalogInfo}
 * obejcts using {@link CatalogInfoXmlEncoder} and {@link CatalogInfoXmlDecoder}
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

    public @Override WebTestClient get() {
        return client;
    }

    public <C extends CatalogInfo> CatalogTestClient<C> clientFor(
            @NonNull Class<C> infoType, @NonNull String baseUri) {
        return new CatalogTestClient<C>(client, infoType, baseUri);
    }

    public CatalogTestClient<WorkspaceInfo> workspaces() {
        return clientFor(WorkspaceInfo.class, WorkspaceController.BASE_URI);
    }

    public CatalogTestClient<NamespaceInfo> namespaces() {
        return clientFor(NamespaceInfo.class, NamespaceController.BASE_URI);
    }

    public CatalogTestClient<StoreInfo> stores() {
        return clientFor(StoreInfo.class, StoreController.BASE_URI);
    }

    public CatalogTestClient<ResourceInfo> resources() {
        return clientFor(ResourceInfo.class, ResourceController.BASE_URI);
    }

    public CatalogTestClient<LayerInfo> layers() {
        return clientFor(LayerInfo.class, LayerController.BASE_URI);
    }

    public CatalogTestClient<LayerGroupInfo> layerGroups() {
        return clientFor(LayerGroupInfo.class, LayerGroupController.BASE_URI);
    }

    public CatalogTestClient<StyleInfo> styles() {
        return clientFor(StyleInfo.class, StyleController.BASE_URI);
    }
}
