/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.cloud.bus.GeoServerBusProperties;
import org.geoserver.cloud.event.PropertyDiff;
import org.geoserver.cloud.event.PropertyDiffTestSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RemoteEventPayloadCodecTest {

    private Catalog catalog;
    private GeoServer geoServer;
    private GeoServerBusProperties geoServerBusProperties;
    private RemoteEventPayloadCodec codec;

    private final PropertyDiffTestSupport support = new PropertyDiffTestSupport();

    public CatalogTestData catalogTestSupport;

    public @Before void before() throws Exception {
        catalog = new org.geoserver.catalog.impl.CatalogImpl();
        geoServer = new GeoServerImpl();
        geoServer.setCatalog(catalog);
        catalogTestSupport =
                CatalogTestData.initialized(() -> catalog, () -> geoServer)
                        .initConfig(false)
                        .initialize();
        geoServerBusProperties = new GeoServerBusProperties();
        codec = new RemoteEventPayloadCodec();
        codec.setGeoServer(geoServer);
        codec.setRawCatalog(catalog);
        codec.setPersisterFactory(new XStreamPersisterFactory());
        codec.setGeoServerBusProperties(geoServerBusProperties);
        codec.initializeCodec();

        geoServerBusProperties.setSendDiff(true);
        geoServerBusProperties.setSendObject(true);
    }

    public @After void after() {
        catalog.getFacade().dispose();
    }

    public @Test void propertyDiff_disabled() throws Exception {
        this.geoServerBusProperties.setSendDiff(false);
        PropertyDiff diff = support.createTestDiff("prop1", "oldValue", "newValue");
        assertNull(codec.encode(diff));
    }

    public @Test void propertyDiff_empty() throws Exception {
        PropertyDiff diff = support.createTestDiff();
        PropertyDiff roundtripped = codec.decode(codec.encode(diff));
        assertEquals(diff, roundtripped);
    }

    public @Test void propertyDiff_Simple() throws Exception {
        PropertyDiff diff = support.createTestDiff("prop1", "oldValue", "newValue");
        PropertyDiff roundtripped = codec.decode(codec.encode(diff));
        assertEquals(diff, roundtripped);
    }

    public @Test void propertyDiff_CRS() throws Exception {
        CoordinateReferenceSystem oldValue = CRS.decode("EPSG:4326", false);
        CoordinateReferenceSystem newValue = CRS.decode("EPSG:4326", true);
        PropertyDiff diff = support.createTestDiff("crsProp", oldValue, newValue);
        PropertyDiff roundtripped = codec.decode(codec.encode(diff));
        assertEquals(1, roundtripped.size());
        assertTrue(roundtripped.get(0).getOldValue() instanceof CoordinateReferenceSystem);
        assertTrue(roundtripped.get(0).getNewValue() instanceof CoordinateReferenceSystem);
        assertTrue(CRS.equalsIgnoreMetadata(oldValue, roundtripped.get(0).getOldValue()));
        assertTrue(CRS.equalsIgnoreMetadata(newValue, roundtripped.get(0).getNewValue()));
    }

    public @Test void propertyDiff_Workspace() throws Exception {
        WorkspaceInfo wsA = catalog.getWorkspace(catalogTestSupport.workspaceB.getId());
        WorkspaceInfo wsB = catalog.getWorkspace(catalogTestSupport.workspaceC.getId());
        assertNotNull(wsA);
        assertNotNull(wsB);
        PropertyDiff diff = support.createTestDiff("ws", wsA, wsB);
        PropertyDiff roundtripped = codec.decode(codec.encode(diff));
        assertEquals(diff, roundtripped);
    }

    public @Test void propertyDiff_DataStore() throws Exception {
        DataStoreInfo ds1 = catalog.getDataStore(catalogTestSupport.dataStoreA.getId());
        DataStoreInfo ds2 = catalog.getDataStore(catalogTestSupport.dataStoreB.getId());
        testPropertyDiff_InfoObject(ds1, ds2);
    }

    public @Test void propertyDiff_CoverageStore() throws Exception {
        CoverageStoreInfo cs1 = catalog.getCoverageStore(catalogTestSupport.coverageStoreA.getId());
        testPropertyDiff_InfoObject(null, cs1);
    }

    public @Test void propertyDiff_Layer() throws Exception {
        LayerInfo l = catalog.getLayer(catalogTestSupport.layerFeatureTypeA.getId());
        testPropertyDiff_InfoObject(null, l);
    }

    public @Test void propertyDiff_LayerGroup() throws Exception {
        LayerGroupInfo lg = catalog.getLayerGroup(catalogTestSupport.layerGroup1.getId());
        testPropertyDiff_InfoObject(null, lg);
    }

    public @Test void propertyDiff_GeoServerInfo() throws Exception {
        GeoServerInfo global = geoServer.getGlobal();
        testPropertyDiff_InfoObject(global, global);
    }

    public @Test void object_Disabled() throws IOException {
        this.geoServerBusProperties.setSendObject(false);
        SettingsInfo settings = geoServer.getSettings();
        assertNull(codec.encode(settings));
    }

    public @Test void object_CatalogInfo() throws IOException {
        testInfoObject(catalogTestSupport.workspaceA);
        testInfoObject(catalogTestSupport.coverageStoreA);
        testInfoObject(catalogTestSupport.coverageA);
        testInfoObject(catalogTestSupport.dataStoreA);
        testInfoObject(catalogTestSupport.featureTypeA);
        testInfoObject(catalogTestSupport.layerFeatureTypeA);
        testInfoObject(catalogTestSupport.layerGroup1);
        testInfoObject(catalogTestSupport.namespaceA);
        testInfoObject(catalogTestSupport.style1);
        testInfoObject(catalogTestSupport.wmsLayerA);
        testInfoObject(catalogTestSupport.wmsStoreA);
        testInfoObject(catalogTestSupport.wmtsStoreA);
        testInfoObject(catalogTestSupport.wmtsLayerA);
    }

    public @Test void object_GeoServerInfo() throws IOException {
        testInfoObject(geoServer.getGlobal());
    }

    public @Test void object_LoggingInfo() throws IOException {
        testInfoObject(geoServer.getLogging());
    }

    public @Test void object_Settings() throws IOException {
        SettingsInfo settings = geoServer.getSettings();
        testInfoObject(settings);
        settings.setWorkspace(catalogTestSupport.workspaceC);
        testInfoObject(settings);
    }

    private void testInfoObject(@NonNull Info expected) throws IOException {
        String encoded = codec.encode(expected);
        Class<? extends Info> class1 = ModificationProxy.unwrap(expected).getClass();
        Info roundtripped = codec.decode(encoded, class1);
        assertNotNull(roundtripped);

        // help avoid false positives due to null collections
        if (roundtripped != null) OwsUtils.resolveCollections(roundtripped);

        // help avoid false positives due to modification proxies not implementing equals
        expected = ModificationProxy.unwrap(expected);

        catalogTestSupport.assertEqualsLenientConnectionParameters(expected, roundtripped);
    }

    private void testPropertyDiff_InfoObject(Info expectedOldValue, Info expectedNewValue)
            throws IOException {
        PropertyDiff diff = support.createTestDiff("ws", expectedOldValue, expectedNewValue);
        String encoded = codec.encode(diff);
        PropertyDiff roundtripped = codec.decode(encoded);
        assertEquals(1, roundtripped.size());

        Info roundTrippedOldValue = (Info) roundtripped.get(0).getOldValue();
        Info roundTrippedNewValue = (Info) roundtripped.get(0).getNewValue();
        // help avoid false positives due to null collections
        if (roundTrippedOldValue != null) OwsUtils.resolveCollections(roundTrippedOldValue);
        if (roundTrippedNewValue != null) OwsUtils.resolveCollections(roundTrippedNewValue);

        // help avoid false positives due to modification proxies not implementing equals
        expectedOldValue = ModificationProxy.unwrap(expectedOldValue);
        expectedNewValue = ModificationProxy.unwrap(expectedNewValue);

        catalogTestSupport.assertEqualsLenientConnectionParameters(
                expectedOldValue, roundTrippedOldValue);
        catalogTestSupport.assertEqualsLenientConnectionParameters(
                expectedNewValue, roundTrippedNewValue);
    }
}
