/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.catalog.CatalogFacade.NO_WORKSPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.catalog.plugin.requestcache.RequestCachingCatalogFacade;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.backend.pgconfig.support.DecodeCounter;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PgconfigCatalogFacadeIT {

    private static final int REQUEST_CACHE_SIZE = 10_000;

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private StatementCounter counter;
    private CatalogPlugin catalog;
    private ExtendedCatalogFacade facade;
    private String layerId;
    private String layer2Id;
    private String groupId;
    private String s1Id;
    private String s2Id;

    @BeforeEach
    void setUp() {
        counter = new StatementCounter(db.getDataSource());
        catalog = new PgconfigBackendBuilder(counter.dataSource()).createCatalog();
        facade = (ExtendedCatalogFacade) catalog.getRawFacade();
        createWorkspace("ws");
        s1Id = addStyle("s1").getId();
        s2Id = addStyle("s2").getId();
        LayerInfo layer = addLayer("l1");
        layerId = layer.getId();
        layer2Id = addLayer("l2").getId();
        groupId = addGroup("g1", layer).getId();
        counter.reset();
    }

    @AfterEach
    void endRequest() {
        LocalWorkspace.remove();
        Dispatcher.REQUEST.remove();
        RequestCacheScope.close();
    }

    @Test
    // 1 for the group row, 1 for its members
    void getPublishedGroupTwoQueries() {
        PublishedInfo found = facade.getPublished(groupId);

        assertThat(found).isInstanceOf(LayerGroupInfo.class);
        assertThat(counter.count()).isEqualTo(2);
    }

    @Test
    void getPublishedLayerOneQuery() {
        assertThat(facade.getPublished(layerId)).isInstanceOf(LayerInfo.class);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void getPublishedUnknownIsNull() {
        assertThat(facade.getPublished("nope")).isNull();
    }

    @Test
    // 1 for the ids, 1 for the members of the group
    void publishedByIdTwoQueries() {
        List<String> ids = List.of(layerId, layer2Id, groupId, "nope");
        List<PublishedInfo> found = findAll(ids, PublishedInfo.class);
        assertThat(found).extracting(PublishedInfo::getId).containsExactlyInAnyOrder(layerId, layer2Id, groupId);
        assertThat(counter.count()).isEqualTo(2);
    }

    @Test
    void budgetBoundsBulkPuts() {
        List<StyleInfo> styles = addStyles("budget", 20);
        List<String> ids = addLayersOnOneStore("budget", styles).stream()
                .map(LayerInfo::getId)
                .toList();
        EntityCache unbounded = newEntityCache();
        EntityCache bounded = newEntityCache();

        List<LayerInfo> all = findAllCold(unbounded, ids);
        List<LayerInfo> some;
        try (PutBudget.Binding binding = new PutBudget(4 * 1024).bind()) {
            some = findAllCold(bounded, ids);
        }

        assertThat(all).hasSize(20);
        assertThat(countCachedLayers(unbounded, ids)).isEqualTo(20);
        assertThat(some).extracting(LayerInfo::getId).containsExactlyInAnyOrderElementsOf(ids);
        some.forEach(this::assertResolved);
        assertThat(counter.count()).isOne();
        assertThat(countCachedLayers(bounded, ids)).isPositive().isLessThan(20);
    }

    @Test
    void stylesByIdOneQuery() {
        List<StyleInfo> found = findAll(List.of(s1Id, s2Id), StyleInfo.class);
        assertThat(found).extracting(StyleInfo::getId).containsExactlyInAnyOrder(s1Id, s2Id);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    // no repository holds all the types, each id is looked up on its own
    void catalogInfoTypeFindsAnyType() {
        List<CatalogInfo> found = findAll(List.of(layerId, s1Id), CatalogInfo.class);
        assertThat(found).extracting(CatalogInfo::getId).containsExactlyInAnyOrder(layerId, s1Id);
    }

    @Test
    // more ids than the 65,535 bind parameters accepted for one statement
    void manyIdsInOneQuery() {
        List<String> unknownIds =
                IntStream.range(0, 70_000).mapToObj("id-%d"::formatted).toList();
        List<String> ids = new ArrayList<>(unknownIds);
        ids.add(layerId);

        assertThat(findAll(ids, PublishedInfo.class))
                .extracting(PublishedInfo::getId)
                .containsExactly(layerId);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void emptyIdsRunNoQuery() {
        assertThat(findAll(List.of(), PublishedInfo.class)).isEmpty();
        assertThat(counter.count()).isZero();
    }

    @Test
    void listPublishedResolvesMembers() {
        Filter groups = Predicates.isInstanceOf(LayerGroupInfo.class);
        try (CloseableIterator<PublishedInfo> found = catalog.list(PublishedInfo.class, groups)) {
            LayerGroupInfo group = (LayerGroupInfo) found.next();
            PublishedInfo member = group.getLayers().get(0);

            assertThat(ProxyUtils.isResolvingProxy(member)).isFalse();
            assertThat(member).isInstanceOf(LayerInfo.class);
            assertThat(member.getName()).isEqualTo("l1");
        }
    }

    // the layer comes from the cache with its references, as a row would bring them
    @Test
    void cachedLayerIsResolved() {
        CatalogPlugin cached = cachedCatalog();
        cached.getLayer(layerId);
        counter.reset();

        LayerInfo layer = cached.getLayer(layerId);

        assertThat(counter.count()).isZero();
        assertThat(ProxyUtils.isResolvingProxy(layer.getResource())).isFalse();
        assertThat(layer.getResource().getStore().getWorkspace().getName()).isEqualTo("ws");
        assertThat(layer.getResource().getNamespace().getPrefix()).isEqualTo("ws");
        assertThat(layer.getDefaultStyle().getName()).isEqualTo("s1");
    }

    // the row of the group brings its members and its styles
    @Test
    void cachedGroupInOneQuery() {
        String styledGroupId = addGroup("styled", catalog.getLayer(layerId), catalog.getStyle(s2Id))
                .getId();
        CatalogPlugin cached = cachedCatalog();
        counter.reset();

        LayerGroupInfo group = cached.getLayerGroup(styledGroupId);
        PublishedInfo member = group.getLayers().get(0);
        String resourceName = ((LayerInfo) member).getResource().getName();
        String styleName = group.getStyles().get(0).getName();

        assertThat(member.getName()).isEqualTo("l1");
        assertThat(resourceName).isEqualTo("l1");
        assertThat(styleName).isEqualTo("s2");
        assertThat(counter.count()).isEqualTo(1);
    }

    // listed, or looked up from the cache: the references reach the catalog resolved
    @Test
    void layersArriveResolved() {
        CatalogPlugin cached = cachedCatalog();
        cached.getLayer(layerId);
        List<LayerInfo> layers = new ArrayList<>(cached.getLayers());
        layers.add(cached.getLayer(layerId));

        assertThat(layers).hasSize(3).allSatisfy(this::assertResolved);
    }

    // an OWS request without a local workspace sees no layer of an isolated workspace
    @Test
    void isolatedLayerHiddenFromGlobalList() {
        addIsolatedLayer();
        CatalogPlugin cached = cachedCatalog();
        Dispatcher.REQUEST.set(new Request());

        assertThat(cached.getLayers()).extracting(LayerInfo::getName).doesNotContain("roads");
        assertThat(cached.getLayerByName("iso:roads")).isNull();

        LocalWorkspace.set(cached.getWorkspaceByName("iso"));
        assertThat(cached.getLayers()).extracting(LayerInfo::getName).contains("roads");
        assertThat(cached.getLayerByName("iso:roads")).isNotNull();
    }

    // the lookup of a layer by its prefixed name reads the name key and the entities of the cache
    @Test
    void prefixedLayerNameNoQuery() {
        CatalogPlugin cached = requestCachedCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        LayerInfo first = cached.getLayerByName("ws:l1");
        RequestCacheScope.close();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        counter.reset();

        LayerInfo second = cached.getLayerByName("ws:l1");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(counter.count()).isZero();
    }

    @Test
    void bareNameUsesDefaultWorkspace() {
        createWorkspace("ws2");
        String inWs2 = addLayer("ws2", "l1").getId();
        catalog.setDefaultWorkspace(catalog.getWorkspaceByName("ws2"));
        CatalogPlugin cached = cachedCatalog();

        LayerInfo found = cached.getLayerByName("l1");

        assertThat(found.getId()).isEqualTo(inWs2).isNotEqualTo(layerId);
    }

    @Test
    void isolatedPrefixedNameHidden() {
        addIsolatedLayer();
        CatalogPlugin cached = requestCachedCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        Dispatcher.REQUEST.set(new Request());

        assertThat(cached.getLayerByName("iso:roads")).isNull();

        LocalWorkspace.set(cached.getWorkspaceByName("iso"));
        assertThat(cached.getLayerByName("iso:roads")).isNotNull();
    }

    // w1:x is the layer of the namespace w1, as upstream answers, not the layer of a store in the workspace w1
    @Test
    void prefixIsTheNamespace() {
        createWorkspace("w1");
        createWorkspace("w2");
        String inNamespaceW1 = addLayerInNamespace("w2", "w1", "x").getId();
        String inNamespaceW2 = addLayerInNamespace("w1", "w2", "x").getId();
        CatalogPlugin cached = cachedCatalog();

        LayerInfo byW1 = cached.getLayerByName("w1:x");
        LayerInfo byW2 = cached.getLayerByName("w2:x");

        LayerInfo warmW1 = cached.getLayerByName("w1:x");

        assertThat(byW1.getId()).isEqualTo(inNamespaceW1);
        assertThat(byW2.getId()).isEqualTo(inNamespaceW2);
        assertThat(warmW1.getId()).isEqualTo(inNamespaceW1);
    }

    // the first lookup of a global request puts the layer of the isolated workspace in the cache of the request, below
    // isolation, and isolation hides the cached layer
    @Test
    void isolatedLayerHiddenInRequest() {
        String isolatedId = addIsolatedLayer().getId();
        CatalogPlugin cached = requestCachedCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        Dispatcher.REQUEST.set(new Request());
        List<String> listed = listNames(cached, LayerInfo.class, Filter.INCLUDE);
        LayerInfo firstById = cached.getLayer(isolatedId);
        DecodeCounter decodes = new DecodeCounter();

        LayerInfo byId = cached.getLayer(isolatedId);
        long decodedById = decodes.count();
        LayerInfo byName = cached.getLayerByName("iso:roads");

        assertThat(listed).contains("l1").doesNotContain("roads");
        assertThat(firstById).isNull();
        assertThat(byId).isNull();
        // the cache of the request held the layer
        assertThat(decodedById).isZero();
        assertThat(byName).isNull();
    }

    // the first lookup reads the database, the second the cache
    @Test
    void isolatedLayerHiddenById() {
        String isolatedId = addIsolatedLayer().getId();
        CatalogPlugin cached = cachedCatalog();
        Dispatcher.REQUEST.set(new Request());

        assertThat(cached.getLayer(isolatedId)).isNull();
        counter.reset();
        assertThat(cached.getLayer(isolatedId)).isNull();
        assertThat(counter.count()).isZero();

        LocalWorkspace.set(cached.getWorkspaceByName("iso"));
        assertThat(cached.getLayer(isolatedId)).isNotNull();
    }

    // the access check of a GetCapabilities request reads the members of the listed groups
    @Test
    void inMemoryFilterSeesMembers() {
        LayerInfo l1 = catalog.getLayer(layerId);
        LayerInfo l2 = catalog.getLayer(layer2Id);
        LayerGroupInfo child = addGroup("child", List.of(l2));
        addGroup("parent", List.of(child, l1));
        Catalog advertised = new AdvertisedCatalog(cachedCatalog());
        Dispatcher.REQUEST.set(capabilitiesRequest());

        List<String> names = listNames(advertised, LayerGroupInfo.class, Filter.INCLUDE);

        assertThat(names).contains("parent", "child");
    }

    // an access check reads the workspace of a layer through its resource and its store
    @Test
    void inMemoryFilterSeesWorkspace() {
        createWorkspace("ws2");
        addLayer("ws2", "l3");

        List<String> names = listNames(cachedCatalog(), LayerInfo.class, layersOfWorkspace("ws2"));

        assertThat(names).containsExactly("l3");
    }

    // the cache gives a default style of another workspace with its workspace as a proxy
    @Test
    void inMemoryFilterSeesStyleWorkspace() {
        createWorkspace("ws2");
        StyleInfo style = addStyle("ws2", "ws2-style");
        addLayer("ws", "l3", style, List.of());

        List<String> names = listNames(cachedCatalog(), LayerInfo.class, layersWithStyleOf("ws2"));

        assertThat(names).containsExactly("l3");
    }

    // a listing row gives the alternate style the instance of the default style, with a proxy workspace from the
    // cache. Resolving that workspace changes the hash of a style held by the set.
    @Test
    void styleSetFindsResolvedStyle() {
        createWorkspace("ws2");
        for (int i = 1; i <= 3; i++) {
            StyleInfo style = addStyle("ws2", "ws2-style-%d".formatted(i));
            addLayer("ws", "cross-%d".formatted(i), style, List.of(style));
        }

        List<LayerInfo> listed = listUnwrapped(cachedCatalog());

        List<LayerInfo> crossLayers = listed.stream()
                .filter(layer -> layer.getName().startsWith("cross-"))
                .toList();
        assertThat(crossLayers).hasSize(3).allSatisfy(this::assertStylesFindDefaultStyle);
    }

    // the distinct objects: the group, its 10 layers and their resources, the store, workspace and namespace, 10 styles
    @Test
    void groupLookupDecodesEachObjectOnce() {
        List<StyleInfo> styles = addStyles("shared", 10);
        List<LayerInfo> layers = addLayersOnOneStore("shared", styles);
        String sharedId = addGroup("shared", layers, styles).getId();
        CatalogPlugin cached = cachedCatalog();
        cached.getLayerGroup(sharedId);
        counter.reset();
        DecodeCounter decodes = new DecodeCounter();

        cached.getLayerGroup(sharedId);

        int distinctObjects = 1 + 2 * layers.size() + 3 + styles.size();
        assertThat(counter.count()).isZero();
        assertThat(decodes.count()).isEqualTo(distinctObjects);
    }

    // the distinct objects: the group, its 10 layers and their resources, the store, workspace and namespace, 10 styles
    @Test
    void oneDecodePerObjectPerRequest() {
        List<StyleInfo> styles = addStyles("shared", 10);
        List<LayerInfo> layers = addLayersOnOneStore("shared", styles);
        String sharedId = addGroup("shared", layers, styles).getId();
        CatalogPlugin cached = requestCachedCatalog();
        cached.getLayerGroup(sharedId);
        counter.reset();
        DecodeCounter decodes = new DecodeCounter();

        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        cached.getLayerGroup(sharedId);
        cached.getLayerGroup(sharedId);

        int distinctObjects = 1 + 2 * layers.size() + 3 + styles.size();
        assertThat(decodes.count()).isEqualTo(distinctObjects);
        assertThat(counter.count()).isZero();
    }

    // the cache of the request keeps the group as the first lookup resolved it: a hit decodes and queries nothing
    @Test
    void hitSkipsResolution() {
        List<StyleInfo> styles = addStyles("shared", 10);
        List<LayerInfo> layers = addLayersOnOneStore("shared", styles);
        String sharedId = addGroup("shared", layers, styles).getId();
        CatalogPlugin cached = requestCachedCatalog();
        AtomicInteger resolverPasses = countResolverPasses(cached);
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        cached.getLayerGroup(sharedId);
        counter.reset();
        DecodeCounter decodes = new DecodeCounter();
        resolverPasses.set(0);

        cached.getLayerGroup(sharedId);

        assertThat(resolverPasses).hasValue(0);
        assertThat(decodes.count()).isZero();
        assertThat(counter.count()).isZero();
    }

    // the second group finds the layers, styles and store of the first one in the cache of the request
    @Test
    void sharedMembersDecodedOnce() {
        List<StyleInfo> styles = addStyles("shared", 10);
        List<LayerInfo> layers = addLayersOnOneStore("shared", styles);
        String firstId = addGroup("first", layers, styles).getId();
        String secondId = addGroup("second", layers, styles).getId();
        CatalogPlugin cached = requestCachedCatalog();
        cached.getLayerGroup(firstId);
        cached.getLayerGroup(secondId);
        counter.reset();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        cached.getLayerGroup(firstId);
        DecodeCounter decodes = new DecodeCounter();

        cached.getLayerGroup(secondId);

        // the second group itself
        assertThat(decodes.count()).isOne();
        assertThat(counter.count()).isZero();
    }

    // the filter of the listing reads the members of the groups, and resolves them through the cache of the request
    @Test
    void inMemoryFilterUsesRequestCache() {
        CatalogPlugin cached = requestCachedCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        List<String> listed = listNames(cached, LayerGroupInfo.class, firstMemberNamed("l1"));
        DecodeCounter decodes = new DecodeCounter();

        LayerInfo member = cached.getLayer(layerId);

        assertThat(listed).containsExactly("g1");
        assertThat(member).extracting(LayerInfo::getName).isEqualTo("l1");
        assertThat(decodes.count()).isZero();
    }

    // outside a request, the references are looked up below the cache of the request, and one lookup shares them
    @Test
    void outsideRequestDecodesOnce() {
        List<StyleInfo> styles = addStyles("shared", 10);
        List<LayerInfo> layers = addLayersOnOneStore("shared", styles);
        String sharedId = addGroup("shared", layers, styles).getId();
        CatalogPlugin cached = requestCachedCatalog();
        cached.getLayerGroup(sharedId);
        counter.reset();
        DecodeCounter decodes = new DecodeCounter();

        cached.getLayerGroup(sharedId);

        int distinctObjects = 1 + 2 * layers.size() + 3 + styles.size();
        assertThat(decodes.count()).isEqualTo(distinctObjects);
        assertThat(counter.count()).isZero();
    }

    // past the cap of the cache of the request, the references are looked up below it, and one lookup shares them
    @Test
    void fullCacheDecodesOncePerLookup() {
        List<StyleInfo> styles = addStyles("shared", 10);
        List<LayerInfo> layers = addLayersOnOneStore("shared", styles);
        String sharedId = addGroup("shared", layers, styles).getId();
        CatalogPlugin cached = requestCachedCatalog();
        cached.getLayerGroup(sharedId);
        RequestCacheScope.open(1);
        RequestCache full = RequestCacheScope.current();
        // a lookup finding nothing takes the one entry of the cache
        cached.getWorkspaceByName("missing");
        counter.reset();
        DecodeCounter decodes = new DecodeCounter();

        cached.getLayerGroup(sharedId);

        int distinctObjects = 1 + 2 * layers.size() + 3 + styles.size();
        assertThat(full.rejected()).isPositive();
        assertThat(decodes.count()).isEqualTo(distinctObjects);
        assertThat(counter.count()).isZero();
    }

    @Test
    void secondLookupReturnsSameInstance() {
        CatalogPlugin cached = requestCachedCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);

        LayerInfo first = ModificationProxy.unwrap(cached.getLayer(layerId));
        LayerInfo second = ModificationProxy.unwrap(cached.getLayer(layerId));

        assertThat(second).isSameAs(first);
    }

    // the lookup of the old name was cached before the rename
    @Test
    void requestWriteVisibleInRequest() {
        CatalogPlugin cached = requestCachedCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        cached.getLayerByName("ws:l1");

        renameResource(cached, "ws:l1", "renamed");

        LayerInfo byOldName = cached.getLayerByName("ws:l1");
        LayerInfo byNewName = cached.getLayerByName("ws:renamed");
        assertThat(byOldName).isNull();
        assertThat(byNewName).extracting(LayerInfo::getId).isEqualTo(layerId);
    }

    // outside a request, as for a GWC seed, a listing opens a cache for itself and closes it with its stream
    @Test
    void listingOpensItsOwnScope() {
        CatalogPlugin cached = requestCachedCatalog();
        RequestCache listingCache;

        try (CloseableIterator<LayerInfo> layers = cached.list(LayerInfo.class, Filter.INCLUDE)) {
            layers.next();
            listingCache = RequestCacheScope.current();
            assertThat(listingCache).isNotNull();
            assertThat(listingCache.isDead()).isFalse();
        }

        assertThat(listingCache.isDead()).isTrue();
        assertThat(RequestCacheScope.current()).isNull();
    }

    // the listing does not cache the listed layer: the first lookup reads its row, and decodes the layer with the
    // resource, store, workspace, namespace and style of the row
    @Test
    void listedLayerDecodedOnLookup() {
        CatalogPlugin cached = requestCachedCatalog();

        try (CloseableIterator<LayerInfo> layers = cached.list(LayerInfo.class, Filter.INCLUDE)) {
            LayerInfo listed = ModificationProxy.unwrap(layers.next());
            DecodeCounter decodes = new DecodeCounter();

            LayerInfo found = ModificationProxy.unwrap(cached.getLayer(listed.getId()));
            long decodedByFirstLookup = decodes.count();
            decodes.reset();
            LayerInfo again = ModificationProxy.unwrap(cached.getLayer(listed.getId()));

            assertResolved(found);
            assertThat(decodedByFirstLookup).isEqualTo(6);
            assertThat(again).isSameAs(found);
            assertThat(decodes.count()).isZero();
        }
    }

    @Test
    void listingKeepsRequestScope() {
        CatalogPlugin cached = requestCachedCatalog();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        RequestCache requestCache = RequestCacheScope.current();

        try (CloseableIterator<LayerInfo> layers = cached.list(LayerInfo.class, Filter.INCLUDE)) {
            layers.next();
            assertThat(RequestCacheScope.current()).isSameAs(requestCache);
        }

        assertThat(RequestCacheScope.current()).isSameAs(requestCache);
        assertThat(requestCache.isDead()).isFalse();
    }

    // 1 for the group row, 1 for the members of the group and of its style, 1 for the styles of both
    @Test
    void groupStylesArriveResolved() {
        String styledId = addStyledGroup().getId();
        counter.reset();

        LayerGroupInfo group = ModificationProxy.unwrap(catalog.getLayerGroup(styledId));

        LayerGroupStyle groupStyle = group.getLayerGroupStyles().getFirst();
        assertThat(groupStyle.getLayers()).noneMatch(ProxyUtils::isResolvingProxy);
        assertThat(groupStyle.getLayers()).extracting(PublishedInfo::getName).containsExactly("l2");
        assertThat(groupStyle.getStyles()).noneMatch(ProxyUtils::isResolvingProxy);
        assertThat(groupStyle.getStyles()).extracting(StyleInfo::getName).containsExactly("s2");
        assertThat(group.getStyles()).extracting(StyleInfo::getName).containsExactly("s1");
        assertThat(counter.count()).isEqualTo(3);
    }

    // the nested group shares l1 with its parent: the call loading the members of the parent loads it for both
    @Test
    void sharedMemberLoadsOnce() {
        LayerInfo l1 = catalog.getLayer(layerId);
        LayerGroupInfo child = addGroup("child", List.of(l1));
        String parentId = addGroup("parent", List.of(child, l1)).getId();
        counter.reset();

        LayerGroupInfo parent = ModificationProxy.unwrap(catalog.getLayerGroup(parentId));

        // 1 for the parent row, 1 for its members
        assertThat(counter.count()).isEqualTo(2);
        LayerGroupInfo nested = (LayerGroupInfo) parent.getLayers().get(0);
        assertThat(nested.getLayers().get(0)).isSameAs(parent.getLayers().get(1));
    }

    // the parent lists l1 before the nested group, and the nested group finds the l1 resolved for the parent
    @Test
    void memberBeforeNestedDecodedOnce() {
        LayerInfo l1 = catalog.getLayer(layerId);
        LayerGroupInfo child = addGroup("child", List.of(l1));
        String parentId = addGroup("parent", List.of(l1, child)).getId();
        CatalogPlugin cached = requestCachedCatalog();
        cached.getLayerGroup(parentId);
        counter.reset();
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        DecodeCounter decodes = new DecodeCounter();

        LayerGroupInfo parent = ModificationProxy.unwrap(cached.getLayerGroup(parentId));

        LayerGroupInfo nested = (LayerGroupInfo) parent.getLayers().get(1);
        // the parent, l1 with its resource, store, workspace, namespace and style, the nested group
        assertThat(decodes.count()).isEqualTo(8);
        assertThat(counter.count()).isZero();
        assertThat(nested.getLayers().get(0)).isSameAs(parent.getLayers().get(0));
    }

    // the parent lists l1 after the nested group: the nested group decodes l1 first, and the request keeps that
    // instance
    @Test
    void memberAfterNestedOneInstance() {
        LayerInfo l1 = catalog.getLayer(layerId);
        LayerGroupInfo child = addGroup("child", List.of(l1));
        String parentId = addGroup("parent", List.of(child, l1)).getId();
        CatalogPlugin cached = requestCachedCatalog();
        cached.getLayerGroup(parentId);
        RequestCacheScope.open(REQUEST_CACHE_SIZE);

        LayerGroupInfo parent = ModificationProxy.unwrap(cached.getLayerGroup(parentId));
        LayerInfo kept = ModificationProxy.unwrap(cached.getLayer(layerId));

        LayerGroupInfo nested = (LayerGroupInfo) parent.getLayers().get(0);
        assertThat(nested.getLayers().get(0)).isSameAs(parent.getLayers().get(1));
        assertThat(kept).isSameAs(parent.getLayers().get(1));
    }

    // without the entity cache, a query of members releases its connection before the members are resolved
    @Test
    void nestedGroupHoldsOneConnection() {
        String containerId = addNestedGroups();
        CatalogPlugin uncached = new CatalogPlugin();
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(new JdbcTemplate(counter.dataSource()));
        uncached.setFacade(PgconfigBackendBuilder.requestCaching(facade));
        RequestCacheScope.open(REQUEST_CACHE_SIZE);
        counter.reset();

        LayerGroupInfo container = uncached.getLayerGroup(containerId);

        assertThat(container.getLayers()).hasSize(3);
        assertThat(counter.mostOpenConnections()).isOne();
    }

    // the 3 children share their members and their style: the first child loads them for the others
    @Test
    void nestedGroupLoadsInFourQueries() {
        String containerId = addNestedGroups();
        counter.reset();

        LayerGroupInfo container = catalog.getLayerGroup(containerId);

        // 1 container row + 1 for its 3 children + 1 for the members + 1 for the style of the children
        assertThat(counter.count()).isEqualTo(4);
        LayerGroupInfo first = (LayerGroupInfo) container.getLayers().get(0);
        LayerGroupInfo last = (LayerGroupInfo) container.getLayers().get(2);
        assertThat(first.getStyles()).extracting(this::nameOrNull).containsExactly("s1", null);
        assertThat(last.getLayers()).noneMatch(ProxyUtils::isResolvingProxy);
        assertThat(last.getStyles()).extracting(this::nameOrNull).containsExactly("s1", null);
    }

    // the catalog looks ws:lg up as a group without workspace first, then as lg in ws
    @Test
    void workspaceGroupFoundByPrefixedNameCold() {
        String groupId = addWorkspaceGroup("ws", "lg").getId();
        CatalogPlugin cached = cachedCatalog();

        LayerGroupInfo first = cached.getLayerGroupByName("ws:lg");
        counter.reset();
        LayerGroupInfo second = cached.getLayerGroupByName("ws:lg");

        assertThat(first).extracting(LayerGroupInfo::getId).isEqualTo(groupId);
        assertThat(second).extracting(LayerGroupInfo::getId).isEqualTo(groupId);
        // the lookup without workspace reads its remembered miss, the lookup in ws reads the group
        assertThat(counter.count()).isZero();
    }

    // a:b is also the name key of the group b of the workspace a, and the key of a miss holds no hit
    @Test
    void globalGroupWithColonUncached() {
        LayerInfo l1 = catalog.getLayer(layerId);
        String groupId = addGroup("a:b", l1).getId();
        EntityCache cache = newEntityCache();
        CatalogPlugin cached = cachedCatalog(cache);

        LayerGroupInfo first = cached.getLayerGroupByName("a:b");
        LayerGroupInfo second = cached.getLayerGroupByName("a:b");

        assertThat(first).extracting(LayerGroupInfo::getId).isEqualTo(groupId);
        assertThat(second).extracting(LayerGroupInfo::getId).isEqualTo(groupId);
        assertThat(cache.lookup(CacheKey.name(LAYERGROUP, "a:b"))).isInstanceOf(Lookup.Absent.class);
        assertThat(cache.lookup(CatalogInfoKeys.globalNameMiss(LAYERGROUP, "a:b")))
                .isInstanceOf(Lookup.Absent.class);
    }

    // the add of the group without workspace evicts the miss remembered for its name
    @Test
    void globalGroupNamedLikePrefixedFound() {
        String workspaceGroupId = addWorkspaceGroup("ws", "lg").getId();
        EntityCache cache = newEntityCache();
        CatalogPlugin cached = cachedCatalog(cache);
        ExtendedCatalogFacade cachedFacade = (ExtendedCatalogFacade) cached.getRawFacade();
        cached.getLayerGroupByName("ws:lg");
        Lookup remembered = cache.lookup(CatalogInfoKeys.globalNameMiss(LAYERGROUP, "ws:lg"));

        String globalGroupId = addGlobalGroup(cached, "ws:lg");
        LayerGroupInfo global = cachedFacade.getLayerGroupByName(NO_WORKSPACE, "ws:lg");
        LayerGroupInfo inWorkspace = cached.getLayerGroupByName("ws", "lg");

        assertThat(remembered).isInstanceOf(Lookup.Miss.class);
        assertThat(global).extracting(LayerGroupInfo::getId).isEqualTo(globalGroupId);
        assertThat(inWorkspace).extracting(LayerGroupInfo::getId).isEqualTo(workspaceGroupId);
    }

    // a caller of the facade may look ws:s up without workspace before s in ws
    @Test
    void workspaceStyleFoundByPrefixedNameCold() {
        String styleId = addStyle("ws", "s").getId();
        CatalogPlugin cached = cachedCatalog();
        ExtendedCatalogFacade cold = (ExtendedCatalogFacade) cached.getRawFacade();
        WorkspaceInfo workspace = cold.getWorkspaceByName("ws");

        StyleInfo global = cold.getStyleByName(NO_WORKSPACE, "ws:s");
        StyleInfo inWorkspace = cold.getStyleByName(workspace, "s");

        assertThat(global).isNull();
        assertThat(inWorkspace).extracting(StyleInfo::getId).isEqualTo(styleId);
    }

    /** A second catalog on the same database, with an entity cache. */
    private CatalogPlugin cachedCatalog() {
        return cachedCatalog(newEntityCache());
    }

    /** A second catalog on the same database, with the entity cache. */
    private CatalogPlugin cachedCatalog(EntityCache cache) {
        CatalogPlugin cached = new CatalogPlugin();
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(new JdbcTemplate(counter.dataSource()), cache);
        cached.setFacade(PgconfigBackendBuilder.resolving(facade));
        return cached;
    }

    /**
     * A second catalog on the same database, with an entity cache and the cache of the request, as the backend
     * configuration builds it.
     */
    private CatalogPlugin requestCachedCatalog() {
        CatalogPlugin cached = new CatalogPlugin();
        JdbcTemplate template = new JdbcTemplate(counter.dataSource());
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(template, newEntityCache());
        RequestCachingCatalogFacade requestCaching = PgconfigBackendBuilder.requestCaching(facade);
        cached.setFacade(requestCaching);
        return cached;
    }

    /** Wraps the outbound resolver of the pgconfig chain of the catalog with a counter of its calls. */
    private static AtomicInteger countResolverPasses(CatalogPlugin target) {
        ResolvingCatalogFacadeDecorator resolving = resolvingDecorator(target.getRawFacade());
        UnaryOperator<CatalogInfo> resolver = resolving.getOutboundResolver();
        AtomicInteger passes = new AtomicInteger();
        resolving.setOutboundResolver(info -> {
            passes.incrementAndGet();
            return resolver.apply(info);
        });
        return passes;
    }

    /** The resolving decorator of a pgconfig chain, above or below the cache of the request. */
    private static ResolvingCatalogFacadeDecorator resolvingDecorator(CatalogFacade chain) {
        CatalogFacade facade = chain;
        while (!(facade instanceof ResolvingCatalogFacadeDecorator)) {
            ForwardingCatalogFacade forwarding = (ForwardingCatalogFacade) facade;
            facade = forwarding.getSubject();
        }
        return (ResolvingCatalogFacadeDecorator) facade;
    }

    /** Renames a resource through the catalog: its layer takes the new name. */
    private void renameResource(Catalog target, String prefixedName, String newName) {
        FeatureTypeInfo resource = target.getResourceByName(prefixedName, FeatureTypeInfo.class);
        resource.setName(newName);
        target.save(resource);
    }

    private static EntityCache newEntityCache() {
        EntityCache cache = new EntityCache("gs-catalog", 1024 * 1024, Duration.ofSeconds(30), new NoopCodec());
        cache.setInitialized(true);
        return cache;
    }

    /** Checks the raw layer: a proxy left in it would be resolved later by its {@link ModificationProxy}. */
    private void assertResolved(LayerInfo layer) {
        LayerInfo raw = ModificationProxy.unwrap(layer);
        ResourceInfo resource = raw.getResource();

        assertThat(ProxyUtils.isResolvingProxy(resource)).as("resource").isFalse();
        assertThat(ProxyUtils.isResolvingProxy(resource.getStore())).as("store").isFalse();
        assertThat(ProxyUtils.isResolvingProxy(resource.getStore().getWorkspace()))
                .as("workspace")
                .isFalse();
        assertThat(ProxyUtils.isResolvingProxy(raw.getDefaultStyle()))
                .as("default style")
                .isFalse();
    }

    private <T extends CatalogInfo> List<T> findAll(List<String> ids, Class<T> type) {
        return findAll(facade, ids, type);
    }

    private static <T extends CatalogInfo> List<T> findAll(
            ExtendedCatalogFacade source, List<String> ids, Class<T> type) {
        try (Stream<T> found = source.findAllById(ids, type)) {
            return found.toList();
        }
    }

    /** Reads the layers through the chain of a caller, with an empty cache. */
    private List<LayerInfo> findAllCold(EntityCache cache, List<String> ids) {
        PgconfigCatalogFacade cold = new PgconfigCatalogFacade(new JdbcTemplate(counter.dataSource()), cache);
        counter.reset();
        return findAll(PgconfigBackendBuilder.resolving(cold), ids, LayerInfo.class);
    }

    private static long countCachedLayers(EntityCache cache, List<String> ids) {
        return ids.stream()
                .filter(id -> cache.contains(CacheKey.entity(LAYER, id)))
                .count();
    }

    private String nameOrNull(StyleInfo style) {
        if (style == null) {
            return null;
        }
        return style.getName();
    }

    private String addNestedGroups() {
        LayerInfo l1 = catalog.getLayer(layerId);
        LayerInfo l2 = catalog.getLayer(layer2Id);
        StyleInfo s1 = catalog.getStyle(s1Id);
        LayerGroupInfo container = catalog.getFactory().createLayerGroup();
        container.setName("container");
        container.setMode(LayerGroupInfo.Mode.CONTAINER);
        for (String name : List.of("n1", "n2", "n3")) {
            LayerGroupInfo child = catalog.getFactory().createLayerGroup();
            child.setName(name);
            child.getLayers().addAll(List.of(l1, l2));
            child.getStyles().addAll(Arrays.asList(s1, null));
            catalog.add(child);
            container.getLayers().add(catalog.getLayerGroupByName(name));
            container.getStyles().add(null);
        }
        catalog.add(container);
        return catalog.getLayerGroupByName("container").getId();
    }

    private void createWorkspace(String name) {
        createWorkspace(name, false);
    }

    private void createWorkspace(String name, boolean isolated) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo workspace = factory.createWorkspace();
        workspace.setName(name);
        workspace.setIsolated(isolated);
        catalog.add(workspace);

        NamespaceInfo namespace = factory.createNamespace();
        namespace.setPrefix(name);
        namespace.setURI("http://%s.example".formatted(name));
        namespace.setIsolated(isolated);
        catalog.add(namespace);
    }

    /** Adds the layer {@code iso:roads} in the isolated workspace {@code iso}. */
    private LayerInfo addIsolatedLayer() {
        createWorkspace("iso", true);
        return addLayer("iso", "roads");
    }

    private StyleInfo addStyle(String name) {
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(name);
        style.setFilename("%s.sld".formatted(name));
        catalog.add(style);
        return catalog.getStyleByName(name);
    }

    private LayerInfo addLayer(String name) {
        return addLayer("ws", name);
    }

    private LayerInfo addLayer(String workspaceName, String name) {
        return addLayer(workspaceName, name, catalog.getStyleByName("s1"), List.of());
    }

    private LayerInfo addLayer(String workspaceName, String name, StyleInfo defaultStyle, List<StyleInfo> styles) {
        DataStoreInfo store = addStore(workspaceName, "%s-store".formatted(name));
        FeatureTypeInfo ft = addFeatureType(store, name);

        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(ft);
        layer.setDefaultStyle(defaultStyle);
        layer.getStyles().addAll(styles);
        catalog.add(layer);
        return catalog.getLayerByName("%s:%s".formatted(workspaceName, name));
    }

    /**
     * Adds a layer of a feature type in the namespace {@code prefix}, on a store of the workspace {@code workspace}.
     */
    private LayerInfo addLayerInNamespace(String workspaceName, String prefix, String name) {
        DataStoreInfo store = addStore(workspaceName, "%s-%s-store".formatted(prefix, name));
        FeatureTypeInfo ft = addFeatureType(store, name, prefix);
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(ft);
        layer.setDefaultStyle(catalog.getStyleByName("s1"));
        catalog.add(layer);
        return catalog.getLayers(ft).get(0);
    }

    private StyleInfo addStyle(String workspaceName, String name) {
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(name);
        style.setFilename("%s.sld".formatted(name));
        style.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        catalog.add(style);
        return catalog.getStyleByName(workspaceName, name);
    }

    private List<StyleInfo> addStyles(String prefix, int count) {
        List<StyleInfo> styles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            styles.add(addStyle("%s-style-%d".formatted(prefix, i)));
        }
        return styles;
    }

    /** Adds a layer of ws per style, on one store. A layer has its own style by default, and the list as alternates. */
    private List<LayerInfo> addLayersOnOneStore(String prefix, List<StyleInfo> styles) {
        DataStoreInfo store = addStore("ws", "%s-store".formatted(prefix));
        List<LayerInfo> layers = new ArrayList<>();
        for (int i = 0; i < styles.size(); i++) {
            String name = "%s-layer-%d".formatted(prefix, i);
            LayerInfo layer = catalog.getFactory().createLayer();
            layer.setResource(addFeatureType(store, name));
            layer.setDefaultStyle(styles.get(i));
            layer.getStyles().addAll(styles);
            catalog.add(layer);
            layers.add(catalog.getLayerByName("ws:" + name));
        }
        return layers;
    }

    private DataStoreInfo addStore(String workspaceName, String name) {
        DataStoreInfo store = catalog.getFactory().createDataStore();
        store.setName(name);
        store.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        store.setEnabled(true);
        catalog.add(store);
        return store;
    }

    /** Adds a feature type in the namespace of the workspace of the store. */
    private FeatureTypeInfo addFeatureType(DataStoreInfo store, String name) {
        return addFeatureType(store, name, store.getWorkspace().getName());
    }

    private FeatureTypeInfo addFeatureType(DataStoreInfo store, String name, String prefix) {
        FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
        ft.setName(name);
        ft.setNativeName(name);
        ft.setStore(store);
        ft.setNamespace(catalog.getNamespaceByPrefix(prefix));
        ft.setEnabled(true);
        catalog.add(ft);
        return ft;
    }

    private LayerGroupInfo addGroup(String name, LayerInfo member) {
        return addGroup(name, member, null);
    }

    private LayerGroupInfo addGroup(String name, LayerInfo member, @Nullable StyleInfo style) {
        List<StyleInfo> styles = new ArrayList<>();
        styles.add(style);
        return addGroup(name, List.of(member), styles);
    }

    /** Adds a group of the members, drawn with their default styles. */
    private LayerGroupInfo addGroup(String name, List<? extends PublishedInfo> members) {
        List<StyleInfo> defaultStyles = Collections.nCopies(members.size(), null);
        return addGroup(name, members, defaultStyles);
    }

    private LayerGroupInfo addGroup(String name, List<? extends PublishedInfo> members, List<StyleInfo> styles) {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.getLayers().addAll(members);
        group.getStyles().addAll(styles);
        catalog.add(group);
        return catalog.getLayerGroupByName(name);
    }

    /** Adds a group of l1 in the workspace, drawn with the default style of l1. */
    private LayerGroupInfo addWorkspaceGroup(String workspaceName, String name) {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        group.getLayers().add(catalog.getLayer(layerId));
        group.getStyles().add(null);
        catalog.add(group);
        return catalog.getLayerGroupByName(workspaceName, name);
    }

    /** Adds a group of l1 without workspace through the catalog, and returns its id. */
    private String addGlobalGroup(Catalog target, String name) {
        LayerGroupInfo group = target.getFactory().createLayerGroup();
        group.setName(name);
        group.getLayers().add(target.getLayer(layerId));
        group.getStyles().add(null);
        target.add(group);
        return group.getId();
    }

    /** Adds a group drawing l1 with s1, with a layer group style drawing l2 with s2. */
    private LayerGroupInfo addStyledGroup() {
        StyleInfo styleName = new StyleInfoImpl(null);
        styleName.setName("alternative");
        LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
        groupStyle.setName(styleName);
        groupStyle.getLayers().add(catalog.getLayer(layer2Id));
        groupStyle.getStyles().add(catalog.getStyle(s2Id));

        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName("styled-group");
        group.getLayers().add(catalog.getLayer(layerId));
        group.getStyles().add(catalog.getStyle(s1Id));
        group.getLayerGroupStyles().add(groupStyle);
        catalog.add(group);
        return catalog.getLayerGroupByName("styled-group");
    }

    private Request capabilitiesRequest() {
        Request request = new Request();
        request.setRequest("GetCapabilities");
        return request;
    }

    private <T extends PublishedInfo> List<String> listNames(Catalog source, Class<T> type, Filter filter) {
        List<String> names = new ArrayList<>();
        try (CloseableIterator<T> found = source.list(type, filter)) {
            found.forEachRemaining(published -> names.add(published.getName()));
        }
        return names;
    }

    /** Lists the layers without their {@link ModificationProxy}, to read the objects returned by the facade. */
    private List<LayerInfo> listUnwrapped(Catalog source) {
        List<LayerInfo> layers = new ArrayList<>();
        try (CloseableIterator<LayerInfo> found = source.list(LayerInfo.class, Filter.INCLUDE)) {
            found.forEachRemaining(layer -> layers.add(ModificationProxy.unwrap(layer)));
        }
        return layers;
    }

    private Filter layersOfWorkspace(String workspaceName) {
        return workspaceNameIs(layer -> layer.getResource().getStore().getWorkspace(), workspaceName);
    }

    private Filter layersWithStyleOf(String workspaceName) {
        return workspaceNameIs(layer -> layer.getDefaultStyle().getWorkspace(), workspaceName);
    }

    /** Compares the name of the first member of a group, in memory: SQL cannot run a function written in Java. */
    private Filter firstMemberNamed(String memberName) {
        Expression nameOfFirstMember = new InternalVolatileFunction() {
            @Override
            public Object evaluate(Object info) {
                LayerGroupInfo group = (LayerGroupInfo) info;
                PublishedInfo first = group.getLayers().getFirst();
                return first.getName();
            }
        };
        FilterFactory factory = Predicates.factory;
        return factory.equals(nameOfFirstMember, factory.literal(memberName));
    }

    /** Compares the name of a workspace read from a layer, in memory: SQL cannot run a function written in Java. */
    private Filter workspaceNameIs(Function<LayerInfo, WorkspaceInfo> workspaceOf, String workspaceName) {
        Expression nameOfWorkspace = new InternalVolatileFunction() {
            @Override
            public Object evaluate(Object info) {
                WorkspaceInfo workspace = workspaceOf.apply((LayerInfo) info);
                return workspace == null ? null : workspace.getName();
            }
        };
        FilterFactory factory = Predicates.factory;
        return factory.equals(nameOfWorkspace, factory.literal(workspaceName));
    }

    /** Asks the set itself: AssertJ iterates, and would find a style kept in the bucket of its former hash. */
    private void assertStylesFindDefaultStyle(LayerInfo layer) {
        Set<StyleInfo> styles = layer.getStyles();
        assertThat(styles.contains(layer.getDefaultStyle())).as(layer.getName()).isTrue();
    }
}
