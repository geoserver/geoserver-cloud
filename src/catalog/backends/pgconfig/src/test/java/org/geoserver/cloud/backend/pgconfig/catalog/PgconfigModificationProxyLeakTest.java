/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Geometry;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies that the object graphs built below {@link CatalogPlugin} hold raw objects, never a
 * {@link ModificationProxy}. {@code CatalogPlugin} wraps each returned object in a proxy of its own, for one caller.
 *
 * <p>A proxy must never end up inside a stored object graph. Threads reading the graph would share it, and its
 * unsynchronized state maps would take their writes. {@code ModificationProxyCloner} serializes these maps when it
 * clones a collection such as {@code FeatureTypeInfo.getAttributes()}. A write during the serialization corrupts the
 * stream, and the clone fails with {@code java.io.OptionalDataException}.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigModificationProxyLeakTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private CatalogPlugin catalog;

    @BeforeEach
    void setUp() {
        catalog = new PgconfigBackendBuilder(db.getDataSource()).createCatalog();
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

    private void assertNotAModificationProxy(String role, Object info) {
        assertThat(info).as(role).isNotNull();
        assertThat(ModificationProxy.handler(info))
                .as("%s must not be a ModificationProxy: the DAO layer trades in raw objects only".formatted(role))
                .isNull();
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
        ft.getAttributes().add(attribute(factory, "geom", Geometry.class));
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
