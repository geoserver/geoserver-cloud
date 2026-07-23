/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.catalog.cache.CachingCatalogFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the DAO-layer invariant that materialized catalog object graphs contain no {@link ModificationProxy}
 * instances: everything below {@link CatalogPlugin} trades in raw implementations, and proxy wrapping is exclusively a
 * {@code CatalogPlugin} outbound concern applied per retrieval.
 *
 * <p>Background: with {@code geoserver.catalog.caching.enabled=true}, {@link CachingCatalogFacade} shares one
 * materialized graph across all request threads. When that graph embeds live {@code ModificationProxy} instances
 * (attribute {@code featureType} back-references, store/namespace/style references, layer group members), the proxies'
 * non-transient, unsynchronized {@code properties}/{@code oldCollectionValues} maps are both written by reader threads
 * (collection getters and setters intercepted by {@code ModificationProxy.invoke}) and included in the Java
 * serialization streams produced by {@code ModificationProxyCloner.cloneSerializable} when a collection getter is
 * deep-cloned (e.g. {@code FeatureTypeInfo.getAttributes()}). A write landing during another thread's
 * {@code ObjectOutputStream} pass corrupts the stream and surfaces as {@code java.io.OptionalDataException} wrapped in
 * {@code RuntimeException("Error cloning serializable object")}, as reported for WMS GetCapabilities on large
 * workspaces with layer groups and data security rules.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigModificationProxyLeakTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private static final String CACHE_NAME = "gs-catalog";

    private CatalogPlugin catalog;
    private Cache cache;

    @BeforeEach
    void setUp() {
        catalog = newCachingCatalog();
    }

    @Test
    void featureTypeAttributeBackReferenceIsNotAProxy() {
        Fixture fx = newFixture();

        FeatureTypeInfo ft = catalog.getResource(fx.resourceId, FeatureTypeInfo.class);
        FeatureTypeInfo raw = ModificationProxy.unwrap(ft);

        List<AttributeTypeInfo> attributes = raw.getAttributes();
        assertThat(attributes).isNotEmpty();
        for (AttributeTypeInfo att : attributes) {
            FeatureTypeInfo backRef = att.getFeatureType();
            assertNotAModificationProxy("attribute '%s' featureType back-reference".formatted(att.getName()), backRef);
            assertThat(backRef)
                    .as("attribute back-reference points to the raw feature type")
                    .isSameAs(raw);
        }
    }

    @Test
    void resourceReferencesAreNotProxies() {
        Fixture fx = newFixture();

        FeatureTypeInfo raw = ModificationProxy.unwrap(catalog.getResource(fx.resourceId, FeatureTypeInfo.class));

        assertNotAModificationProxy("resource.store", raw.getStore());
        assertNotAModificationProxy("resource.namespace", raw.getNamespace());
        assertNotAModificationProxy(
                "resource.store.workspace",
                ModificationProxy.unwrap(raw.getStore()).getWorkspace());
    }

    @Test
    void layerReferencesAreNotProxies() {
        Fixture fx = newFixture();

        LayerInfo raw = ModificationProxy.unwrap(catalog.getLayer(fx.layerId));

        assertNotAModificationProxy("layer.resource", raw.getResource());
        assertNotAModificationProxy("layer.defaultStyle", raw.getDefaultStyle());
    }

    @Test
    void layerGroupMembersAreNotProxies() {
        Fixture fx = newFixture();

        LayerGroupInfo raw = ModificationProxy.unwrap(catalog.getLayerGroup(fx.layerGroupId));

        assertThat(raw.getLayers()).isNotEmpty();
        for (PublishedInfo member : raw.getLayers()) {
            assertNotAModificationProxy("layer group member '%s'".formatted(member.getName()), member);
        }
        for (StyleInfo style : raw.getStyles()) {
            if (style != null) {
                assertNotAModificationProxy("layer group style '%s'".formatted(style.getName()), style);
            }
        }
    }

    /**
     * Reproduces the reported failure: concurrent deep-cloning of {@code getAttributes()} (which Java-serializes each
     * attribute and, through the {@code featureType} back-reference, whatever object graph it reaches) while other
     * threads exercise intercepted getters/setters against the same shared graph.
     *
     * <p>Each cycle evicts the cache entry to force a fresh materialization: freshly embedded proxies have empty state
     * maps, and the racing first-touch writes are what corrupt a concurrent {@code ObjectOutputStream} pass.
     */
    @Test
    void concurrentAttributeCloningDoesNotCorruptSerialization() throws Exception {
        Fixture fx = newFixture();
        final int cycles = 500;
        final int readers = 2;
        final int writers = 2;
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(readers + writers);
        try {
            for (int cycle = 0; cycle < cycles && failures.isEmpty(); cycle++) {
                cache.clear();
                CyclicBarrier barrier = new CyclicBarrier(readers + writers);
                List<Future<?>> tasks = new CopyOnWriteArrayList<>();
                for (int r = 0; r < readers; r++) {
                    tasks.add(executor.submit(() -> {
                        await(barrier);
                        cloneAttributesThroughProxy(fx, failures);
                    }));
                }
                for (int w = 0; w < writers; w++) {
                    final int salt = w;
                    tasks.add(executor.submit(() -> {
                        await(barrier);
                        touchSharedGraph(fx, salt, failures);
                    }));
                }
                for (Future<?> task : tasks) {
                    task.get();
                }
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures)
                .as("no serialization corruption expected; got: %s"
                        .formatted(failures.stream().map(String::valueOf).toList()))
                .isEmpty();
    }

    private void cloneAttributesThroughProxy(Fixture fx, List<Throwable> failures) {
        try {
            FeatureTypeInfo ft = catalog.getResource(fx.resourceId, FeatureTypeInfo.class);
            // collection getter through the proxy: deep clone, Java-serializing each attribute
            ft.getAttributes();
        } catch (RuntimeException e) {
            failures.add(e);
        }
    }

    /**
     * Emulates concurrent request traffic touching the shared graph: intercepted getters and setters whose state lands
     * in whatever object {@code att.getFeatureType()} exposes. Against an embedded shared proxy these are
     * unsynchronized map writes; against a raw back-reference they are plain field reads/writes.
     */
    private void touchSharedGraph(Fixture fx, int salt, List<Throwable> failures) {
        try {
            FeatureTypeInfo raw = ModificationProxy.unwrap(catalog.getResource(fx.resourceId, FeatureTypeInfo.class));
            List<AttributeTypeInfo> attributes = raw.getAttributes();
            if (attributes.isEmpty()) {
                return;
            }
            FeatureTypeInfo backRef = attributes.get(0).getFeatureType();
            if (salt % 2 == 0) {
                backRef.setTitle("title");
                backRef.setAbstract("abstract");
                backRef.setDescription("description");
                backRef.setNativeName("ft");
                backRef.setSRS("EPSG:4326");
                backRef.setCqlFilter(null);
                backRef.setMaxFeatures(1000);
                backRef.setNumDecimals(8);
                backRef.setEnabled(true);
                backRef.setAdvertised(true);
            } else {
                backRef.getKeywords();
                backRef.getAlias();
                backRef.getResponseSRS();
                backRef.getMetadata();
                backRef.getMetadataLinks();
                backRef.getDataLinks();
            }
        } catch (RuntimeException e) {
            failures.add(e);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void assertNotAModificationProxy(String role, Object info) {
        assertThat(info).as(role).isNotNull();
        assertThat(ModificationProxy.handler(info))
                .as("%s must not be a ModificationProxy: the DAO layer trades in raw objects only".formatted(role))
                .isNull();
    }

    private CatalogPlugin newCachingCatalog() {
        CatalogPlugin plugin = new CatalogPlugin();
        PgconfigCatalogFacade pgconfigFacade =
                (PgconfigCatalogFacade) new PgconfigBackendBuilder(db.getDataSource()).createCatalogFacade();
        cache = newCache();
        CachingCatalogFacade cachingFacade = new CachingCatalogFacade(pgconfigFacade, cache);
        plugin.setFacade(cachingFacade);
        return plugin;
    }

    private static Cache newCache() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_NAME);
        return Objects.requireNonNull(manager.getCache(CACHE_NAME));
    }

    private Fixture newFixture() {
        CatalogFactory factory = catalog.getFactory();

        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName("ws1");
        catalog.add(ws);

        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix("ws1");
        ns.setURI("http://ws1.example");
        catalog.add(ns);

        DataStoreInfo store = factory.createDataStore();
        store.setName("ds");
        store.setWorkspace(ws);
        store.setEnabled(true);
        catalog.add(store);

        FeatureTypeInfo ft = factory.createFeatureType();
        ft.setName("ft");
        ft.setNativeName("ft");
        ft.setStore(store);
        ft.setNamespace(ns);
        ft.setEnabled(true);
        ft.getAttributes().add(attribute(factory, "geom", org.locationtech.jts.geom.Geometry.class));
        ft.getAttributes().add(attribute(factory, "name", String.class));
        catalog.add(ft);

        StyleInfo style = factory.createStyle();
        style.setName("default-style");
        style.setFilename("default-style.sld");
        catalog.add(style);

        LayerInfo layer = factory.createLayer();
        layer.setResource(catalog.getResource(ft.getId(), FeatureTypeInfo.class));
        layer.setDefaultStyle(catalog.getStyleByName("default-style"));
        layer.setEnabled(true);
        catalog.add(layer);

        LayerGroupInfo group = factory.createLayerGroup();
        group.setName("group");
        group.getLayers().add(catalog.getLayer(layer.getId()));
        group.getStyles().add(null);
        catalog.add(group);

        return new Fixture(ft.getId(), layer.getId(), group.getId());
    }

    private AttributeTypeInfo attribute(CatalogFactory factory, String name, Class<?> binding) {
        AttributeTypeInfo att = factory.createAttribute();
        att.setName(name);
        att.setBinding(binding);
        att.setMinOccurs(0);
        att.setMaxOccurs(1);
        att.setNillable(true);
        return att;
    }

    private record Fixture(String resourceId, String layerId, String layerGroupId) {}
}
