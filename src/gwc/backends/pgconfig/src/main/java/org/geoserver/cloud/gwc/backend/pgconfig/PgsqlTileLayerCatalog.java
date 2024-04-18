/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import com.google.common.annotations.VisibleForTesting;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.resolving.ModificationProxyDecorator;
import org.geoserver.cloud.backend.pgconfig.catalog.PgsqlCatalogFacade;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geowebcache.config.BaseConfiguration;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.mapstruct.factory.Mappers;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.sql.DataSource;

/**
 * A {@link TileLayerConfiguration} is like the GeoServer {@link Catalog} for {@link TileLayer};
 * this one runs against the PostgreSQL database set up for {@link PgsqlCatalogFacade}, and returns
 * {@link GeoServerTileLayer}.
 *
 * @see TileLayerInfoRepository
 * @see PgsqlTileLayerInfoRepository
 * @since 1.7
 */
public class PgsqlTileLayerCatalog implements TileLayerConfiguration {

    final @NonNull TileLayerInfoRepository repository;
    final @NonNull GridSetBroker gridsetBroker;
    final @NonNull UnaryOperator<PublishedInfo> publishedResolver;

    final GeoServerTileLayerInfoMapper infoMapper =
            Mappers.getMapper(GeoServerTileLayerInfoMapper.class);

    public PgsqlTileLayerCatalog(
            @NonNull TileLayerInfoRepository repository,
            @NonNull GridSetBroker gridsetBroker,
            @NonNull Supplier<Catalog> catalog) {

        this.repository = repository;
        this.gridsetBroker = gridsetBroker;

        UnaryOperator<PublishedInfo> resolve = PgsqlCatalogFacade.resolvingFunction(catalog);
        UnaryOperator<PublishedInfo> proxify = ModificationProxyDecorator::wrap;
        this.publishedResolver = resolve.andThen(proxify)::apply;
    }

    @VisibleForTesting
    PgsqlTileLayerCatalog(
            DataSource dataSource, GridSetBroker gridsets, Supplier<Catalog> catalog) {

        this(new PgsqlTileLayerInfoRepository(new JdbcTemplate(dataSource)), gridsets, catalog);
    }

    @Override
    public String getIdentifier() {
        return "PostgreSQL Tile Layer Catalog";
    }

    @Override
    public String getLocation() {
        return "PostgreSQL catalog and config database";
    }

    @Override
    public int getPriority(Class<? extends BaseConfiguration> clazz) {
        return BASE_PRIORITY - 1;
    }

    @Override
    public void afterPropertiesSet() {
        // nothing to do
    }

    @Override
    public void deinitialize() {
        // nothing to do
    }

    @Override
    public void setGridSetBroker(GridSetBroker broker) {
        throw new UnsupportedOperationException("use constructor injection instead");
    }

    @Override
    public int getLayerCount() {
        return repository.count();
    }

    @Override
    public Set<String> getLayerNames() {
        return repository.findAllNames();
    }

    @Override
    public List<GeoServerTileLayer> getLayers() {
        try (Stream<TileLayerInfo> pginfos = repository.findAll()) {
            return pginfos.map(this::toLayer).toList();
        }
    }

    public Stream<GeoServerTileLayer> streamLayers() {
        return repository.findAll().map(this::toLayer);
    }

    @Override
    public Optional<TileLayer> getLayer(@NonNull String layerName) {

        final String localName = localName(layerName);
        String workspaceName = workspaceName(layerName);

        final WorkspaceInfo localWorkspace = LocalWorkspace.get();
        if (null != localWorkspace) {
            if (null != workspaceName && !workspaceName.equals(localWorkspace.getName())) {
                return Optional.empty();
            }
            workspaceName = localWorkspace.getName();
        }

        Optional<TileLayerInfo> info = repository.find(workspaceName, localName);
        return info.map(this::toLayer);
    }

    @Override
    public void addLayer(@NonNull TileLayer tl) throws IllegalArgumentException {
        GeoServerTileLayer tileLayer =
                canSave(tl, "Can't add TileLayer of type: %s ", tl.getClass());
        TileLayerInfo pgInfo = toInfo(tileLayer);
        repository.add(pgInfo);
    }

    @Override
    public void removeLayer(String layerName) {
        final String localName = localName(layerName);
        String workspaceName = workspaceName(layerName);

        final WorkspaceInfo localWorkspace = LocalWorkspace.get();
        if (null != localWorkspace) {
            if (null != workspaceName && !workspaceName.equals(localWorkspace.getName())) {
                return;
            }
            workspaceName = localWorkspace.getName();
        }
        repository.delete(workspaceName, localName);
    }

    @Override
    public void modifyLayer(@NonNull TileLayer tl) throws NoSuchElementException {
        GeoServerTileLayer tileLayer =
                canSave(tl, "Can't save TileLayer of type: %s ", tl.getClass());

        TileLayerInfo pgInfo = toInfo(tileLayer);
        boolean updated = repository.save(pgInfo);
        if (!updated) {
            throw new NoSuchElementException("TileLayer %s does not exist".formatted(tl.getName()));
        }
    }

    @Override
    public void renameLayer(String oldName, String newName) {
        // no-op, no need to rename, name is taken directly from the PublishedInfo
    }

    @Override
    public boolean containsLayer(String layerName) {
        final String localName = localName(layerName);
        String workspaceName = workspaceName(layerName);

        final WorkspaceInfo localWorkspace = LocalWorkspace.get();
        if (null != localWorkspace) {
            if (null != workspaceName && !workspaceName.equals(localWorkspace.getName())) {
                return false;
            }
            workspaceName = localWorkspace.getName();
        }
        return repository.exists(workspaceName, localName);
    }

    /**
     * @return {@code true} only if {@code tl instanceof} {@link GeoServerTileLayer} .
     * @see TileLayerConfiguration#canSave(TileLayer)
     */
    @Override
    public boolean canSave(TileLayer tl) {
        return supportedAndNonTransient(tl).isPresent();
    }

    private GeoServerTileLayer canSave(TileLayer tl, String message, Object... args) {
        return supportedAndNonTransient(tl)
                .orElseThrow(() -> new IllegalArgumentException(message.formatted(args)));
    }

    private Optional<GeoServerTileLayer> supportedAndNonTransient(TileLayer tl) {
        if ((tl instanceof GeoServerTileLayer gsl) && !tl.isTransientLayer()) {
            return Optional.of(gsl);
        }
        return Optional.empty();
    }

    private String workspaceName(@NonNull String layerName) {
        int idx = layerName.indexOf(':');
        return idx == -1 ? null : layerName.substring(0, idx);
    }

    @NonNull
    private String localName(@NonNull String layerName) {
        int idx = layerName.indexOf(':');
        return idx == -1 ? layerName : layerName.substring(1 + idx);
    }

    GeoServerTileLayer toLayer(TileLayerInfo pgTileLayerInfo) {
        PublishedInfo published = pgTileLayerInfo.getPublished();
        published = publishedResolver.apply(published);

        GeoServerTileLayerInfo info = infoMapper.map(pgTileLayerInfo);
        return new GeoServerTileLayer(published, this.gridsetBroker, info);
    }

    TileLayerInfo toInfo(GeoServerTileLayer tileLayer) {
        return infoMapper.map(tileLayer);
    }
}
