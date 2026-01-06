/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.GridSubset;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.WarningType;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.mapstruct.factory.Mappers;

/**
 * @since 1.7
 */
class TileLayerMocking {

    private @Getter GridSetBroker gridsets;

    private @Getter CatalogFaker faker;

    final GeoServerTileLayerInfoMapper infoMapper = Mappers.getMapper(GeoServerTileLayerInfoMapper.class);

    private Catalog catalog;

    public TileLayerMocking(Catalog catalog, GeoServer geoserver) {
        this.catalog = catalog;
        gridsets = new GridSetBroker(List.of(new DefaultGridsets(false, false)));
        faker = new CatalogFaker(catalog, geoserver);
    }

    public Supplier<Catalog> catalog() {
        return () -> catalog;
    }

    public @NonNull String layerName(PublishedInfo published) {
        String prefixedName = published.prefixedName();
        int idx = prefixedName.indexOf(':');
        return idx == -1 ? prefixedName : prefixedName.substring(idx + 1);
    }

    public String workspaceName(PublishedInfo published) {
        String prefixedName = published.prefixedName();
        int idx = prefixedName.indexOf(':');
        return idx == -1 ? null : prefixedName.substring(0, idx);
    }

    public GeoServerTileLayer geoServerTileLayer(PublishedInfo published) {
        GeoServerTileLayerInfo tileLayerInfo = TileLayerInfoUtil.loadOrCreate(published, new GWCConfig());
        return new GeoServerTileLayer(published, gridsets, tileLayerInfo);
    }

    public TileLayerInfo pgLayerInfo(PublishedInfo info) {
        List<XMLGridSubset> defaultXmlGridSubsets =
                List.copyOf(TileLayerInfoUtil.create(new GWCConfig()).getGridSubsets());
        XMLGridSubset set1 = defaultXmlGridSubsets.getFirst();
        set1.setExtent(new BoundingBox(-180, -90, 0, 0));
        set1.setMinCachedLevel(3);
        set1.setMaxCachedLevel(12);
        set1.setZoomStart(1);
        set1.setZoomStop(16);

        XMLGridSubset set2 = defaultXmlGridSubsets.get(1);
        set2.setExtent(new BoundingBox(0, 0, 10000, 20000));
        set2.setMinCachedLevel(1);
        set2.setMaxCachedLevel(4);
        set2.setZoomStart(1);
        set2.setZoomStop(12);

        Set<GridSubset> gridSubsets =
                defaultXmlGridSubsets.stream().map(infoMapper::map).collect(Collectors.toSet());

        return new TileLayerInfo()
                .setPublished(info)
                .setBlobStoreId("defaultBlobStore")
                .setCacheWarningSkips(Set.of(WarningType.FailedNearest))
                .setEnabled(true)
                .setMetaTilingX(4)
                .setMetaTilingY(4)
                .setGutter(5)
                .setExpireCacheList(List.of(new TileLayerInfo.ExpirationRule(12, 100)))
                .setMimeFormats(Set.of("image/png", "image/jpeg"))
                .setInMemoryCached(false)
                .setGridSubsets(gridSubsets);
    }

    public WorkspaceInfo workspace() {
        return workspace(null);
    }

    public WorkspaceInfo workspace(String name) {
        WorkspaceInfo ws = null == name ? faker.workspaceInfo() : faker.workspaceInfo(name);
        NamespaceInfo ns = faker.namespace(ws.getName());
        faker.catalog().add(ws);
        faker.catalog().add(ns);
        return ws;
    }

    public LayerInfo layerInfo() {
        WorkspaceInfo ws = workspace();
        return layerInfo(ws);
    }

    public LayerInfo layerInfo(@NonNull WorkspaceInfo ws) {
        String name = faker.name();
        return layerInfo(ws, name);
    }

    public LayerInfo layerInfo(@NonNull WorkspaceInfo ws, @NonNull String name) {
        DataStoreInfo ds = faker.dataStoreInfo(ws);
        faker.catalog().add(ds);

        StyleInfo defaultStyle = faker.styleInfo();
        faker.catalog().add(defaultStyle);

        FeatureTypeInfo featureType = faker.featureTypeInfo(ds, name);
        faker.catalog().add(featureType);

        LayerInfo layer = faker.layerInfo(featureType, defaultStyle);
        faker.catalog().add(layer);
        return layer;
    }

    public LayerGroupInfo layerGroupInfo(String workspace) {
        WorkspaceInfo ws = null == workspace ? null : workspace(workspace);
        return layerGroupInfo(ws);
    }

    public LayerGroupInfo layerGroupInfo(WorkspaceInfo ws) {
        LayerGroupInfo lg = faker.layerGroupInfo(ws);
        LayerInfo li = null == ws ? layerInfo() : layerInfo(ws);
        lg.getLayers().add(li);
        return lg;
    }
}
