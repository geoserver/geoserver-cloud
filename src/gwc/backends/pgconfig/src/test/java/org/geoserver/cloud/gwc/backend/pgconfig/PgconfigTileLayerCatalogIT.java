/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheBusListener;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.support.DecodeCounter;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geoserver.cloud.event.bus.RemoteEventsResumed;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type;
import org.geoserver.cloud.gwc.event.GridsetEvent;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the tile layer catalog over a catalog with the entity cache and the cache of the request, as the services build
 * them. Each test runs in a request, and {@link #newRequest()} starts another one.
 *
 * @since 1.7
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigTileLayerCatalogIT {

    private static final int REQUEST_CACHE_SIZE = 10_000;

    private static final int KEPT_TILE_LAYERS = 1_000;

    private static final String EPSG_4326 = "EPSG:4326";

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private StatementCounter counter;

    private EntityCache cache;

    private CatalogPlugin catalog;

    private TileLayerMocking support;

    private BuiltTileLayerCache builtTileLayers;

    private PgconfigTileLayerCatalog tlCatalog;

    @BeforeEach
    void setUp() {
        counter = new StatementCounter(db.getDataSource());
        cache = newEntityCache();
        catalog = requestCachedCatalog();
        GeoServerImpl geoServer = new PgconfigBackendBuilder(counter.dataSource()).createGeoServer(catalog);
        support = new TileLayerMocking(catalog, geoServer);
        builtTileLayers = new BuiltTileLayerCache(cache, counter.dataSource(), KEPT_TILE_LAYERS);
        tlCatalog = tileLayerCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
    }

    @AfterEach
    void cleanUp() {
        LocalWorkspace.remove();
        Dispatcher.REQUEST.remove();
        RequestCacheScope.close();
    }

    @Test
    void testSimpleLayer() {
        LayerInfo layerInfo = support.layerInfo();

        assertThat(tlCatalog.getLayer(layerInfo.prefixedName())).isEmpty();

        GeoServerTileLayer tl = support.geoServerTileLayer(layerInfo);
        tlCatalog.addLayer(tl);
        tlCatalog.getLayer(layerInfo.prefixedName());

        Optional<TileLayer> result = tlCatalog.getLayer(layerInfo.prefixedName());
        assertThat(result).isPresent().get().isInstanceOf(GeoServerTileLayer.class);
        GeoServerTileLayer gsl = (GeoServerTileLayer) result.orElseThrow();
        PublishedInfo publishedInfo = gsl.getPublishedInfo();
        assertThat(publishedInfo.getId()).isEqualTo(layerInfo.getId());

        LocalWorkspace.set(support.workspace("local"));
        assertThat(tlCatalog.getLayer(layerInfo.prefixedName())).isEmpty();

        LocalWorkspace.set(layerInfo.getResource().getStore().getWorkspace());
        assertThat(tlCatalog.getLayer(layerInfo.prefixedName())).isPresent();

        String unprefixedName = support.layerName(layerInfo);
        assertThat(tlCatalog.getLayer(unprefixedName)).isPresent();

        LocalWorkspace.remove();
        gsl.setBlobStoreId("newBlobStore");
        gsl.setEnabled(false);
        assertThat(gsl.getGridSubsets()).hasSize(2);
        gsl.removeGridSubset(gsl.getGridSubsets().iterator().next());
        tlCatalog.modifyLayer(gsl);

        GeoServerTileLayer modified = tlCatalog
                .getLayer(layerInfo.prefixedName())
                .map(GeoServerTileLayer.class::cast)
                .orElseThrow();
        assertThat(modified.getBlobStoreId()).isEqualTo("newBlobStore");
        assertThat(modified.isEnabled()).isFalse();
        assertThat(modified.getGridSubsets()).hasSize(1);

        final String oldName = layerInfo.prefixedName();

        ResourceInfo resource = catalog.getResource(layerInfo.getResource().getId(), ResourceInfo.class);
        resource.setName("newName");
        catalog.save(resource);

        final String newName = resource.prefixedName();

        layerInfo = catalog.getLayer(layerInfo.getId());
        assertThat(layerInfo.prefixedName()).isEqualTo(newName);

        assertThat(tlCatalog.containsLayer(oldName)).isFalse();
        assertThat(tlCatalog.containsLayer(newName)).isTrue();

        assertThat(tlCatalog.getLayer(oldName)).isEmpty();
        assertThat(tlCatalog.getLayer(newName)).isPresent();
        tlCatalog.removeLayer(unprefixedName);
    }

    @Test
    void tileLayerHasAltStyles() {
        LayerInfo layerInfo = support.layerInfo();
        StyleInfo s2 = addStyle(catalog);
        StyleInfo s3 = addStyle(catalog);
        LayerInfo layer = catalog.getLayer(layerInfo.getId());
        layer.getStyles().add(s2);
        layer.getStyles().add(s3);
        catalog.save(layer);
        tlCatalog.addLayer(support.geoServerTileLayer(catalog.getLayer(layerInfo.getId())));

        TileLayer found = tlCatalog.getLayer(layerInfo.prefixedName()).orElseThrow();
        LayerInfo published = (LayerInfo) ((GeoServerTileLayer) found).getPublishedInfo();
        Set<String> expected = Set.of(s2.getName(), s3.getName());
        Set<String> actual =
                published.getStyles().stream().map(StyleInfo::getName).collect(Collectors.toSet());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testMultipleLayers() {
        assertThat(tlCatalog.getLayerNames()).isEmpty();

        WorkspaceInfo ws1 = support.workspace("ws1");
        WorkspaceInfo ws2 = support.workspace("ws2");
        WorkspaceInfo ws3 = support.workspace("ws3");

        final int count = 100;
        List<String> ws1Layers = addLayers(ws1, count);
        assertThat(ws1Layers).hasSize(count);
        List<String> ws2Layers = addLayers(ws2, count);
        assertThat(ws2Layers).hasSize(count);
        List<String> ws3Layers = addLayers(ws3, count);
        assertThat(ws3Layers).hasSize(count);

        List<String> allNames = new ArrayList<>(ws1Layers);
        allNames.addAll(ws2Layers);
        allNames.addAll(ws3Layers);

        assertThat(tlCatalog.getLayerCount()).isEqualTo(allNames.size());

        Set<String> layernames = tlCatalog.getLayerNames();
        assertThat(layernames).hasSameSizeAs(allNames).containsAll(allNames);
        for (String name : allNames) {
            assertThat(tlCatalog.containsLayer(name)).isTrue();
        }

        List<GeoServerTileLayer> layers = tlCatalog.getLayers();
        assertThat(layers).hasSameSizeAs(allNames);
    }

    // the name is looked up while free, between the two renames
    @Test
    void renameOntoLookedUpName() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));
        String formerRoadsNewId = addTileLayer(support.layerInfo(ws, "roads_new"));
        tileLayer("ws:roads");

        renameResource("ws:roads", "roads_old");
        assertThat(tlCatalog.getLayer("ws:roads")).isEmpty();
        renameResource("ws:roads_new", "roads");

        assertThat(tileLayer("ws:roads").getPublishedInfo().getId()).isEqualTo(formerRoadsNewId);
    }

    @Test
    void disabledLayerShowsDisabled() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        assertThat(tileLayer("ws:roads").isEnabled()).isTrue();

        LayerInfo layer = catalog.getLayer(layerId);
        layer.setEnabled(false);
        catalog.save(layer);

        assertThat(tileLayer("ws:roads").isEnabled()).isFalse();
    }

    @Test
    void styleChangeShowsInTileLayer() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        tileLayer("ws:roads");

        StyleInfo highways = addStyle(catalog, "highways");
        LayerInfo layer = catalog.getLayer(layerId);
        layer.setDefaultStyle(highways);
        catalog.save(layer);

        LayerInfo published = (LayerInfo) tileLayer("ws:roads").getPublishedInfo();
        assertThat(published.getDefaultStyle().getName()).isEqualTo("highways");
    }

    @Test
    void boundsChangeShowsInTileLayer() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));
        tileLayer("ws:roads");

        FeatureTypeInfo resource = catalog.getResourceByName("ws:roads", FeatureTypeInfo.class);
        resource.setLatLonBoundingBox(new ReferencedEnvelope(-10, 10, -5, 5, DefaultGeographicCRS.WGS84));
        catalog.save(resource);

        LayerInfo published = (LayerInfo) tileLayer("ws:roads").getPublishedInfo();
        ReferencedEnvelope bounds = published.getResource().getLatLonBoundingBox();
        assertThat(bounds).as("lat/lon bounds").isNotNull();
        assertThat(List.of(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()))
                .containsExactly(-10d, -5d, 10d, 5d);
    }

    @Test
    void busResumeRereadsSettings() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        tileLayer("ws:roads");

        changeBlobStoreWithSql(layerId, "lost-event");
        newRequest();
        assertThat(tileLayer("ws:roads").getBlobStoreId()).isNotEqualTo("lost-event");

        new EntityCacheBusListener(List.of(cache)).onRemoteEventsResumed(new RemoteEventsResumed("q"));
        newRequest();
        assertThat(tileLayer("ws:roads").getBlobStoreId()).isEqualTo("lost-event");
    }

    @Test
    void remoteChangeSeenInRequest() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        assertThat(tileLayer("ws:roads").getBlobStoreId()).isNotEqualTo("other-instance");

        changeBlobStoreWithSql(layerId, "other-instance");
        TileLayerEvent remote = TileLayerEvent.modified(this, layerId, "ws:roads", null);
        remote.setRemote(true);
        new PgconfigTileLayerEventListener(cache, RemoteChangeCounter.shared()).onTileLayerEvent(remote);

        assertThat(tileLayer("ws:roads").getBlobStoreId()).isEqualTo("other-instance");
    }

    @Test
    void warmLookupDecodesNothing() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));
        GeoServerTileLayer built = tileLayer("ws:roads");
        newRequest();

        counter.reset();
        DecodeCounter decodes = new DecodeCounter();
        GeoServerTileLayer kept = tileLayer("ws:roads");

        assertThat(counter.count()).isZero();
        assertThat(decodes.count()).isZero();
        assertThat(kept).isSameAs(built);
    }

    @Test
    void catalogChangeRebuildsLayer() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        GeoServerTileLayer built = tileLayer("ws:roads");
        GeoServerTileLayer kept = tileLayer("ws:roads");

        LayerInfo layer = catalog.getLayer(layerId);
        layer.setEnabled(false);
        catalog.save(layer);

        GeoServerTileLayer rebuilt = tileLayer("ws:roads");
        assertThat(kept).isSameAs(built);
        assertThat(rebuilt).isNotSameAs(built);
        assertThat(rebuilt.isEnabled()).isFalse();
    }

    @Test
    void gwcEventDropsBuiltLayers() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));
        GeoServerTileLayer built = tileLayer("ws:roads");
        GeoServerTileLayer kept = tileLayer("ws:roads");

        builtTileLayers.onGeoWebCacheEvent(new GridsetEvent(this, Type.MODIFIED, EPSG_4326));

        GeoServerTileLayer rebuilt = tileLayer("ws:roads");
        assertThat(kept).isSameAs(built);
        assertThat(rebuilt).isNotSameAs(built);
    }

    // another instance changed the settings after the first lookup of this request
    @Test
    void buildReadsCurrentSettings() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        tileLayer("ws:roads");

        changeBlobStoreWithSql(layerId, "remote");
        cache.evict(CacheKey.tileLayer(layerId));

        assertThat(tileLayer("ws:roads").getBlobStoreId()).isEqualTo("remote");
    }

    // the getters of a ModificationProxy fill its maps: the requests sharing a kept tile layer read plain fields
    @Test
    void keptLayerHoldsRawDecode() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));

        PublishedInfo published = tileLayer("ws:roads").getPublishedInfo();

        assertThat(published).isInstanceOf(LayerInfoImpl.class);
        ResourceInfo resource = ((LayerInfo) published).getResource();
        assertThat(resource).isInstanceOf(FeatureTypeInfoImpl.class);
        assertThat(resource.getCatalog()).isNotNull();
    }

    // the request holds the layer already, and the cached bytes of the layer stay as stored
    @Test
    void privateDecodeForBuild() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        catalog.getLayer(layerId);
        String storedLayer = cachedLayer(layerId);
        newRequest();

        LayerInfo fromRequest = catalog.getFacade().getLayerByName("ws:roads");
        GeoServerTileLayer tileLayer = tileLayer("ws:roads");

        assertThat(unwrap(tileLayer)).isNotSameAs(ModificationProxy.unwrap(fromRequest));
        assertThat(cachedLayer(layerId)).isEqualTo(storedLayer);
    }

    // a layer takes the title of its resource
    @Test
    void callerObjectNotShared() {
        WorkspaceInfo ws = support.workspace("ws");
        LayerInfo layer = support.layerInfo(ws, "roads");
        FeatureTypeInfo resource = catalog.getResourceByName("ws:roads", FeatureTypeInfo.class);
        resource.setTitle("Roads");
        catalog.save(resource);
        GeoServerTileLayer tl = support.geoServerTileLayer(layer);
        tlCatalog.addLayer(tl);

        tl.getPublishedInfo().setTitle("changed without a save");

        assertThat(tileLayer("ws:roads").getPublishedInfo().getTitle()).isEqualTo("Roads");
    }

    @Test
    void namesFollowChanges() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));
        assertThat(tlCatalog.getLayerNames()).containsExactly("ws:roads");

        addTileLayer(support.layerInfo(ws, "rivers"));
        renameResource("ws:roads", "streets");

        assertThat(tlCatalog.getLayerNames()).containsExactlyInAnyOrder("ws:rivers", "ws:streets");
    }

    // an OWS request outside the virtual service of an isolated workspace sees none of its tile layers
    @Test
    void isolatedTileLayerOnlyInItsService() {
        WorkspaceInfo iso = isolatedWorkspace("iso");
        addTileLayer(support.layerInfo(iso, "roads"));
        Dispatcher.REQUEST.set(new Request());

        assertThat(tlCatalog.getLayer("iso:roads")).isEmpty();

        LocalWorkspace.set(iso);
        assertThat(tlCatalog.getLayer("iso:roads")).isPresent();
    }

    @Test
    void suspendedCacheBuildsPerCall() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));
        cache.setSuspended(true);

        GeoServerTileLayer first = tileLayer("ws:roads");
        GeoServerTileLayer second = tileLayer("ws:roads");

        assertThat(second).isNotSameAs(first);
    }

    // the virtual service of the workspace finds the tile layer kept for it, whatever was looked up in between
    @Test
    void isolatedContextsKeptApart() {
        WorkspaceInfo iso = isolatedWorkspace("iso");
        addTileLayer(support.layerInfo(iso, "roads"));
        Dispatcher.REQUEST.set(new Request());
        long epoch = cache.epoch();

        LocalWorkspace.set(iso);
        GeoServerTileLayer inService = tileLayer("iso:roads");
        LocalWorkspace.remove();
        Optional<TileLayer> outsideService = tlCatalog.getLayer("iso:roads");
        LocalWorkspace.set(iso);
        GeoServerTileLayer againInService = tileLayer("iso:roads");

        assertThat(outsideService).isEmpty();
        assertThat(againInService).isSameAs(inService);
        assertThat(cache.epoch()).isEqualTo(epoch);
    }

    @Test
    void coldLookupOneQuery() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "l"));
        cache.clear();
        newRequest();

        counter.reset();
        tileLayer("ws:l");
        int coldStatements = counter.count();
        counter.reset();
        tileLayer("ws:l");

        assertThat(coldStatements).isOne();
        assertThat(counter.count()).isZero();
    }

    @Test
    void noTileLayerIsCached() {
        WorkspaceInfo ws = support.workspace("ws");
        support.layerInfo(ws, "plain");
        assertThat(tlCatalog.getLayer("ws:plain")).isEmpty();
        newRequest();

        counter.reset();
        assertThat(tlCatalog.getLayer("ws:plain")).isEmpty();
        assertThat(counter.count()).isZero();
    }

    // the cache of the request and the entity cache both remember that the layer had no tile layer
    @Test
    void addedAfterNoneVisible() {
        WorkspaceInfo ws = support.workspace("ws");
        LayerInfo layer = support.layerInfo(ws, "plain");
        assertThat(tlCatalog.getLayer("ws:plain")).isEmpty();
        newRequest();
        assertThat(tlCatalog.getLayer("ws:plain")).isEmpty();

        tlCatalog.addLayer(support.geoServerTileLayer(layer));

        assertThat(tlCatalog.getLayer("ws:plain")).isPresent();
    }

    // a seed task, or a worker after the end of its request, reads the shared caches
    @Test
    void lookupWithoutScope() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayer(support.layerInfo(ws, "roads"));
        RequestCacheScope.close();

        GeoServerTileLayer first = tileLayer("ws:roads");
        GeoServerTileLayer second = tileLayer("ws:roads");

        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        RequestCache killed = RequestCacheScope.current();
        killed.kill();
        GeoServerTileLayer third = tileLayer("ws:roads");

        assertThat(second).isSameAs(first);
        assertThat(third).isSameAs(first);
    }

    // the request holds the layers and groups of the first listing
    @Test
    void listingsInRequestDecodeOnce() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayers(ws, 30);
        tlCatalog.getLayers();

        DecodeCounter decodes = new DecodeCounter();
        List<GeoServerTileLayer> second = tlCatalog.getLayers();

        assertThat(second).hasSize(30);
        assertThat(decodes.count()).isZero();
    }

    @Test
    void warmListingOneQuery() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayers(ws, 30);
        tlCatalog.getLayers();
        newRequest();

        counter.reset();
        List<GeoServerTileLayer> warm = tlCatalog.getLayers();

        assertThat(warm).hasSize(30);
        assertThat(counter.count()).isOne();
    }

    // one query for the ids, one for the layers with their settings
    @Test
    void coldListingTwoQueries() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayers(ws, 30);
        cache.clear();
        newRequest();

        counter.reset();
        List<GeoServerTileLayer> cold = tlCatalog.getLayers();

        assertThat(cold).hasSize(30);
        assertThat(counter.count()).isEqualTo(2);
    }

    @Test
    void listingShowsCatalogChanges() {
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        boolean enabledBefore = tlCatalog.getLayers().getFirst().isEnabled();

        LayerInfo layer = catalog.getLayer(layerId);
        layer.setEnabled(false);
        catalog.save(layer);

        assertThat(enabledBefore).isTrue();
        assertThat(tlCatalog.getLayers().getFirst().isEnabled()).isFalse();
    }

    // in and out of the virtual service of the workspace, with one layer disabled in the catalog, and one tile layer
    // disabled with one grid subset removed
    @Test
    void listingsAndLookupsAgree() {
        WorkspaceInfo ws = support.workspace("ws");
        List<String> layerIds = addTileLayers(ws, 3);
        LayerInfo member = catalog.getLayer(layerIds.getFirst());
        addTileLayer(addLayerGroup(ws, "wsgroup", member));
        addTileLayer(addLayerGroup(null, "globalgroup", member));
        LayerInfo disabled = catalog.getLayer(layerIds.get(1));
        disabled.setEnabled(false);
        catalog.save(disabled);
        tlCatalog.modifyLayer(disabledWithoutEpsg4326(catalog.getLayer(layerIds.get(2))));

        List<GeoServerTileLayer> listed = listedAsLookedUp();
        newRequest();
        LocalWorkspace.set(ws);
        List<GeoServerTileLayer> listedInService = listedAsLookedUp();

        assertThat(listed).hasSize(5);
        assertThat(listedInService).hasSize(4);
        assertThat(tlCatalog.getLayer("globalgroup")).isEmpty();
    }

    // the virtual service of a workspace lists no layer or group of another workspace, and no group without workspace
    @Test
    void virtualServiceListsItsLayers() {
        WorkspaceInfo ws = support.workspace("ws");
        WorkspaceInfo other = support.workspace("other");
        String layerId = addTileLayer(support.layerInfo(ws, "roads"));
        LayerInfo member = catalog.getLayer(layerId);
        String groupId = addTileLayer(addLayerGroup(ws, "wsgroup", member));
        addTileLayer(addLayerGroup(null, "globalgroup", member));
        String otherLayerId = addTileLayer(support.layerInfo(other, "rivers"));
        addTileLayer(addLayerGroup(other, "othergroup", catalog.getLayer(otherLayerId)));
        Dispatcher.REQUEST.set(new Request());
        LocalWorkspace.set(ws);

        List<GeoServerTileLayer> listed = tlCatalog.getLayers();

        assertThat(listed).extracting(TileLayer::getId).containsExactlyInAnyOrder(layerId, groupId);
    }

    // an OWS request outside the virtual service of an isolated workspace lists none of its tile layers
    @Test
    void isolatedListedOnlyInItsService() {
        WorkspaceInfo iso = isolatedWorkspace("iso");
        String isolatedId = addTileLayer(support.layerInfo(iso, "roads"));
        WorkspaceInfo ws = support.workspace("ws");
        String layerId = addTileLayer(support.layerInfo(ws, "rivers"));
        Dispatcher.REQUEST.set(new Request());

        List<GeoServerTileLayer> outsideService = tlCatalog.getLayers();
        newRequest();
        LocalWorkspace.set(iso);
        List<GeoServerTileLayer> inService = tlCatalog.getLayers();

        assertThat(outsideService).extracting(TileLayer::getId).containsExactly(layerId);
        assertThat(inService).extracting(TileLayer::getId).containsExactly(isolatedId);
    }

    // past the budget, the rows are read without being put, and each group looks up its members and styles
    @Test
    void overBudgetListingCompletes() {
        WorkspaceInfo ws = support.workspace("ws");
        List<String> layerIds = addTileLayers(ws, 30);
        PgconfigTileLayerCatalog bounded = tileLayerCatalog(tileLayerRepository(), () -> new PutBudget(2 * 1024));
        int coldLayersStatements = coldListingStatements(bounded, 30);
        long cachedLayerSettings = cachedSettings(layerIds);

        int groups = 3;
        for (int i = 0; i < groups; i++) {
            LayerInfo member = support.layerInfo(ws, "member" + i);
            addTileLayer(addLayerGroup(ws, "group" + i, member));
        }
        int coldGroupsStatements = coldListingStatements(bounded, 30 + groups);

        assertThat(coldLayersStatements).isLessThanOrEqualTo(3);
        assertThat(cachedLayerSettings).isLessThan(layerIds.size());
        assertThat(coldGroupsStatements).isLessThanOrEqualTo(3 + 2 * groups);
    }

    // the layer is removed through the catalog between the ids query of the listing and the read of its batch
    @Test
    void listingSkipsRemovedLayer() {
        WorkspaceInfo ws = support.workspace("ws");
        addTileLayers(ws, 3);
        String removedId = addTileLayer(support.layerInfo(ws, "removed"));
        catalog.remove(catalog.getLayer(removedId));
        TileLayerInfoRepository repository = tileLayerRepository();
        List<String> listedIds = repository.findAllIds();
        List<String> idsWithRemoved = new ArrayList<>(listedIds);
        idsWithRemoved.add(1, removedId);
        TileLayerInfoRepository stale = mock(TileLayerInfoRepository.class, delegatesTo(repository));
        doReturn(idsWithRemoved).when(stale).findAllIds();

        List<GeoServerTileLayer> listed =
                tileLayerCatalog(stale, () -> PutBudget.halfOf(cache)).getLayers();

        assertThat(listed).doesNotContainNull().extracting(TileLayer::getId).containsExactlyElementsOf(listedIds);
    }

    /** Looks a tile layer up, and fails the test if the catalog does not find it. */
    private GeoServerTileLayer tileLayer(String name) {
        Optional<TileLayer> found = tlCatalog.getLayer(name);
        assertThat(found).as(name).isPresent();
        return (GeoServerTileLayer) found.orElseThrow();
    }

    /** Gives the layer or group a tile layer, and returns the id of the layer or group. */
    private String addTileLayer(PublishedInfo published) {
        tlCatalog.addLayer(support.geoServerTileLayer(published));
        return published.getId();
    }

    /**
     * A tile layer of the layer or group, disabled and without the EPSG:4326 grid subset. The settings change before
     * the tile layer is built. Removing a grid subset from a tile layer computes its grids, and a layer without bounds
     * then logs warnings.
     */
    private static GeoServerTileLayer disabledWithoutEpsg4326(PublishedInfo published) {
        GeoServerTileLayerInfo settings = TileLayerInfoUtil.loadOrCreate(published, new GWCConfig());
        settings.setEnabled(false);
        settings.getGridSubsets().removeIf(subset -> EPSG_4326.equals(subset.getGridSetName()));
        return new GeoServerTileLayer(published, geoServerGridsets(), settings);
    }

    /** Adds a group holding the layer, in the workspace, or without workspace if it is null. */
    private LayerGroupInfo addLayerGroup(@Nullable WorkspaceInfo workspace, String name, LayerInfo layer) {
        LayerGroupInfo group = support.getFaker().layerGroupInfo(workspace);
        group.setName(name);
        group.getLayers().add(layer);
        catalog.add(group);
        return catalog.getLayerGroup(group.getId());
    }

    /**
     * Lists the tile layers, checks that each one agrees with its lookup by name, see {@link #summary}, and returns
     * them.
     */
    private List<GeoServerTileLayer> listedAsLookedUp() {
        List<GeoServerTileLayer> listed = tlCatalog.getLayers();
        List<Tuple> lookedUp = listed.stream()
                .map(listedLayer -> tileLayer(listedLayer.getName()))
                .map(PgconfigTileLayerCatalogIT::summary)
                .toList();
        assertThat(listed).extracting(PgconfigTileLayerCatalogIT::summary).containsExactlyElementsOf(lookedUp);
        return listed;
    }

    /** What a listing and a lookup by name must agree on: id, name, enabled flag and grid subsets. */
    private static Tuple summary(GeoServerTileLayer tileLayer) {
        return tuple(tileLayer.getId(), tileLayer.getName(), tileLayer.isEnabled(), tileLayer.getGridSubsets());
    }

    /**
     * Lists the tile layers with an empty entity cache, in a new request, checks their number, and returns the
     * statements run.
     */
    private int coldListingStatements(PgconfigTileLayerCatalog listing, int expectedTileLayers) {
        cache.clear();
        newRequest();
        counter.reset();
        List<GeoServerTileLayer> listed = listing.getLayers();
        assertThat(listed).hasSize(expectedTileLayers);
        return counter.count();
    }

    /** The number of layers and groups of the ids with their GWC settings in the entity cache. */
    private long cachedSettings(List<String> publishedIds) {
        return publishedIds.stream()
                .map(CacheKey::tileLayer)
                .filter(cache::contains)
                .count();
    }

    /** Gives tile layers to new layers of the workspace, and returns the ids of the layers. */
    private List<String> addTileLayers(WorkspaceInfo workspace, int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LayerInfo layer = support.layerInfo(workspace, "layer" + i);
            ids.add(addTileLayer(layer));
        }
        return ids;
    }

    /** Ends the request of the test and starts another one, with an empty cache of the request. */
    private void newRequest() {
        RequestCacheScope.close();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
    }

    /** Renames a resource through the catalog: its layer takes the new name. */
    private void renameResource(String prefixedName, String newName) {
        FeatureTypeInfo resource = catalog.getResourceByName(prefixedName, FeatureTypeInfo.class);
        resource.setName(newName);
        catalog.save(resource);
    }

    /** Changes the blob store of a tile layer in the database alone, as if the event of the change were lost. */
    private void changeBlobStoreWithSql(String layerId, String blobStoreId) {
        String sql =
                """
                UPDATE publishedinfo SET tilelayer = jsonb_set(tilelayer, '{blobStoreId}', to_jsonb(?::text)) \
                WHERE id = ?
                """;
        int updated = db.getTemplate().update(sql, blobStoreId, layerId);
        assertThat(updated).isOne();
    }

    /** The JSON of a layer held by the entity cache, as a copy: a change to the cached array would show. */
    private String cachedLayer(String layerId) {
        Lookup found = cache.lookup(CacheKey.entity(ConfigInfoType.LAYER, layerId));
        assertThat(found).isInstanceOf(Lookup.Hit.class);
        return new String(((Lookup.Hit) found).json(), UTF_8);
    }

    private static PublishedInfo unwrap(GeoServerTileLayer tileLayer) {
        return ModificationProxy.unwrap(tileLayer.getPublishedInfo());
    }

    private WorkspaceInfo isolatedWorkspace(String name) {
        CatalogFaker faker = support.getFaker();
        WorkspaceInfo workspace = faker.workspaceInfo(name);
        workspace.setIsolated(true);
        NamespaceInfo namespace = faker.namespace(name);
        namespace.setIsolated(true);
        catalog.add(workspace);
        catalog.add(namespace);
        return workspace;
    }

    /**
     * The tile layer catalog as the services build it: the settings cached in the request, over the pgconfig repository
     * with the entity cache of the catalog, the tile layers found by name kept until the catalog changes, and a budget
     * of half the entity cache for each listing.
     */
    private PgconfigTileLayerCatalog tileLayerCatalog() {
        Supplier<PutBudget> listingBudget = () -> PutBudget.halfOf(cache);
        return tileLayerCatalog(tileLayerRepository(), listingBudget);
    }

    private PgconfigTileLayerCatalog tileLayerCatalog(
            TileLayerInfoRepository repository, Supplier<PutBudget> listingBudget) {
        return new PgconfigTileLayerCatalog(
                repository,
                geoServerGridsets(),
                catalog::getFacade,
                defaultsProvider(),
                builtTileLayers,
                listingBudget);
    }

    /** The GWC settings cached in the request, over the pgconfig repository with the entity cache of the catalog. */
    private TileLayerInfoRepository tileLayerRepository() {
        JdbcTemplate template = new JdbcTemplate(counter.dataSource());
        return new RequestCachingTileLayerInfoRepository(new PgconfigTileLayerInfoRepository(template, cache));
    }

    /** The grid sets of GeoServer, named like the grid subsets of the default tile layer settings. */
    private static GridSetBroker geoServerGridsets() {
        DefaultGridsets gridsets = new DefaultGridsets(true, true);
        return new GridSetBroker(List.of(gridsets));
    }

    private static GWCConfigPersister defaultsProvider() {
        GWCConfigPersister defaultsProvider = mock(GWCConfigPersister.class);
        when(defaultsProvider.getConfig()).thenReturn(new GWCConfig());
        return defaultsProvider;
    }

    /** A catalog with an entity cache and the cache of the request, as the backend configuration builds it. */
    private CatalogPlugin requestCachedCatalog() {
        CatalogPlugin cached = new CatalogPlugin();
        JdbcTemplate template = new JdbcTemplate(counter.dataSource());
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(template, cache);
        cached.setFacade(PgconfigBackendBuilder.requestCaching(facade));
        return cached;
    }

    private static EntityCache newEntityCache() {
        EntityCache entityCache =
                new EntityCache("gs-catalog", 16 * 1024 * 1024, Duration.ofSeconds(30), new NoopCodec());
        entityCache.setInitialized(true);
        return entityCache;
    }

    private List<String> addLayers(WorkspaceInfo workspace, int count) {

        CatalogFaker faker = support.getFaker();
        DataStoreInfo ds = faker.dataStoreInfo(workspace);
        faker.catalog().add(ds);

        StyleInfo defaultStyle = faker.styleInfo();
        faker.catalog().add(defaultStyle);

        return IntStream.range(0, count)
                .parallel()
                .mapToObj(i -> {
                    String name = faker.name() + "-" + i;
                    FeatureTypeInfo featureType = faker.featureTypeInfo(ds, name);
                    faker.catalog().add(featureType);

                    LayerInfo layer = faker.layerInfo(featureType, defaultStyle);
                    faker.catalog().add(layer);

                    return support.geoServerTileLayer(layer);
                })
                .peek(tlCatalog::addLayer)
                .map(GeoServerTileLayer::getName)
                .toList();
    }

    private StyleInfo addStyle(Catalog target) {
        StyleInfo style = support.getFaker().styleInfo();
        target.add(style);
        return style;
    }

    private StyleInfo addStyle(Catalog target, String name) {
        StyleInfo style = support.getFaker().styleInfo(name);
        target.add(style);
        return target.getStyleByName(name);
    }
}
