/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PgconfigAmbiguousNameLookupIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private CatalogPlugin catalog;

    @BeforeEach
    void setUp() {
        catalog = new PgconfigBackendBuilder(db.getDataSource()).createCatalog();

        // A default workspace that does NOT publish "roads", so the unqualified lookup misses the
        // default namespace and falls back to the ANY_NAMESPACE path
        // (as it does for a workspace virtual service whose workspace is not the catalog's default).
        createWorkspaceWithFeatureType("base", "rivers");
        catalog.setDefaultWorkspace(catalog.getWorkspaceByName("base"));
        catalog.setDefaultNamespace(catalog.getNamespaceByPrefix("base"));

        // Two workspaces publishing a feature type with the same name "roads",
        // plus a feature type "lakes" unique to wsa.
        createWorkspaceWithFeatureType("wsa", "roads");
        createWorkspaceWithFeatureType("wsb", "roads");
        addFeatureType(catalog.getWorkspaceByName("wsa"), "lakes");
    }

    @Test
    void getFeatureTypeByNameReturnsFirstMatchWhenNameIsAmbiguous() {
        FeatureTypeInfo ft = catalog.getFeatureTypeByName("roads");
        assertThat(ft)
                .as("an unqualified name present in >1 workspace must resolve to the first match, not throw")
                .isNotNull();
        assertThat(ft.getName()).isEqualTo("roads");
    }

    @Test
    void getFeatureTypeByNameIsDeterministicWhenNameIsAmbiguous() {
        NamespaceInfo nsA = catalog.getNamespaceByPrefix("wsa");
        NamespaceInfo nsB = catalog.getNamespaceByPrefix("wsb");
        String idA =
                catalog.getResourceByName(nsA, "roads", FeatureTypeInfo.class).getId();
        String idB =
                catalog.getResourceByName(nsB, "roads", FeatureTypeInfo.class).getId();
        // findFirstByName resolves with "ORDER BY id", so the lowest id wins, repeatably.
        String expected = idA.compareTo(idB) <= 0 ? idA : idB;
        assertThat(catalog.getFeatureTypeByName("roads").getId()).isEqualTo(expected);
        assertThat(catalog.getFeatureTypeByName("roads").getId()).isEqualTo(expected);
    }

    @Test
    void getFeatureTypeByNameResolvesUniqueName() {
        FeatureTypeInfo ft = catalog.getFeatureTypeByName("lakes");
        assertThat(ft)
                .as("a unique unqualified name was never affected and must still resolve")
                .isNotNull();
        assertThat(ft.getName()).isEqualTo("lakes");
        assertThat(ft.getNamespace().getPrefix()).isEqualTo("wsa");
    }

    private void createWorkspaceWithFeatureType(String workspaceName, String featureType) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo workspace = factory.createWorkspace();
        workspace.setName(workspaceName);
        catalog.add(workspace);

        NamespaceInfo namespace = factory.createNamespace();
        namespace.setPrefix(workspaceName);
        namespace.setURI("http://%s.example".formatted(workspaceName));
        catalog.add(namespace);

        addFeatureType(workspace, featureType);
    }

    private void addFeatureType(WorkspaceInfo workspace, String featureType) {
        CatalogFactory factory = catalog.getFactory();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());

        DataStoreInfo store = factory.createDataStore();
        store.setName("%s-%s-store".formatted(workspace.getName(), featureType));
        store.setWorkspace(workspace);
        store.setEnabled(true);
        catalog.add(store);

        FeatureTypeInfo ft = factory.createFeatureType();
        ft.setName(featureType);
        ft.setNativeName(featureType);
        ft.setStore(store);
        ft.setNamespace(namespace);
        ft.setEnabled(true);
        catalog.add(ft);
    }
}
