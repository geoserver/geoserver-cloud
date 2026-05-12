/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies that moving a {@code StoreInfo} from one workspace to another correctly cascades the
 * namespace change to every {@code ResourceInfo} belonging to that store. Unlike the standard
 * conformance tests, these checks read the {@code resourceinfo} table directly via JDBC so they
 * catch inconsistencies between the JSON {@code info} column and the denormalized {@code namespace}
 * FK column.
 *
 * <p>Background: in production users reported that after moving a data store between workspaces,
 * the resources kept their old namespace. The catalog-level read path was returning the resources
 * via a JOIN through the FK column, masking partial cascade failures. These tests query the
 * underlying table so any cascade gap surfaces immediately.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigStoreNamespaceCascadeTest {

    @Container
    static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    private CatalogPlugin catalog;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        container.setUp();
        jdbc = container.getTemplate();
        catalog = new PgconfigBackendBuilder(container.getDataSource()).createCatalog();
    }

    @AfterEach
    void tearDown() {
        container.tearDown();
    }

    @Test
    void movingStoreUpdatesResourceinfoNamespaceFkColumn() {
        Fixture fixture = newFixtureWithResources(3);

        moveStore(fixture.store, fixture.targetWorkspace);

        assertResourceinfoFkPointsTo(fixture.resourceIds, fixture.targetNamespace);
    }

    @Test
    void movingStoreUpdatesResourceinfoInfoJson() {
        Fixture fixture = newFixtureWithResources(3);

        moveStore(fixture.store, fixture.targetWorkspace);

        assertResourceinfoJsonNamespaceIs(fixture.resourceIds, fixture.targetNamespace);
    }

    @Test
    void movingStoreWithSingleResourceCascades() {
        Fixture fixture = newFixtureWithResources(1);

        moveStore(fixture.store, fixture.targetWorkspace);

        assertResourceinfoFkPointsTo(fixture.resourceIds, fixture.targetNamespace);
        assertResourceinfoJsonNamespaceIs(fixture.resourceIds, fixture.targetNamespace);
    }

    @Test
    void movingStoreWithManyResourcesCascades() {
        Fixture fixture = newFixtureWithResources(10);

        moveStore(fixture.store, fixture.targetWorkspace);

        assertResourceinfoFkPointsTo(fixture.resourceIds, fixture.targetNamespace);
        assertResourceinfoJsonNamespaceIs(fixture.resourceIds, fixture.targetNamespace);
    }

    @Test
    void movingStoreToOriginalWorkspaceIsNoOp() {
        Fixture fixture = newFixtureWithResources(2);

        moveStore(fixture.store, fixture.targetWorkspace);
        moveStore(reloadStore(fixture.store), fixture.sourceWorkspace);

        assertResourceinfoFkPointsTo(fixture.resourceIds, fixture.sourceNamespace);
        assertResourceinfoJsonNamespaceIs(fixture.resourceIds, fixture.sourceNamespace);
    }

    @Test
    void triggerIsInstalled() {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM pg_trigger
                WHERE tgname = 'storeinfo_cascade_workspace_to_resource_namespace'
                """,
                Integer.class);
        assertThat(count)
                .as("cascade trigger should be installed by V3_0_1 migration")
                .isEqualTo(1);
    }

    @Test
    void dbTriggerCascadesWhenStoreWorkspaceChangedDirectly() {
        Fixture fixture = newFixtureWithResources(3);

        // Simulates a non-catalog write path (REST import, manual SQL, restore tool) that
        // updates storeinfo.info directly. The existing BEFORE trigger derives the workspace
        // FK column from the new JSON, and our AFTER trigger then propagates to the resources.
        int updated = jdbc.update(
                "UPDATE storeinfo SET info = jsonb_set(info, '{workspace}', to_jsonb(?::text)) WHERE id = ?",
                fixture.targetWorkspace.getId(),
                fixture.store.getId());
        assertThat(updated).isOne();

        assertResourceinfoFkPointsTo(fixture.resourceIds, fixture.targetNamespace);
        assertResourceinfoJsonNamespaceIs(fixture.resourceIds, fixture.targetNamespace);
    }

    @Test
    void dbTriggerRaisesWhenWorkspaceHasNoMatchingNamespace() {
        Fixture fixture = newFixtureWithResources(1);

        WorkspaceInfo orphanWs = catalog.getFactory().createWorkspace();
        orphanWs.setName("orphan");
        catalog.add(orphanWs);
        WorkspaceInfo persistedOrphan = catalog.getWorkspaceByName("orphan");

        assertThat(persistedOrphan).isNotNull();

        Throwable thrown = catchThrowable(() -> jdbc.update(
                "UPDATE storeinfo SET info = jsonb_set(info, '{workspace}', to_jsonb(?::text)) WHERE id = ?",
                persistedOrphan.getId(),
                fixture.store.getId()));

        assertThat(thrown).isNotNull().hasMessageContaining("No namespace matches workspace orphan");
    }

    @Test
    void renamingWorkspaceLeavesGlobalLayerGroupAlone() {
        // A global layer group (workspace = NULL) referencing a layer/group from the renamed
        // workspace must keep its own row stable in publishedinfos_mat (its "workspace.name" is
        // NULL and its "prefixedName" has no workspace prefix). V3_0_2 trigger filters global
        // groups out by WHERE workspace = NEW.id, which is the desired no-op behaviour.
        Fixture fixture = newFixtureWithResources(1);
        WorkspaceInfo ws = fixture.sourceWorkspace;

        CatalogFactory factory = catalog.getFactory();
        StyleInfo style = factory.createStyle();
        style.setName("style1");
        style.setFilename("style1.sld");
        catalog.add(style);
        LayerInfo layer = factory.createLayer();
        layer.setResource(catalog.getResource(fixture.resourceIds.getFirst(), FeatureTypeInfo.class));
        layer.setDefaultStyle(catalog.getStyleByName("style1"));
        layer.setEnabled(true);
        catalog.add(layer);

        // Global group (workspace = NULL) wrapping the layer from `ws`. styles has a NULL entry
        // (the "use the layer's default style" placeholder), exercising NULL-in-array handling.
        LayerGroupInfo globalGroup = factory.createLayerGroup();
        globalGroup.setName("globalg");
        globalGroup.setMode(LayerGroupInfo.Mode.SINGLE);
        globalGroup.setWorkspace(null);
        globalGroup.getLayers().add(catalog.getLayer(layer.getId()));
        globalGroup.getStyles().add(null);
        catalog.add(globalGroup);

        // Before rename
        String globalPrefixedNameBefore = jdbc.queryForObject(
                "SELECT \"prefixedName\" FROM publishedinfos_mat WHERE id = ?", String.class, globalGroup.getId());
        assertThat(globalPrefixedNameBefore).isEqualTo("globalg");
        assertWorkspaceNameInMat(layer.getId(), ws.getName());

        jdbc.update(
                "UPDATE workspaceinfo SET info = jsonb_set(info, '{name}', to_jsonb(?::text)) WHERE id = ?",
                "renamed",
                ws.getId());

        // Layer row refreshed; global group row unchanged
        assertWorkspaceNameInMat(layer.getId(), "renamed");
        String globalPrefixedNameAfter = jdbc.queryForObject(
                "SELECT \"prefixedName\" FROM publishedinfos_mat WHERE id = ?", String.class, globalGroup.getId());
        assertThat(globalPrefixedNameAfter).isEqualTo("globalg");
        String globalWsName = jdbc.queryForObject(
                "SELECT \"workspace.name\" FROM publishedinfos_mat WHERE id = ?", String.class, globalGroup.getId());
        assertThat(globalWsName).isNull();
    }

    @Test
    void renamingWorkspaceRefreshesNestedLayerGroupHolders() {
        // A layer group in workspace A whose layers array contains another layer group (also in
        // workspace A) is just a row with layers.id = [nested-group-id]. Renaming workspace A
        // refreshes the outer group via the V3_0_2 trigger; the layers.id array (by ID) doesn't
        // need to change.
        Fixture fixture = newFixtureWithResources(1);
        WorkspaceInfo ws = fixture.sourceWorkspace;

        CatalogFactory factory = catalog.getFactory();
        StyleInfo style = factory.createStyle();
        style.setName("style1");
        style.setFilename("style1.sld");
        catalog.add(style);
        LayerInfo layer = factory.createLayer();
        layer.setResource(catalog.getResource(fixture.resourceIds.getFirst(), FeatureTypeInfo.class));
        layer.setDefaultStyle(catalog.getStyleByName("style1"));
        layer.setEnabled(true);
        catalog.add(layer);

        LayerGroupInfo inner = factory.createLayerGroup();
        inner.setName("inner");
        inner.setMode(LayerGroupInfo.Mode.SINGLE);
        inner.setWorkspace(ws);
        inner.getLayers().add(catalog.getLayer(layer.getId()));
        inner.getStyles().add(null);
        catalog.add(inner);

        LayerGroupInfo outer = factory.createLayerGroup();
        outer.setName("outer");
        outer.setMode(LayerGroupInfo.Mode.SINGLE);
        outer.setWorkspace(ws);
        outer.getLayers().add(catalog.getLayerGroup(inner.getId()));
        outer.getStyles().add(null);
        catalog.add(outer);

        assertWorkspaceNameInMat(inner.getId(), ws.getName());
        assertWorkspaceNameInMat(outer.getId(), ws.getName());

        jdbc.update(
                "UPDATE workspaceinfo SET info = jsonb_set(info, '{name}', to_jsonb(?::text)) WHERE id = ?",
                "renamed",
                ws.getId());

        assertWorkspaceNameInMat(inner.getId(), "renamed");
        assertWorkspaceNameInMat(outer.getId(), "renamed");
    }

    @Test
    void renamingWorkspaceRefreshesPublishedInfosMatForBothLayersAndLayerGroups() {
        // Regression for V2_0_0 workspaceinfo_update_trigger(): the trigger had two SELECT INTO
        // statements overwriting each other, dropping LayerGroupInfo IDs from affected_ids
        // before the PERFORM. Without V3_0_2, renaming a workspace leaves the layer-group rows
        // in publishedinfos_mat with stale workspace.name / prefixedName.
        Fixture fixture = newFixtureWithResources(1);
        WorkspaceInfo ws = fixture.sourceWorkspace;

        CatalogFactory factory = catalog.getFactory();
        StyleInfo style = factory.createStyle();
        style.setName("style1");
        style.setFilename("style1.sld");
        catalog.add(style);
        LayerInfo layer = factory.createLayer();
        layer.setResource(catalog.getResource(fixture.resourceIds.getFirst(), FeatureTypeInfo.class));
        layer.setDefaultStyle(catalog.getStyleByName("style1"));
        layer.setEnabled(true);
        catalog.add(layer);

        LayerGroupInfo group = factory.createLayerGroup();
        group.setName("lg");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.setWorkspace(ws);
        group.getLayers().add(catalog.getLayer(layer.getId()));
        group.getStyles().add(null);
        catalog.add(group);

        assertWorkspaceNameInMat(layer.getId(), ws.getName());
        assertWorkspaceNameInMat(group.getId(), ws.getName());

        // Direct JDBC rename so only the workspaceinfo BEFORE+AFTER triggers run; this isolates
        // the trigger from any Java-side catalog cascade and exposes the regression.
        jdbc.update(
                "UPDATE workspaceinfo SET info = jsonb_set(info, '{name}', to_jsonb(?::text)) WHERE id = ?",
                "renamed",
                ws.getId());

        assertWorkspaceNameInMat(layer.getId(), "renamed");
        assertWorkspaceNameInMat(group.getId(), "renamed");
    }

    private void assertWorkspaceNameInMat(String publishedId, String expectedWorkspaceName) {
        String layerWsName = jdbc.queryForObject(
                """
                SELECT COALESCE("workspace.name", "resource.store.workspace.name")
                  FROM publishedinfos_mat
                 WHERE id = ?
                """,
                String.class,
                publishedId);
        assertThat(layerWsName)
                .as("publishedinfos_mat[%s] workspace name", publishedId)
                .isEqualTo(expectedWorkspaceName);
    }

    @Test
    void dataRepairFixesPreexistingMismatch() {
        Fixture fixture = newFixtureWithResources(3);

        // Simulate the broken production state: disable the cascade trigger and force the
        // store into the target workspace via the JSON path. The existing BEFORE trigger
        // still updates the FK column, but the resource namespaces stay out of sync.
        jdbc.execute("ALTER TABLE storeinfo DISABLE TRIGGER storeinfo_cascade_workspace_to_resource_namespace");
        try {
            int moved = jdbc.update(
                    "UPDATE storeinfo SET info = jsonb_set(info, '{workspace}', to_jsonb(?::text)) WHERE id = ?",
                    fixture.targetWorkspace.getId(),
                    fixture.store.getId());
            assertThat(moved).isOne();
            // resources still point at the source namespace - confirms broken state
            assertResourceinfoFkPointsTo(fixture.resourceIds, fixture.sourceNamespace);
        } finally {
            jdbc.execute("ALTER TABLE storeinfo ENABLE TRIGGER storeinfo_cascade_workspace_to_resource_namespace");
        }

        // Run the same one-shot repair SQL the migration ships.
        int repaired = jdbc.update(
                """
                WITH expected AS (
                  SELECT r.id           AS resource_id,
                         n.id           AS expected_namespace_id
                    FROM resourceinfo r
                    INNER JOIN storeinfo     s ON r.store = s.id
                    INNER JOIN workspaceinfo w ON s.workspace = w.id
                    INNER JOIN namespaceinfo n ON n.name = w.name
                   WHERE r.namespace <> n.id
                )
                UPDATE resourceinfo r
                   SET info = jsonb_set(r.info, '{namespace}', to_jsonb(e.expected_namespace_id))
                  FROM expected e
                 WHERE r.id = e.resource_id
                """);
        assertThat(repaired).isEqualTo(3);

        assertResourceinfoFkPointsTo(fixture.resourceIds, fixture.targetNamespace);
        assertResourceinfoJsonNamespaceIs(fixture.resourceIds, fixture.targetNamespace);
    }

    private Fixture newFixtureWithResources(int resourceCount) {
        CatalogFactory factory = catalog.getFactory();

        WorkspaceInfo wsSource = factory.createWorkspace();
        wsSource.setName("src");
        catalog.add(wsSource);

        NamespaceInfo nsSource = factory.createNamespace();
        nsSource.setPrefix("src");
        nsSource.setURI("http://src.example");
        catalog.add(nsSource);

        WorkspaceInfo wsTarget = factory.createWorkspace();
        wsTarget.setName("tgt");
        catalog.add(wsTarget);

        NamespaceInfo nsTarget = factory.createNamespace();
        nsTarget.setPrefix("tgt");
        nsTarget.setURI("http://tgt.example");
        catalog.add(nsTarget);

        DataStoreInfo store = factory.createDataStore();
        store.setName("ds");
        store.setWorkspace(wsSource);
        store.setEnabled(true);
        catalog.add(store);

        List<String> resourceIds = new ArrayList<>(resourceCount);
        for (int i = 0; i < resourceCount; i++) {
            FeatureTypeInfo ft = factory.createFeatureType();
            ft.setName("ft" + i);
            ft.setNativeName("ft" + i);
            ft.setStore(store);
            ft.setNamespace(nsSource);
            ft.setEnabled(true);
            catalog.add(ft);
            resourceIds.add(ft.getId());
        }

        DataStoreInfo persistedStore = catalog.getDataStore(store.getId());
        NamespaceInfo persistedSrc = catalog.getNamespaceByPrefix("src");
        NamespaceInfo persistedTgt = catalog.getNamespaceByPrefix("tgt");
        WorkspaceInfo persistedSrcWs = catalog.getWorkspaceByName("src");
        WorkspaceInfo persistedTgtWs = catalog.getWorkspaceByName("tgt");
        return new Fixture(persistedStore, persistedSrcWs, persistedTgtWs, persistedSrc, persistedTgt, resourceIds);
    }

    private void moveStore(DataStoreInfo store, WorkspaceInfo target) {
        DataStoreInfo fresh = catalog.getDataStore(store.getId());
        fresh.setWorkspace(target);
        catalog.save(fresh);
    }

    private DataStoreInfo reloadStore(DataStoreInfo store) {
        return catalog.getDataStore(store.getId());
    }

    private void assertResourceinfoFkPointsTo(List<String> resourceIds, NamespaceInfo expected) {
        for (String id : resourceIds) {
            String fk = jdbc.queryForObject("SELECT namespace FROM resourceinfo WHERE id = ?", String.class, id);
            assertThat(fk).as("resourceinfo[%s].namespace FK column", id).isEqualTo(expected.getId());
        }
    }

    private void assertResourceinfoJsonNamespaceIs(List<String> resourceIds, NamespaceInfo expected) {
        for (String id : resourceIds) {
            String jsonNs =
                    jdbc.queryForObject("SELECT info->>'namespace' FROM resourceinfo WHERE id = ?", String.class, id);
            assertThat(jsonNs).as("resourceinfo[%s].info->>'namespace'", id).isEqualTo(expected.getId());
        }
    }

    private record Fixture(
            DataStoreInfo store,
            WorkspaceInfo sourceWorkspace,
            WorkspaceInfo targetWorkspace,
            NamespaceInfo sourceNamespace,
            NamespaceInfo targetNamespace,
            List<String> resourceIds) {}
}
