/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Patch.Property;
import org.geoserver.catalog.plugin.PropertyDiffTestSupport;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.catalog.DefaultNamespaceSet;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceSet;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.cog.CogSettings.RangeReaderType;
import org.geoserver.cog.CogSettingsStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CatalogRemoteApplicationEventsIT extends BusAmqpIntegrationTests {

    @Test
    void testCatalogSetDefaultWorkspace() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceC);
        final Class<DefaultWorkspaceSet> eventType = DefaultWorkspaceSet.class;
        {
            Patch expected = new PropertyDiffTestSupport()
                    .createTestDiff("defaultWorkspace", testData.workspaceA, testData.workspaceC)
                    .toPatch();

            Consumer<Catalog> modifier = c -> c.setDefaultWorkspace(testData.workspaceC);
            Predicate<DefaultWorkspaceSet> filter =
                    e -> testData.workspaceC.getId().equals(e.getNewWorkspaceId());

            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
        {
            Patch expected = new PropertyDiffTestSupport()
                    .createTestDiff("defaultWorkspace", testData.workspaceC, null)
                    .toPatch();

            Consumer<Catalog> modifier = c -> c.setDefaultWorkspace(null);
            Predicate<DefaultWorkspaceSet> filter = e -> e.getNewWorkspaceId() == null;
            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
    }

    @Test
    void testCatalogSetDefaultNamespace() {
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);

        final Class<DefaultNamespaceSet> eventType = DefaultNamespaceSet.class;

        {
            Consumer<Catalog> modifier = c -> c.setDefaultNamespace(testData.namespaceB);
            Predicate<DefaultNamespaceSet> filter =
                    e -> testData.namespaceB.getId().equals(e.getNewNamespaceId());
            Patch expected = new PropertyDiffTestSupport()
                    .createTestDiff("defaultNamespace", testData.namespaceA, testData.namespaceB)
                    .toPatch();

            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
        {
            Patch expected = new PropertyDiffTestSupport()
                    .createTestDiff("defaultNamespace", testData.namespaceA, null)
                    .toPatch();

            Consumer<Catalog> modifier = c -> c.setDefaultNamespace(null);
            Predicate<DefaultNamespaceSet> filter = e -> null == e.getNewNamespaceId();

            testCatalogModifiedEvent(catalog, modifier, expected, eventType, filter);
        }
    }

    @Test
    void testCatalogSetDefaultDataStoreByWorkspace() {
        WorkspaceInfo workspace = testData.workspaceA;
        DataStoreInfo dataStore = testData.dataStoreA;

        catalog.add(workspace);
        catalog.add(testData.namespaceA);

        final Class<DefaultDataStoreSet> eventType = DefaultDataStoreSet.class;

        {
            Patch expected = new PropertyDiffTestSupport()
                    .createTestDiff("defaultDataStore", null, dataStore)
                    .toPatch();

            Predicate<DefaultDataStoreSet> filter = e -> dataStore.getId().equals(e.getDefaultDataStoreId());
            DefaultDataStoreSet event =
                    testCatalogModifiedEvent(catalog, c -> c.add(dataStore), expected, eventType, filter);

            assertThat(event.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(event.getDefaultDataStoreId()).isEqualTo(dataStore.getId());
        }
        {
            Patch expected = new PropertyDiffTestSupport()
                    .createTestDiff("defaultDataStore", dataStore, null)
                    .toPatch();

            Predicate<DefaultDataStoreSet> filter = e -> null == e.getDefaultDataStoreId();

            DefaultDataStoreSet event =
                    testCatalogModifiedEvent(catalog, c -> c.remove(dataStore), expected, eventType, filter);

            assertThat(event.getWorkspaceId()).isEqualTo(workspace.getId());
            assertThat(event.getDefaultDataStoreId()).isNull();
        }
    }

    @Test
    void testAdd_Workspace() {
        testRemoteCatalogInfoAddEvent(testData.workspaceA, catalog::add);
    }

    @Test
    void testAdd_Namespace() {
        testRemoteCatalogInfoAddEvent(testData.namespaceA, catalog::add);
    }

    @Test
    void testAdd_CoverageStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        testRemoteCatalogInfoAddEvent(testData.coverageStoreA, catalog::add);
    }

    @Test
    void testAdd_DataStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        testRemoteCatalogInfoAddEvent(testData.dataStoreA, catalog::add);
    }

    @Test
    void testAdd_Coverage() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);
        catalog.add(testData.coverageStoreA);
        testRemoteCatalogInfoAddEvent(testData.coverageA, catalog::add);
    }

    @Test
    void testAdd_FeatureType() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        testRemoteCatalogInfoAddEvent(testData.featureTypeA);
    }

    @Test
    void testAdd_Layer() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.style1);
        testRemoteCatalogInfoAddEvent(testData.layerFeatureTypeA);
    }

    @Test
    void testAdd_LayerGroup() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.layerFeatureTypeA);
        catalog.add(testData.style1);
        testRemoteCatalogInfoAddEvent(testData.layerGroup1, catalog::add);
    }

    @Test
    void testAdd_Style_Payload() {
        testRemoteCatalogInfoAddEvent(testData.style1, catalog::add);
    }

    @Test
    void testModifyEventsWorkspace() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.workspaceA,
                ws -> {
                    ws.setName("newName");
                },
                catalog::save);
    }

    @Test
    void testModifyEventsNamespace() {
        setupClean();
        testCatalogInfoModifyEvent(
                testData.namespaceA,
                ns -> {
                    ns.setPrefix("newPrefix");
                    ns.setURI("http://test.com/modified");
                },
                catalog::save);
    }

    @Test
    void testModifyEventsDataStore() {
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

    @Test
    void testModifyEventsCoverageStore() {
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

    @Test
    void tesAddCoverageStore_COG() {
        CoverageStoreInfo store = createCOGStoreInfo();
        RemoteGeoServerEvent remoteEvent = testRemoteCatalogInfoAddEvent(store, catalog::add);

        store = catalog.getCoverageStoreByName(testData.workspaceA.getName(), store.getName());
        assertNotNull(store);

        CatalogInfoAdded event = (CatalogInfoAdded) remoteEvent.getEvent();
        CoverageStoreInfo sentObject = (CoverageStoreInfo) event.getObject();
        MetadataMap metadata = sentObject.getMetadata();
        assertThat(metadata).containsKey("cogSettings");
        assertThat(metadata.get("cogSettings")).isInstanceOf(CogSettingsStore.class);
    }

    private CoverageStoreInfo createCOGStoreInfo() {
        setupClean();
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);

        final String cogUrl =
                "https://s3-us-west-2.amazonaws.com/sentinel-cogs/sentinel-s2-l2a-cogs/5/C/MK/2018/10/S2B_5CMK_20181020_0_L2A/B01.tif";
        CoverageStoreInfo store = new CoverageStoreInfoImpl(catalog);
        store.setWorkspace(testData.workspaceA);
        store.setName("COG");
        store.setEnabled(true);
        store.setDisableOnConnFailure(true);
        store.setType("GeoTIFF");
        store.setURL(cogUrl);

        CogSettingsStore cogSettings = new CogSettingsStore();
        cogSettings.setRangeReaderSettings(RangeReaderType.HTTP);
        store.getMetadata().put("cogSettings", cogSettings);

        return store;
    }

    @Test
    void testModifyEventsCoverageStore_COG() {
        setupClean();
        CoverageStoreInfo store = testData.coverageStoreA;

        CogSettingsStore cogSettings = new CogSettingsStore();
        cogSettings.setRangeReaderSettings(RangeReaderType.S3);
        cogSettings.setUsername("user");

        Patch patch = testCatalogInfoModifyEventNoEquals(
                store,
                cs -> {
                    cs.getMetadata().put("cogSettings", cogSettings);
                },
                catalog::save);

        store = catalog.getCoverageStoreByName(testData.workspaceA.getName(), store.getName());
        assertNotNull(store);

        Map<?, ?> md = (Map<?, ?>) patch.get("metadata").orElseThrow().getValue();
        CogSettingsStore settings = (CogSettingsStore) md.get("cogSettings");
        assertThat(settings).isNotNull();
        assertThat(settings.getUsername()).isEqualTo("user");
        assertThat(settings.getRangeReaderSettings()).isEqualTo(RangeReaderType.S3);
    }

    @Test
    void testModifyEventsWMSStore() {
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

    @Test
    void testModifyEventsWMTSStore() {
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

    @Test
    void testModifyEventsFeatureType() throws SchemaException {
        setupClean();

        SimpleFeatureType type = DataUtilities.createType("test", "name:String,location:Point");
        List<AttributeTypeInfo> attributes = new CatalogBuilder(catalog).getAttributes(type, testData.featureTypeA);

        // don't run equals
        Patch patch = testCatalogInfoModifyEventNoEquals(
                testData.featureTypeA,
                ft -> {
                    ft.getMetadata().put("md-key", "md-value");
                    ft.getMetadata().put("md-int-key", 1000);
                    ft.setNamespace(testData.namespaceC);
                    ft.getAttributes().addAll(attributes);
                },
                catalog::save);

        Property atts = patch.get("attributes").orElseThrow();
        @SuppressWarnings("unchecked")
        List<AttributeTypeInfo> decodedAtts = (List<AttributeTypeInfo>) atts.getValue();
        assertEquals(attributes.size(), decodedAtts.size());
        IntStream.range(0, attributes.size()).forEach(i -> {
            AttributeTypeInfo decoded = decodedAtts.get(i);
            AttributeTypeInfo orig = attributes.get(i);
            assertThat(decoded.equalsIgnoreFeatureType(orig));
            assertNotNull(decoded.getFeatureType());
            FeatureTypeInfo expected = ModificationProxy.unwrap(orig.getFeatureType());
            FeatureTypeInfo actual = ModificationProxy.unwrap(decoded.getFeatureType());
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getName(), actual.getName());
        });
    }

    @Disabled("implement")
    @Test
    void testModifyEventsCoverage() {
        fail("NOT IMPLEMENTED");
    }

    @Test
    void testModifyEventsWMSLayer() throws Exception {
        setupClean();

        WMSLayerInfo layer = testData.wmsLayerA;

        ReferencedEnvelope bounds = new ReferencedEnvelope(-180, -90, 0, 0, CRS.decode("EPSG:4326"));

        testCatalogInfoModifyEvent(layer, l -> l.setDisabledServices(List.of("WMS", "WFS")), catalog::save);
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

    @Test
    void testModifyEventsWMTSLayer() throws Exception {
        setupClean();

        WMTSLayerInfo layer = testData.wmtsLayerA;

        ReferencedEnvelope bounds = new ReferencedEnvelope(-180, -90, 0, 0, CRS.decode("EPSG:4326"));

        testCatalogInfoModifyEvent(layer, l -> l.setDisabledServices(List.of("WMS", "WFS")), catalog::save);
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

    @Test
    void testModifyEventsLayer() {
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
                            testData.faker().internationalString(Locale.ENGLISH, "test", Locale.ITALIAN, "proba"));

                    l.setType(PublishedType.REMOTE);
                },
                catalog::save);
    }

    @Disabled("implement")
    @Test
    void testModifyEventsLayerGroup() {
        fail("NOT IMPLEMENTED");
    }

    @Disabled("implement")
    @Test
    void testModifyEventsStyle() {
        fail("NOT IMPLEMENTED");
    }

    @Test
    void testRemoveEvents() {
        setupClean();

        Class<CatalogInfoRemoved> eventType = CatalogInfoRemoved.class;

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

    private <E extends InfoEvent> E testCatalogModifiedEvent(
            Catalog catalog, Consumer<Catalog> modifier, Patch expected, Class<E> eventType, Predicate<E> filter) {

        this.eventsCaptor.stop().clear().start();

        modifier.accept(catalog);

        RemoteGeoServerEvent localRemoteEvent = eventsCaptor.local().expectOne(eventType, filter);
        RemoteGeoServerEvent sentEvent = eventsCaptor.remote().expectOne(eventType, filter);

        assertCatalogEvent((InfoModified) localRemoteEvent.getEvent(), expected);
        assertCatalogEvent((InfoModified) sentEvent.getEvent(), expected);
        return eventType.cast(sentEvent.getEvent());
    }

    private void assertCatalogEvent(InfoModified event, Patch expected) {
        assertThat(event.getObjectId()).isEqualTo("catalog"); // i.e. InfoEvent.CATALOG_ID
        assertThat(event.getObjectType()).isEqualTo(ConfigInfoType.CATALOG);

        Patch actual = event.getPatch();
        assertThat(actual).isNotNull();
        assertThat(actual.getPropertyNames()).isEqualTo(expected.getPropertyNames());
        // can't compare value equality here, RevolvingProxy instances won't be resolved against
        // the remote catalog because that depends on having an actual catalog backend
        // configured
    }
}
