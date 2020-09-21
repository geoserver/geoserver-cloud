/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Sets.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.Date;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.test.CatalogTestData;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ContactInfoImpl;
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
    private GeoServer geoserver;
    private ProxyUtils proxyResolver;

    public static @BeforeClass void oneTimeSetup() {
        // avoid the chatty warning logs due to catalog looking up a bean of type
        // GeoServerConfigurationLock
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    public @Before void before() {
        catalog = new CatalogImpl();
        testData = CatalogTestData.initialized(() -> catalog).initCatalog();
        geoserver = testData.initConfig().getConfigCatalog();
        proxyResolver = new ProxyUtils(catalog, geoserver);

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
        decoded = proxyResolver.resolve(decoded);

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
        StyleInfo style1 = testData.style1;
        style1.setFormatVersion(SLDHandler.VERSION_10);
        style1.setFormat(SLDHandler.FORMAT);

        StyleInfo style2 = testData.style2;
        style1.setFormatVersion(SLDHandler.VERSION_11);
        style1.setFormat(SLDHandler.FORMAT);

        roundtripTest(style1);
        roundtripTest(style2);
    }

    public @Test void testStyleWorkspace() throws Exception {
        testData.style1.setWorkspace(testData.workspaceA);
        testData.style2.setWorkspace(testData.workspaceB);
        roundtripTest(testData.style1);
        roundtripTest(testData.style2);
    }

    public @Test void testPatch() throws Exception {

        testPatch("nullvalue", null);
        testPatch("int", Integer.MAX_VALUE);
        testPatch("long", Long.MAX_VALUE);
        testPatch("date", new Date(10_000_000));
        testPatch("string", "string value");
        // some CatalogInfo properties, , shall be converted to references
        testPatch("workspace", testData.workspaceA);
        testPatch("namespace", testData.namespaceA);
        testPatch("dataStore", testData.dataStoreA);
        testPatch("coverageStore", testData.coverageStoreA);
        testPatch("layer", testData.layerFeatureTypeA);
        testPatch("style", testData.style1);
        // some Config Info properties, shall be converted to references
        testPatch("global", testData.global);
        testPatch("logging", testData.logging);
        // testPatch("settings", testData.workspaceASettings);
        testPatch("wmsService", testData.wmsService);

        // some Info properties that are actually value-objects and shall be serialized
        testPatch("attribution", attInfo("attributionInfo1"));
        testPatch("contact", contact("acme"));
    }

    protected ContactInfoImpl contact(String org) {
        ContactInfoImpl contact = new ContactInfoImpl();
        contact.setContactOrganization(org);
        contact.setContactPerson("myself");
        return contact;
    }

    protected AttributionInfoImpl attInfo(String id) throws Exception {
        AttributionInfoImpl attinfo = new AttributionInfoImpl();
        attinfo.setId(id);
        attinfo.setHref("http://nevermind");
        attinfo.setLogoWidth(10);
        return attinfo;
    }

    public @Test void testPatchWithListProperty() throws Exception {
        testPatch("nullvalue", newArrayList(null, null));
        testPatch("int", newArrayList(Integer.MAX_VALUE, Integer.MIN_VALUE));
        testPatch("long", newArrayList(Long.MAX_VALUE, Long.MIN_VALUE));
        testPatch("date", newArrayList(new Date(10_000_000), new Date(11_000_000)));
        testPatch("string", newArrayList("string1", "string2"));
        // some CatalogInfo properties, , shall be converted to references
        testPatch("workspaces", newArrayList(testData.workspaceA, testData.workspaceB));
        testPatch("namespaces", newArrayList(testData.namespaceA, testData.namespaceB));
        testPatch("stores", newArrayList(testData.dataStoreA, testData.coverageStoreA));
        testPatch("layers", newArrayList(testData.layerFeatureTypeA));
        testPatch("styles", newArrayList(testData.style1, testData.style2));

        testPatch("serviceInfos", newArrayList(testData.wmsService, testData.wfsService));

        testPatch(
                "attribution", newArrayList(attInfo("attributionInfo1"), attInfo("attribution2")));
        testPatch("contact", newArrayList(contact("org1"), contact("org2")));
    }

    public @Test void testPatchWithSetProperty() throws Exception {
        Set<WorkspaceInfo> workspaces =
                newLinkedHashSet(newArrayList(testData.workspaceA, testData.workspaceB));
        Set<NamespaceInfo> namespaces =
                newLinkedHashSet(newArrayList(testData.namespaceA, testData.namespaceB));
        Set<StoreInfo> stores =
                newLinkedHashSet(newArrayList(testData.dataStoreA, testData.coverageStoreA));
        Set<LayerInfo> layers = newLinkedHashSet(newArrayList(testData.layerFeatureTypeA));
        Set<StyleInfo> styles = newLinkedHashSet(newArrayList(testData.style1, testData.style2));
        Set<ServiceInfo> services =
                newLinkedHashSet(newArrayList(testData.wmsService, testData.wfsService));
        Set<AttributionInfoImpl> attributionInfos =
                newLinkedHashSet(
                        newArrayList(attInfo("attributionInfo1"), attInfo("attribution2")));
        Set<ContactInfoImpl> contactInfos =
                newLinkedHashSet(newArrayList(contact("org1"), contact("org2")));

        testPatch("workspaces", workspaces);
        testPatch("namespaces", namespaces);
        testPatch("stores", stores);
        testPatch("layers", layers);
        testPatch("styles", styles);
        testPatch("serviceInfos", services);
        testPatch("attribution", attributionInfos);
        testPatch("contact", contactInfos);
    }

    private void testPatch(String name, Object value) throws Exception {
        Patch patch = new Patch();
        patch.add(name, value);

        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();
        String encoded = writer.writeValueAsString(patch);
        System.out.println(encoded);
        Patch decoded = objectMapper.readValue(encoded, Patch.class);

        Patch resolved = proxyResolver.resolve(decoded);
        assertEquals(patch, resolved);
        //
        // // assert it can also be parsed using the generic CatalogInfo class
        // CatalogInfo asCatalogInfo = objectMapper.readValue(encoded, CatalogInfo.class);
        // // and also as its direct super-type, if it not CatalogInfo
        // if (orig instanceof StoreInfo)
        // assertNotNull(objectMapper.readValue(encoded, StoreInfo.class));
        // if (orig instanceof ResourceInfo)
        // assertNotNull(objectMapper.readValue(encoded, ResourceInfo.class));
        // if (orig instanceof PublishedInfo)
        // assertNotNull(objectMapper.readValue(encoded, PublishedInfo.class));
        //
        // // This is the client code's responsibility, the Deserializer returns "resolving
        // proxy"
        // // proxies for Info references
        // decoded = new ProxyUtils(catalog).resolve(decoded);
        //
        // testData.assertEqualsLenientConnectionParameters(orig, decoded);
    }
}
