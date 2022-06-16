/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Patch.Property;
import org.geoserver.catalog.plugin.PropertyDiffTestSupport;
import org.geoserver.cloud.bus.catalog.RemoteInfoEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoveEvent;
import org.geoserver.cloud.event.catalog.DefaultDataStoreEvent;
import org.geoserver.cloud.event.catalog.DefaultNamespaceEvent;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceEvent;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModifyEvent;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class CatalogRemoteApplicationEventsTest extends BusAmqpIntegrationTests {

    public @Test void testCatalogSetDefaultWorkspace() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceC);
        final Class<DefaultWorkspaceEvent> eventType = DefaultWorkspaceEvent.class;
        {
            Patch expected =
                    new PropertyDiffTestSupport()
                            .createTestDiff(
                                    "defaultWorkspace", testData.workspaceA, testData.workspaceC)
                            .toPatch();

            Consumer<Catalog> modifier = c -> c.setDefaultWorkspace(testData.workspaceC);
            Predicate<DefaultWorkspaceEvent> filter =
                    e -> testData.workspaceC.getId().equals(e.getNewWorkspaceId());

            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
        {
            Patch expected =
                    new PropertyDiffTestSupport()
                            .createTestDiff("defaultWorkspace", testData.workspaceC, null)
                            .toPatch();

            Consumer<Catalog> modifier = c -> c.setDefaultWorkspace(null);
            Predicate<DefaultWorkspaceEvent> filter = e -> e.getNewWorkspaceId() == null;
            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
    }

    public @Test void testCatalogSetDefaultNamespace() {
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);

        final Class<DefaultNamespaceEvent> eventType = DefaultNamespaceEvent.class;

        {
            Consumer<Catalog> modifier = c -> c.setDefaultNamespace(testData.namespaceB);
            Predicate<DefaultNamespaceEvent> filter =
                    e -> testData.namespaceB.getId().equals(e.getNewNamespaceId());
            Patch expected =
                    new PropertyDiffTestSupport()
                            .createTestDiff(
                                    "defaultNamespace", testData.namespaceA, testData.namespaceB)
                            .toPatch();

            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
        {
            Patch expected =
                    new PropertyDiffTestSupport()
                            .createTestDiff("defaultNamespace", testData.namespaceA, null)
                            .toPatch();

            Consumer<Catalog> modifier = c -> c.setDefaultNamespace(null);
            Predicate<DefaultNamespaceEvent> filter = e -> null == e.getNewNamespaceId();

            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
    }

    public @Test void testCatalogSetDefaultDataStoreByWorkspace() {
        WorkspaceInfo workspace = testData.workspaceA;
        DataStoreInfo dataStore = testData.dataStoreA;

        catalog.add(workspace);
        catalog.add(testData.namespaceA);

        final Class<DefaultDataStoreEvent> eventType = DefaultDataStoreEvent.class;

        {
            Patch expected =
                    new PropertyDiffTestSupport()
                            .createTestDiff("defaultDataStore", null, dataStore)
                            .toPatch();

            Predicate<DefaultDataStoreEvent> filter =
                    e -> dataStore.getId().equals(e.getDefaultDataStoreId());
            DefaultDataStoreEvent event =
                    testCatalogModifiedEvent(
                            catalog, c -> c.add(dataStore), expected, eventType, filter);

            assertThat(event.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(event.getDefaultDataStoreId()).isEqualTo(dataStore.getId());
        }
        {
            Patch expected =
                    new PropertyDiffTestSupport()
                            .createTestDiff("defaultDataStore", dataStore, null)
                            .toPatch();

            Predicate<DefaultDataStoreEvent> filter = e -> null == e.getDefaultDataStoreId();

            DefaultDataStoreEvent event =
                    testCatalogModifiedEvent(
                            catalog, c -> c.remove(dataStore), expected, eventType, filter);

            assertThat(event.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(event.getDefaultDataStoreId()).isNull();
        }
    }

    public @Test void testAdd_Workspace() {
        testRemoteCatalogInfoAddEvent(testData.workspaceA, catalog::add);
    }

    public @Test void testAdd_Namespace() {
        testRemoteCatalogInfoAddEvent(testData.namespaceA, catalog::add);
    }

    public @Test void testAdd_CoverageStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        testRemoteCatalogInfoAddEvent(testData.coverageStoreA, catalog::add);
    }

    public @Test void testAdd_DataStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        testRemoteCatalogInfoAddEvent(testData.dataStoreA, catalog::add);
    }

    public @Test void testAdd_Coverage() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);
        catalog.add(testData.coverageStoreA);
        testRemoteCatalogInfoAddEvent(testData.coverageA, catalog::add);
    }

    public @Test void testAdd_FeatureType() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        testRemoteCatalogInfoAddEvent(testData.featureTypeA, catalog::add);
    }

    public @Test void testAdd_Layer() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.style1);
        testRemoteCatalogInfoAddEvent(testData.layerFeatureTypeA, catalog::add);
    }

    public @Test void testAdd_LayerGroup() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.layerFeatureTypeA);
        catalog.add(testData.style1);
        testRemoteCatalogInfoAddEvent(testData.layerGroup1, catalog::add);
    }

    public @Test void testAdd_Style_Payload() {
        testRemoteCatalogInfoAddEvent(testData.style1, catalog::add);
    }

    public @Test void testModifyEventsWorkspace() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.workspaceA,
                ws -> {
                    ws.setName("newName");
                },
                catalog::save);
    }

    public @Test void testModifyEventsNamespace() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.namespaceA,
                ns -> {
                    ns.setPrefix("newPrefix");
                    ns.setURI("http://test.com/modified");
                },
                catalog::save);
    }

    public @Test void testModifyEventsDataStore() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.dataStoreA,
                ds -> {
                    ds.getConnectionParameters().put("new-key", "new-value");
                    ds.setWorkspace(testData.workspaceB);
                    ds.getMetadata().put("md-key", "md-value");
                    ds.getMetadata().put("md-int-key", 1000);
                },
                catalog::save);
    }

    public @Test void testModifyEventsCoverageStore() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.coverageStoreA,
                cs -> {
                    cs.getConnectionParameters().put("new-key", "new-value");
                    cs.setWorkspace(testData.workspaceB);
                    cs.getMetadata().put("md-key", "md-value");
                    cs.getMetadata().put("md-int-key", 1000);
                },
                catalog::save);
    }

    public @Test void testModifyEventsWMSStore() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.wmsStoreA,
                wms -> {
                    wms.getConnectionParameters().put("new-key", "new-value");
                    wms.setWorkspace(testData.workspaceB);
                    wms.getMetadata().put("md-key", "md-value");
                    wms.getMetadata().put("md-int-key", 1000);
                    wms.setCapabilitiesURL("http://test.caps.com");
                    wms.setConnectTimeout(10);
                },
                catalog::save);
    }

    public @Test void testModifyEventsWMTSStore() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.wmsStoreA,
                wmts -> {
                    wmts.getConnectionParameters().put("new-key", "new-value");
                    wmts.setWorkspace(testData.workspaceB);
                    wmts.getMetadata().put("md-key", "md-value");
                    wmts.getMetadata().put("md-int-key", 1000);
                    wmts.setCapabilitiesURL("http://test.caps.com");
                    wmts.setConnectTimeout(10);
                },
                catalog::save);
    }

    public @Test void testModifyEventsFeatureType() throws SchemaException {
        setupClean();

        SimpleFeatureType type = DataUtilities.createType("test", "name:String,location:Point");
        List<AttributeTypeInfo> attributes =
                new CatalogBuilder(catalog).getAttributes(type, testData.featureTypeA);

        // don't run equals
        Patch patch =
                testCatalogInfoModifyEventNoEquals(
                        testData.featureTypeA,
                        ft -> {
                            ft.getMetadata().put("md-key", "md-value");
                            ft.getMetadata().put("md-int-key", 1000);
                            ft.setNamespace(testData.namespaceC);
                            ft.getAttributes().addAll(attributes);
                        },
                        catalog::save);

        Property atts = patch.get("attributes").orElseThrow();
        List<AttributeTypeInfo> decodedAtts = (List<AttributeTypeInfo>) atts.getValue();
        assertEquals(attributes.size(), decodedAtts.size());
        IntStream.range(0, attributes.size())
                .forEach(
                        i -> {
                            AttributeTypeInfo decoded = decodedAtts.get(i);
                            AttributeTypeInfo orig = attributes.get(i);
                            assertThat(decoded.equalsIngnoreFeatureType(orig));
                            assertNotNull(decoded.getFeatureType());
                            FeatureTypeInfo expected =
                                    ModificationProxy.unwrap(orig.getFeatureType());
                            FeatureTypeInfo actual =
                                    ModificationProxy.unwrap(decoded.getFeatureType());
                            assertEquals(expected.getId(), actual.getId());
                            assertEquals(expected.getName(), actual.getName());
                        });
    }

    @Disabled("implement")
    public @Test void testModifyEventsCoverage() {
        fail("NOT IMPLEMENTED");
    }

    public @Test void testModifyEventsWMSLayer() throws Exception {
        setupClean();

        WMSLayerInfo layer = testData.wmsLayerA;

        ReferencedEnvelope bounds =
                new ReferencedEnvelope(-180, -90, 0, 0, CRS.decode("EPSG:4326"));

        testCatalogInfoModifyEvent(
                layer, l -> l.setDisabledServices(List.of("WMS", "WFS")), catalog::save);
        testCatalogInfoModifyEvent(layer, l -> l.setAbstract("modified"), catalog::save);
        testCatalogInfoModifyEvent(layer, l -> l.setNativeBoundingBox(bounds), catalog::save);

        testCatalogInfoModifyEvent(
                layer,
                l -> {
                    l.setDisabledServices(List.of());
                    l.setAbstract("modified again");
                    l.setNativeBoundingBox(null);
                },
                catalog::save);
    }

    public @Test void testModifyEventsWMTSLayer() throws Exception {
        setupClean();

        WMTSLayerInfo layer = testData.wmtsLayerA;

        ReferencedEnvelope bounds =
                new ReferencedEnvelope(-180, -90, 0, 0, CRS.decode("EPSG:4326"));

        testCatalogInfoModifyEvent(
                layer, l -> l.setDisabledServices(List.of("WMS", "WFS")), catalog::save);
        testCatalogInfoModifyEvent(layer, l -> l.setAbstract("modified"), catalog::save);
        testCatalogInfoModifyEvent(layer, l -> l.setNativeBoundingBox(bounds), catalog::save);

        testCatalogInfoModifyEvent(
                layer,
                l -> {
                    l.setDisabledServices(List.of());
                    l.setAbstract("modified again");
                    l.setNativeBoundingBox(null);
                },
                catalog::save);
    }

    public @Test void testModifyEventsLayer() {
        setupClean();

        LayerInfo layer = testData.layerFeatureTypeA;
        testCatalogInfoModifyEvent(
                layer,
                l -> {
                    l.setAbstract("modified");
                    AttributionInfo attr = new AttributionInfoImpl();
                    attr.setHref("http://test.com");
                    attr.setTitle("attribution");
                    l.setAttribution(attr);

                    l.setDefaultWMSInterpolationMethod(WMSInterpolation.Bicubic);
                    l.setInternationalTitle(
                            testData.faker()
                                    .internationalString(
                                            Locale.ENGLISH, "test", Locale.ITALIAN, "proba"));

                    l.setType(PublishedType.REMOTE);
                },
                catalog::save);
    }

    @Disabled("implement")
    public @Test void testModifyEventsLayerGroup() {
        fail("NOT IMPLEMENTED");
    }

    @Disabled("implement")
    public @Test void testModifyEventsStyle() {
        fail("NOT IMPLEMENTED");
    }

    public @Test void testRemoveEvents() {
        setupClean();

        Class<CatalogInfoRemoveEvent> eventType = CatalogInfoRemoveEvent.class;

        testRemoteRemoveEvent(testData.layerGroup1, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.layerFeatureTypeA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.featureTypeA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.coverageA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.style1, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.dataStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.coverageStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmsLayerA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmsStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmtsLayerA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.wmtsStoreA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.namespaceA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.workspaceA, catalog::remove, eventType);
    }

    @SuppressWarnings("rawtypes")
    private <E extends InfoEvent> E testCatalogModifiedEvent(
            Catalog catalog,
            Consumer<Catalog> modifier,
            Patch expected,
            Class<E> eventType,
            Predicate<E> filter) {

        this.eventsCaptor.stop().clear().start();

        modifier.accept(catalog);

        RemoteInfoEvent localRemoteEvent = eventsCaptor.local().expectOne(eventType, filter);
        RemoteInfoEvent sentEvent = eventsCaptor.remote().expectOne(eventType, filter);

        assertCatalogEvent(catalog, (InfoModifyEvent) localRemoteEvent.getEvent(), expected);
        assertCatalogEvent(catalog, (InfoModifyEvent) sentEvent.getEvent(), expected);
        return eventType.cast(sentEvent.getEvent());
    }

    @SuppressWarnings("rawtypes")
    private void assertCatalogEvent(Catalog catalog, InfoModifyEvent event, Patch expected) {
        assertThat(event.getObjectId()).isEqualTo("catalog"); // i.e. InfoEvent.CATALOG_ID
        assertThat(event.getObjectType()).isEqualTo(ConfigInfoType.Catalog);

        Patch actual = event.getPatch();
        assertThat(actual).isNotNull();
        assertThat(actual.getPropertyNames()).isEqualTo(expected.getPropertyNames());
        // can't compare value equality here, RevolvingProxy instances won't be resolved against
        // the remote catalog because that depends on having an actual catalog backend
        // configured
        // assertEquals(expected, actual);
    }
}
