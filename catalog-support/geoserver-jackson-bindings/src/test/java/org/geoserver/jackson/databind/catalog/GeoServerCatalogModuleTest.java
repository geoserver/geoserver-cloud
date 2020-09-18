/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.Date;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.cloud.test.CatalogTestData;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that all {@link CatalogInfo} can be sent over the wire and parsed back using jackson,
 * thanks to {@link GeoServerCatalogModule} jackcon-databind module
 */
public class GeoServerCatalogModuleTest {

    private Catalog catalog;
    private CatalogTestData testData;
    private ObjectMapper objectMapper;

    public static @BeforeClass void oneTimeSetup() {
        // avoid the chatty warning logs due to catalog looking up a bean of type
        // GeoServerConfigurationLock
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    public @Before void before() {
        catalog = new CatalogImpl();
        testData = CatalogTestData.initialized(() -> catalog).initCatalog();

        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.findAndRegisterModules();
    }

    private <T extends CatalogInfo> void roundtripTest(T orig) throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();

        ClassMappings classMappings = ClassMappings.fromImpl(orig.getClass());
        Class<T> abstractType = classMappings.getInterface();

        String encoded = writer.writeValueAsString(orig);
        System.out.println(encoded);
        T decoded = objectMapper.readValue(encoded, abstractType);
        // assert it can also be parsed using the generic CatalogInfo class
        CatalogInfo asCatalogInfo = objectMapper.readValue(encoded, CatalogInfo.class);
        // and also as its direct super-type, if it not CatalogInfo
        if (orig instanceof StoreInfo)
            assertNotNull(objectMapper.readValue(encoded, StoreInfo.class));
        if (orig instanceof ResourceInfo)
            assertNotNull(objectMapper.readValue(encoded, ResourceInfo.class));
        if (orig instanceof PublishedInfo)
            assertNotNull(objectMapper.readValue(encoded, PublishedInfo.class));

        // This is the client code's responsibility, the Deserializer returns "resolving proxy"
        // proxies for Info references
        decoded = new ProxyUtils(catalog).resolve(decoded);

        testData.assertEqualsLenientConnectionParameters(orig, decoded);
    }

    public @Test void testWorkspace() throws Exception {
        roundtripTest(testData.workspaceA);

        testData.workspaceB.setIsolated(true);
        testData.workspaceB.setDateCreated(new Date());
        testData.workspaceB.setDateModified(new Date());
        roundtripTest(testData.workspaceB);
    }

    public @Test void testNamespace() throws Exception {
        roundtripTest(testData.namespaceA);

        testData.namespaceB.setIsolated(true);
        testData.namespaceB.setDateCreated(new Date());
        testData.namespaceB.setDateModified(new Date());
        roundtripTest(testData.workspaceB);
    }

    public @Test void testDataStore() throws Exception {
        roundtripTest(testData.dataStoreA);
        roundtripTest(testData.dataStoreB);
        roundtripTest(testData.dataStoreC);
    }

    public @Test void testCoverageStore() throws Exception {
        roundtripTest(testData.coverageStoreA);
    }

    public @Test void testWmsStore() throws Exception {
        roundtripTest(testData.wmsStoreA);
    }

    public @Test void testWmtsStore() throws Exception {
        roundtripTest(testData.wmtsStoreA);
    }

    public @Test void testFeatureType() throws Exception {
        roundtripTest(testData.featureTypeA);
    }

    public @Test void testCoverage() throws Exception {
        roundtripTest(testData.coverageA);
    }

    public @Test void testWmsLayer() throws Exception {
        roundtripTest(testData.wmsLayerA);
    }

    public @Test void testWtmsLayer() throws Exception {
        roundtripTest(testData.wmtsLayerA);
    }

    public @Test void testLayer() throws Exception {
        roundtripTest(testData.layerFeatureTypeA);
    }

    public @Test void testLayerGroup() throws Exception {
        roundtripTest(testData.layerGroup1);
    }

    public @Test void testLayerGroupWorkspace() throws Exception {
        testData.layerGroup1.setWorkspace(testData.workspaceC);
        roundtripTest(testData.layerGroup1);
    }

    public @Test void testStyle() throws Exception {
        roundtripTest(testData.style1);
        roundtripTest(testData.style2);
    }

    public @Test void testStyleWorkspace() throws Exception {
        testData.style1.setWorkspace(testData.workspaceA);
        testData.style2.setWorkspace(testData.workspaceB);
        roundtripTest(testData.style1);
        roundtripTest(testData.style2);
    }
}
