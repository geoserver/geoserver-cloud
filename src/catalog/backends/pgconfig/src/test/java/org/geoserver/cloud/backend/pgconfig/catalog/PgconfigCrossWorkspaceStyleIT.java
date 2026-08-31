/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
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
class PgconfigCrossWorkspaceStyleIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private CatalogPlugin catalog;

    @BeforeEach
    void setUp() {
        catalog = new PgconfigBackendBuilder(db.getDataSource()).createCatalog();
        createWorkspace("shared");
        createWorkspace("wsa");
        addStyle("shared", "common");
        addLayer("wsa", "roads", catalog.getStyleByName(catalog.getWorkspaceByName("shared"), "common"));
    }

    @Test
    void layerStyleKeepsItsOwnWorkspace() {
        LayerInfo layer = catalog.getLayerByName("wsa:roads");
        StyleInfo style = layer.getDefaultStyle();

        assertThat(style.getWorkspace().getName()).isEqualTo("shared");
        assertThat(style.prefixedName()).isEqualTo("shared:common");
    }

    @Test
    void layerStyleIsTheStoredStyle() {
        StyleInfo stored = catalog.getStyleByName(catalog.getWorkspaceByName("shared"), "common");
        StyleInfo fromLayer = catalog.getLayerByName("wsa:roads").getDefaultStyle();

        assertThat(fromLayer.getId()).isEqualTo(stored.getId());
        assertThat(fromLayer.getFilename()).isEqualTo(stored.getFilename());
    }

    private void createWorkspace(String name) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo workspace = factory.createWorkspace();
        workspace.setName(name);
        catalog.add(workspace);

        NamespaceInfo namespace = factory.createNamespace();
        namespace.setPrefix(name);
        namespace.setURI("http://%s.example".formatted(name));
        catalog.add(namespace);
    }

    private void addStyle(String workspaceName, String name) {
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(name);
        style.setFilename("%s.sld".formatted(name));
        style.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        catalog.add(style);
    }

    private void addLayer(String workspaceName, String featureType, StyleInfo defaultStyle) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo workspace = catalog.getWorkspaceByName(workspaceName);
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspaceName);

        DataStoreInfo store = factory.createDataStore();
        store.setName("%s-store".formatted(featureType));
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

        LayerInfo layer = factory.createLayer();
        layer.setResource(ft);
        layer.setDefaultStyle(defaultStyle);
        catalog.add(layer);
    }
}
