/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.NamespaceWorkspaceConsistencyListener;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.catalog.cache.CachingCatalogFacade;
import org.geotools.api.filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies that renaming a workspace (or its paired namespace) does not leave {@code CachingCatalogFacade} returning
 * stale prefixed names through any of the catalog-level read paths.
 *
 * <p>Background: production users hit this when {@code geoserver.catalog.caching.enabled=true} - after renaming a
 * workspace in the Wicket UI, the {@code NewCachedLayerPage} ("Tile Layers -> Add a new cached layer") kept showing the
 * old prefixed layer names until the user clicked "Server Status -> Resource Cache -> Clear". The page exercises
 * {@code Catalog.getLayerByName}, which internally calls {@code getResourceByName} then {@code getLayers(resource)} -
 * the second call hits the {@code "layers@<resourceId>"} {@code List<LayerInfo>} cache that the cascade-evict pass did
 * not visit (it only iterated {@link org.geoserver.catalog.CatalogInfo} cache values).
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigCachingWorkspaceRenameTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private static final String CACHE_NAME = "gs-catalog";

    private CatalogPlugin catalog;
    private CachingCatalogFacade cachingFacade;

    @BeforeEach
    void setUp() {
        catalog = newCachingCatalog();
    }

    @Test
    void renamingWorkspaceUpdatesCachedLayerById() {
        Fixture fx = newFixture();
        primeCaches(fx);

        renameWorkspace(fx.workspace, "ws_renamed");

        LayerInfo layer = catalog.getLayer(fx.layerId);
        assertThat(layer.getResource().prefixedName())
                .as("getLayer(id).resource.prefixedName after rename")
                .isEqualTo("ws_renamed:ft");
    }

    @Test
    void renamingWorkspaceUpdatesCachedLayerByName() {
        Fixture fx = newFixture();
        primeCaches(fx);

        renameWorkspace(fx.workspace, "ws_renamed");

        assertThat(catalog.getLayerByName("ws1:ft"))
                .as("getLayerByName(old prefix) after rename")
                .isNull();

        LayerInfo byNewName = catalog.getLayerByName("ws_renamed:ft");
        assertThat(byNewName).as("getLayerByName(new prefix) after rename").isNotNull();
        assertThat(byNewName.getResource().prefixedName())
                .as("getLayerByName(new).resource.prefixedName")
                .isEqualTo("ws_renamed:ft");
    }

    @Test
    void renamingWorkspaceUpdatesCachedLayerByNameWithoutPriming() {
        // Reproduction of the production symptom: even without an explicit cache prime,
        // creating the fixture itself populates the "layers@<resourceId>" cache (via the
        // catalog.getLayerByName("ws1:ft") at the end of fixture setup). Renaming the workspace
        // must evict that list entry so the next prefixed-name lookup is fresh.
        Fixture fx = newFixture();
        renameWorkspace(fx.workspace, "ws_renamed");

        LayerInfo byNewName = catalog.getLayerByName("ws_renamed:ft");
        assertThat(byNewName).as("getLayerByName(new prefix) after rename").isNotNull();
        assertThat(byNewName.getResource().prefixedName())
                .as("getLayerByName(new).resource.prefixedName")
                .isEqualTo("ws_renamed:ft");
    }

    @Test
    void renamingWorkspaceUpdatesCachedNamespace() {
        Fixture fx = newFixture();
        primeCaches(fx);

        renameWorkspace(fx.workspace, "ws_renamed");

        assertThat(catalog.getNamespace(fx.namespaceId).getPrefix())
                .as("getNamespace(id).prefix after rename")
                .isEqualTo("ws_renamed");

        assertThat(catalog.getNamespaceByPrefix("ws1"))
                .as("getNamespaceByPrefix(old) after rename")
                .isNull();
        assertThat(catalog.getNamespaceByPrefix("ws_renamed"))
                .as("getNamespaceByPrefix(new) after rename")
                .isNotNull();
    }

    @Test
    void renamingWorkspaceUpdatesListedLayers() {
        Fixture fx = newFixture();
        primeCaches(fx);

        renameWorkspace(fx.workspace, "ws_renamed");

        List<String> prefixedNames = listLayerPrefixedNames();
        assertThat(prefixedNames)
                .as("catalog.list(LayerInfo).map(prefixedName)")
                .containsExactly("ws_renamed:ft");
    }

    private List<String> listLayerPrefixedNames() {
        List<String> names = new ArrayList<>();
        try (var it = catalog.list(LayerInfo.class, Filter.INCLUDE)) {
            while (it.hasNext()) {
                names.add(it.next().getResource().prefixedName());
            }
        }
        return names;
    }

    private CatalogPlugin newCachingCatalog() {
        CatalogPlugin plugin = new CatalogPlugin();
        PgconfigCatalogFacade pgconfigFacade =
                (PgconfigCatalogFacade) new PgconfigBackendBuilder(db.getDataSource()).createCatalogFacade();
        cachingFacade = new CachingCatalogFacade(pgconfigFacade, newCache());
        plugin.setFacade(cachingFacade);
        new NamespaceWorkspaceConsistencyListener(plugin);
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

        WorkspaceInfo persistedWs = catalog.getWorkspaceByName("ws1");
        NamespaceInfo persistedNs = catalog.getNamespaceByPrefix("ws1");
        LayerInfo persistedLayer = catalog.getLayerByName("ws1:ft");
        return new Fixture(persistedWs, persistedNs.getId(), persistedLayer.getId(), ft.getId());
    }

    private void primeCaches(Fixture fx) {
        // Force CachingCatalogFacade to populate entries on every key path that production exercises:
        // by id, by prefixed name, by namespace prefix, and the layers-by-resource list cache.
        catalog.getLayer(fx.layerId);
        catalog.getLayerByName("ws1:ft");
        catalog.getNamespace(fx.namespaceId);
        catalog.getNamespaceByPrefix("ws1");
        catalog.getLayers(catalog.getResource(fx.resourceId, FeatureTypeInfo.class));
    }

    private void renameWorkspace(WorkspaceInfo ws, String newName) {
        WorkspaceInfo fresh = catalog.getWorkspace(ws.getId());
        fresh.setName(newName);
        catalog.save(fresh);
    }

    private record Fixture(WorkspaceInfo workspace, String namespaceId, String layerId, String resourceId) {}
}
