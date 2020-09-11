/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.cloud.catalog.client.feign.LayerClient;

public class CloudLayerRepository extends CatalogServiceClientRepository<LayerInfo, LayerClient>
        implements LayerRepository {

    private final @Getter Class<LayerInfo> infoType = LayerInfo.class;

    protected CloudLayerRepository(@NonNull LayerClient client) {
        super(client);
    }

    public @Override List<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style) {
        return client().findAllByDefaultStyleOrStyles(style.getId());
    }

    public @Override List<LayerInfo> findAllByResource(ResourceInfo resource) {
        return client().findAllByResourceId(resource.getId());
    }

    public @Override LayerInfo findOneByName(
            @NonNull String possiblyPrefixedNameMostProbablyDeadCodeFromCatalogImpl) {
        throw new UnsupportedOperationException(
                "looks like it wasn't dead code from CatalogImpl.getLayerByName(String)");
    }
}
