/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.gwc.repository.CloudCatalogConfiguration;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.ows.LocalWorkspace;
import org.geowebcache.config.BaseConfiguration;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.jspecify.annotations.Nullable;
import org.mapstruct.factory.Mappers;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A {@link TileLayerConfiguration} is like the GeoServer {@link Catalog} for {@link TileLayer}; this one runs against
 * the PostgreSQL database set up for {@link PgconfigCatalogFacade}, and returns {@link GeoServerTileLayer}.
 *
 * <p>A tile layer is a layer or group of the catalog with its GWC settings. A lookup by name takes the layer or group
 * from the facade of the catalog, and the settings from the {@link TileLayerInfoRepository}, by the id of the object.
 * The facade applies the workspace isolation of the request, answers from the caches of the catalog, and returns the
 * object in a {@link ModificationProxy}. The {@link BuiltTileLayerCache} keeps the tile layers found by name until the
 * catalog changes, and the callers share them, as in upstream GeoWebCache.
 *
 * <p>A listing reads the ids of the tile layers, then reads the tile layers in batches of {@value #LISTING_BATCH_SIZE}
 * ids: the layers and groups from the facade of the catalog, and their settings from the repository. It builds new tile
 * layers. In the virtual service of a workspace, a listing keeps the layers and groups of the workspace, as the lookups
 * by name do. The cache of the request holds the layers and groups while it has room: a request listing the tile layers
 * several times then decodes them once. While a batch is read, the {@link PutBudget} of the listing is bound to the
 * thread. Once it is spent, the reads stop putting rows in the entity cache of the catalog. A listing larger than the
 * cache then leaves the objects used most in it.
 *
 * @see TileLayerInfoRepository
 * @see PgconfigTileLayerInfoRepository
 * @since 1.7
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.backend.pgconfig")
public class PgconfigTileLayerCatalog implements TileLayerConfiguration {

    /** The number of ids read by each batch of a listing. A batch reads its layers and groups, then their settings. */
    static final int LISTING_BATCH_SIZE = 1_000;

    final @NonNull TileLayerInfoRepository repository;
    final @NonNull GridSetBroker gridsetBroker;
    private final @NonNull Supplier<ExtendedCatalogFacade> catalogFacade;
    final @NonNull GWCConfigPersister defaultsProvider;

    private final @NonNull BuiltTileLayerCache builtTileLayers;

    private final @NonNull Supplier<PutBudget> listingBudget;

    final GeoServerTileLayerInfoMapper infoMapper = Mappers.getMapper(GeoServerTileLayerInfoMapper.class);

    /**
     * @param catalogFacade the facade of the catalog, such as {@code rawCatalog::getFacade}, for the layers and groups
     *     of the tile layers
     * @param builtTileLayers keeps the tile layers found by name
     * @param listingBudget gives each listing a new budget for its puts in the entity cache of the catalog
     */
    public PgconfigTileLayerCatalog(
            @NonNull TileLayerInfoRepository repository,
            @NonNull GridSetBroker gridsetBroker,
            @NonNull Supplier<ExtendedCatalogFacade> catalogFacade,
            @NonNull GWCConfigPersister defaultsProvider,
            @NonNull BuiltTileLayerCache builtTileLayers,
            @NonNull Supplier<PutBudget> listingBudget) {

        this.repository = repository;
        this.gridsetBroker = gridsetBroker;
        this.catalogFacade = catalogFacade;
        this.defaultsProvider = defaultsProvider;
        this.builtTileLayers = builtTileLayers;
        this.listingBudget = listingBudget;
    }

    @VisibleForTesting
    PgconfigTileLayerCatalog(
            DataSource dataSource,
            GridSetBroker gridsets,
            Supplier<ExtendedCatalogFacade> catalogFacade,
            @NonNull GWCConfigPersister defaultsProvider) {

        this(
                new PgconfigTileLayerInfoRepository(new JdbcTemplate(dataSource), null),
                gridsets,
                catalogFacade,
                defaultsProvider,
                new BuiltTileLayerCache(null, dataSource, 0),
                PutBudget::unlimited);
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
        try (Stream<GeoServerTileLayer> tileLayers = streamLayers()) {
            return tileLayers.toList();
        }
    }

    /**
     * Returns the tile layers, in the order of their ids. The stream reads the next batch once it has used the tile
     * layers of the previous one. A tile layer is left out if its layer or group, or its settings, are removed after
     * the read of the ids.
     */
    public Stream<GeoServerTileLayer> streamLayers() {
        List<String> ids = repository.findAllIds();
        PutBudget budget = listingBudget.get();
        List<List<String>> batches = Lists.partition(ids, LISTING_BATCH_SIZE);
        return batches.stream().flatMap(batch -> tileLayersOf(batch, budget).stream());
    }

    /**
     * Builds the tile layers of a batch of ids, in the order of the ids, with the budget of the listing bound to the
     * thread while it reads. The settings are read only for the layers and groups found. An id gives no tile layer if
     * the listing does not find its layer or group, or if the layer or group has no settings, as when the catalog
     * changed after the read of the ids.
     */
    private List<GeoServerTileLayer> tileLayersOf(List<String> batch, PutBudget budget) {
        Map<String, PublishedInfo> published;
        Map<String, TileLayerInfo> settings;
        try (PutBudget.Binding binding = budget.bind()) {
            published = findAllPublished(batch);
            settings = repository.findAllSettings(foundIds(batch, published));
        }
        return toLayers(batch, published, settings);
    }

    /**
     * Finds the layers and groups of the ids through the facade of the catalog, by id. An id is absent if the catalog
     * has no layer or group with that id, if the workspace isolation of the request hides it, or if the virtual service
     * of the request does not show it.
     */
    private Map<String, PublishedInfo> findAllPublished(List<String> ids) {
        Predicate<PublishedInfo> shown = shownByVirtualService();
        List<PublishedInfo> found;
        try (Stream<PublishedInfo> stream = catalogFacade.get().findAllById(ids, PublishedInfo.class)) {
            found = stream.filter(shown).toList();
        }
        return byId(found);
    }

    /**
     * Returns a predicate telling whether the virtual service of the request shows a layer or group, as for the lookups
     * by name. The virtual service of a workspace shows the layers of the stores of the workspace and the groups of the
     * workspace, and no group without workspace. Outside a virtual service, each layer and group shows.
     */
    private static Predicate<PublishedInfo> shownByVirtualService() {
        WorkspaceInfo localWorkspace = LocalWorkspace.get();
        if (null == localWorkspace) {
            return layerOrGroup -> true;
        }
        String serviceWorkspace = localWorkspace.getName();
        return layerOrGroup -> belongsTo(layerOrGroup, serviceWorkspace);
    }

    /** Whether a layer belongs to a store of the workspace, or a group to the workspace. */
    private static boolean belongsTo(PublishedInfo layerOrGroup, String workspaceName) {
        WorkspaceInfo workspace =
                switch (layerOrGroup) {
                    case LayerInfo layer -> layer.getResource().getStore().getWorkspace();
                    case LayerGroupInfo group -> group.getWorkspace();
                    default -> null;
                };
        return null != workspace && workspaceName.equals(workspace.getName());
    }

    private static Map<String, PublishedInfo> byId(List<PublishedInfo> layersAndGroups) {
        Map<String, PublishedInfo> byId = new HashMap<>();
        for (PublishedInfo layerOrGroup : layersAndGroups) {
            byId.put(layerOrGroup.getId(), layerOrGroup);
        }
        return byId;
    }

    /** The ids of the batch with a layer or group found, in the order of the batch. */
    private static List<String> foundIds(List<String> batch, Map<String, PublishedInfo> published) {
        return batch.stream().filter(published::containsKey).toList();
    }

    /**
     * Builds the tile layers of the ids with a layer or group and with settings, in the order of the ids. Each tile
     * layer gets a new info.
     */
    private List<GeoServerTileLayer> toLayers(
            List<String> ids, Map<String, PublishedInfo> published, Map<String, TileLayerInfo> settings) {
        List<GeoServerTileLayer> tileLayers = new ArrayList<>();
        for (String id : ids) {
            PublishedInfo layerOrGroup = published.get(id);
            TileLayerInfo layerSettings = settings.get(id);
            if (null != layerOrGroup && null != layerSettings) {
                tileLayers.add(toLayer(layerSettings, layerOrGroup));
            }
        }
        return tileLayers;
    }

    @Override
    public Optional<TileLayer> getLayer(@NonNull String layerName) {
        Optional<GeoServerTileLayer> tileLayer = findTileLayer(layerName);
        return tileLayer.map(TileLayer.class::cast);
    }

    @Override
    public boolean containsLayer(@NonNull String layerName) {
        Optional<GeoServerTileLayer> tileLayer = findTileLayer(layerName);
        return tileLayer.isPresent();
    }

    /**
     * Finds the tile layer of a name among the kept tile layers, or builds it, see {@link BuiltTileLayerCache}. Empty
     * for a name of another workspace in a virtual service, see {@link #tileLayerName(String)}.
     */
    private Optional<GeoServerTileLayer> findTileLayer(String layerName) {
        Optional<TileLayerName> name = tileLayerName(layerName);
        if (name.isEmpty()) {
            return Optional.empty();
        }
        Supplier<Optional<PublishedInfo>> finder = () -> findPublished(name.get());
        return builtTileLayers.getLayer(layerName, finder, this::tileLayerOf);
    }

    /**
     * Reads a tile layer name. The workspace comes from the prefix of the name, or from the virtual service of a
     * workspace for a bare name. Empty for a name of another workspace in a virtual service: its tile layer is not
     * visible there.
     */
    private static Optional<TileLayerName> tileLayerName(String layerName) {
        String workspace = workspaceName(layerName);
        String localName = localName(layerName);
        WorkspaceInfo localWorkspace = LocalWorkspace.get();
        if (null == localWorkspace) {
            return Optional.of(new TileLayerName(workspace, localName));
        }
        String serviceWorkspace = localWorkspace.getName();
        if (null != workspace && !workspace.equals(serviceWorkspace)) {
            return Optional.empty();
        }
        return Optional.of(new TileLayerName(serviceWorkspace, localName));
    }

    private static @Nullable String workspaceName(String layerName) {
        int idx = layerName.indexOf(':');
        return idx == -1 ? null : layerName.substring(0, idx);
    }

    private static String localName(String layerName) {
        int idx = layerName.indexOf(':');
        return idx == -1 ? layerName : layerName.substring(1 + idx);
    }

    /**
     * Finds the layer or group of a tile layer name in the catalog. A name with a workspace is the layer with that
     * prefixed name, else the group of that workspace. A name without workspace is a group without workspace: layers
     * always have one.
     */
    private Optional<PublishedInfo> findPublished(TileLayerName name) {
        ExtendedCatalogFacade facade = catalogFacade.get();
        String workspaceName = name.workspace();
        if (null == workspaceName) {
            PublishedInfo globalGroup = facade.getLayerGroupByName(CatalogFacade.NO_WORKSPACE, name.localName());
            return Optional.ofNullable(globalGroup);
        }
        PublishedInfo layer = facade.getLayerByName(name.prefixedName());
        if (null != layer) {
            return Optional.of(layer);
        }
        return findGroup(facade, workspaceName, name.localName());
    }

    /** Finds the group of a workspace by name. Empty if the workspace does not exist. */
    private static Optional<PublishedInfo> findGroup(ExtendedCatalogFacade facade, String workspaceName, String name) {
        WorkspaceInfo workspace = facade.getWorkspaceByName(workspaceName);
        if (null == workspace) {
            return Optional.empty();
        }
        PublishedInfo group = facade.getLayerGroupByName(workspace, name);
        return Optional.ofNullable(group);
    }

    /** Builds the tile layer of a layer or group. Empty if the object has no GWC settings: it has no tile layer. */
    private Optional<GeoServerTileLayer> tileLayerOf(PublishedInfo published) {
        Optional<TileLayerInfo> settings = findSettings(published);
        return settings.map(found -> toLayer(found, published));
    }

    private Optional<TileLayerInfo> findSettings(PublishedInfo published) {
        return repository.findSettings(published.getId());
    }

    /**
     * Builds a tile layer from the GWC settings of a layer or group. The settings are only read: the tile layer gets a
     * new info, completed with the defaults of GWC.
     */
    GeoServerTileLayer toLayer(TileLayerInfo settings, PublishedInfo published) {
        GeoServerTileLayerInfo info = infoMapper.toInfo(settings, published);
        completeWithDefaults(info, published);
        return new GeoServerTileLayer(published, gridsetBroker, info);
    }

    @Override
    public void addLayer(@NonNull TileLayer tl) throws IllegalArgumentException {
        GeoServerTileLayer tileLayer = canSave(tl, "Can't add TileLayer of type: %s ", tl.getClass());
        save(tileLayer);
    }

    @Override
    public void modifyLayer(@NonNull TileLayer tl) throws NoSuchElementException {
        GeoServerTileLayer tileLayer = canSave(tl, "Can't save TileLayer of type: %s ", tl.getClass());
        boolean updated = save(tileLayer);
        if (!updated) {
            throw new NoSuchElementException("TileLayer %s does not exist".formatted(tl.getName()));
        }
    }

    /**
     * Saves the GWC settings of a tile layer under the id of its layer or group. Returns false if the catalog has no
     * layer or group with that id.
     */
    private boolean save(GeoServerTileLayer tileLayer) {
        TileLayerInfo settings = toInfo(tileLayer);
        String publishedId = tileLayer.getPublishedInfo().getId();
        return repository.save(publishedId, settings);
    }

    @Override
    public void removeLayer(@NonNull String layerName) {
        Optional<TileLayerName> name = tileLayerName(layerName);
        name.ifPresent(found -> remove(layerName, found));
    }

    /** Deletes the GWC settings of the layer or group of a name, then lets GWC remove the tiles stored for the name. */
    private void remove(String layerName, TileLayerName name) {
        Supplier<Optional<PublishedInfo>> finder = () -> findPublished(name);
        Optional<String> publishedId = builtTileLayers.findPublishedId(layerName, finder);
        publishedId.ifPresent(repository::delete);
        notifyBlobStoreLayerRemoved(name.prefixedName());
    }

    /**
     * Bridge to {@link GWC#layerRemoved(String)} so the {@link org.geowebcache.storage.StorageBroker} deletes the
     * on-disk blob store directory and fires {@code BlobStoreListener.layerDeleted} (which lets
     * {@code DiskQuotaMonitor} drop the layer's quota rows). Vanilla GeoServer's
     * {@code CatalogConfiguration.removeLayer} does the equivalent call; without it the pgconfig backend leaves orphan
     * blob directories and stale quota rows.
     *
     * <p>{@link GWC#get()} throws {@link NullPointerException} if the static singleton hasn't been initialized yet
     * (typical in unit tests that don't wire a full Spring context). In that case the bridge is a no-op.
     */
    private void notifyBlobStoreLayerRemoved(String prefixedLayerName) {
        GWC mediator;
        try {
            mediator = GWC.get();
        } catch (NullPointerException _) {
            return;
        }
        if (mediator == null) {
            return;
        }
        try {
            mediator.layerRemoved(prefixedLayerName);
        } catch (RuntimeException e) {
            // Best-effort cache/quota cleanup. The blob store may already have dropped the
            // layer (the GWC REST controller deletes it before calling removeLayer), in which
            // case GWC.layerRemoved throws on an "unknown layer". The authoritative removal
            // already succeeded, so this is not an error.
            log.debug("Blob store cleanup for removed layer {} skipped: {}", prefixedLayerName, e.getMessage());
        }
    }

    @Override
    public void renameLayer(String oldName, String newName) {
        // no-op, no need to rename, name is taken directly from the PublishedInfo
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
        return supportedAndNonTransient(tl).orElseThrow(() -> new IllegalArgumentException(message.formatted(args)));
    }

    private Optional<GeoServerTileLayer> supportedAndNonTransient(TileLayer tl) {
        if ((tl instanceof GeoServerTileLayer gsl) && !tl.isTransientLayer()) {
            return Optional.of(gsl);
        }
        return Optional.empty();
    }

    TileLayerInfo toInfo(GeoServerTileLayer tileLayer) {

        GeoServerTileLayerInfo tileLayerInfo = tileLayer.getInfo();
        checkNotNull(tileLayerInfo, "GeoServerTileLayerInfo is null");
        checkNotNull(tileLayerInfo.getId(), "id is null");
        checkNotNull(tileLayerInfo.getName(), "name is null");

        PublishedInfo publishedInfo = tileLayer.getPublishedInfo();
        completeWithDefaults(tileLayerInfo, publishedInfo);

        return infoMapper.map(tileLayer);
    }

    @VisibleForTesting
    void completeWithDefaults(GeoServerTileLayerInfo info, PublishedInfo publishedInfo) {
        GWCConfig defaults = getDefaultConfig();
        GeoServerTileLayerInfo infoDefaults = TileLayerInfoUtil.loadOrCreate(publishedInfo, defaults);

        CloudCatalogConfiguration.setMissingConfig(info, infoDefaults);
    }

    GWCConfig getDefaultConfig() {
        return defaultsProvider.getConfig();
    }

    /**
     * The name of a tile layer as the request sees it: the name of the workspace of its layer or group, null for a
     * group without workspace, and the local name.
     */
    private record TileLayerName(@Nullable String workspace, String localName) {

        String prefixedName() {
            return null == workspace ? localName : workspace + ":" + localName;
        }
    }
}
