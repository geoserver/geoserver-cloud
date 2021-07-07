/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.DataLinkInfoImpl;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wms.WMSInfo;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.VirtualTable;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.sort.SortOrder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import si.uom.SI;

/**
 * Verifies that all {@link CatalogInfo} can be sent over the wire and parsed back using jackson,
 * thanks to {@link GeoServerCatalogModule} jackcon-databind module
 */
@Slf4j
public class GeoServerCatalogModuleTest {

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    private Catalog catalog;
    private CatalogTestData data;
    private ObjectMapper objectMapper;
    private GeoServer geoserver;
    private ProxyUtils proxyResolver;

    public static @BeforeClass void oneTimeSetup() {
        // avoid the chatty warning logs due to catalog looking up a bean of type
        // GeoServerConfigurationLock
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    public @Before void before() {
        catalog = new CatalogPlugin();
        geoserver = new GeoServerImpl();
        data = CatalogTestData.initialized(() -> catalog, () -> geoserver).initialize();
        proxyResolver = new ProxyUtils(catalog, geoserver);

        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.findAndRegisterModules();
    }

    private <T extends CatalogInfo> void catalogInfoRoundtripTest(T orig)
            throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();

        ClassMappings classMappings = ClassMappings.fromImpl(orig.getClass());
        @SuppressWarnings("unchecked")
        Class<T> abstractType = (Class<T>) classMappings.getInterface();

        String encoded = writer.writeValueAsString(orig);
        log.info(encoded);
        T decoded = objectMapper.readValue(encoded, abstractType);
        // assert it can also be parsed using the generic CatalogInfo class
        CatalogInfo asCatalogInfo = objectMapper.readValue(encoded, CatalogInfo.class);
        assertNotNull(asCatalogInfo);
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

        data.assertEqualsLenientConnectionParameters(orig, decoded);
        data.assertInternationalStringPropertiesEqual(orig, decoded);
    }

    public @Test void testWorkspace() throws Exception {
        catalogInfoRoundtripTest(data.workspaceA);

        data.workspaceB.setIsolated(true);
        data.workspaceB.setDateCreated(new Date());
        data.workspaceB.setDateModified(new Date());
        catalogInfoRoundtripTest(data.workspaceB);
    }

    public @Test void testNamespace() throws Exception {
        catalogInfoRoundtripTest(data.namespaceA);

        data.namespaceB.setIsolated(true);
        data.namespaceB.setDateCreated(new Date());
        data.namespaceB.setDateModified(new Date());
        catalogInfoRoundtripTest(data.workspaceB);
    }

    public @Test void testDataStore() throws Exception {
        catalogInfoRoundtripTest(data.dataStoreA);
        catalogInfoRoundtripTest(data.dataStoreB);
        catalogInfoRoundtripTest(data.dataStoreC);
    }

    public @Test void testCoverageStore() throws Exception {
        catalogInfoRoundtripTest(data.coverageStoreA);
    }

    public @Test void testWmsStore() throws Exception {
        catalogInfoRoundtripTest(data.wmsStoreA);
    }

    public @Test void testWmtsStore() throws Exception {
        catalogInfoRoundtripTest(data.wmtsStoreA);
    }

    public @Test void testFeatureType() throws Exception {
        KeywordInfo k = new Keyword("value");
        k.setLanguage("es");
        FeatureTypeInfo ft = data.featureTypeA;
        ft.getKeywords().add(k);
        List<AttributeTypeInfo> attributes = createTestAttributes(ft);
        ft.getAttributes().addAll(attributes);

        ft.setTitle("Title");
        ft.setAbstract("abstract");
        ft.setInternationalTitle(
                data.createInternationalString(
                        Locale.ENGLISH, "english title", Locale.CANADA_FRENCH, "titre anglais"));
        ft.setInternationalAbstract(
                data.createInternationalString(
                        Locale.ENGLISH,
                        "english abstract",
                        Locale.CANADA_FRENCH,
                        "résumé anglais"));

        catalogInfoRoundtripTest(ft);
    }

    private List<AttributeTypeInfo> createTestAttributes(FeatureTypeInfo info)
            throws SchemaException {
        String typeSpec =
                "name:string,id:String,polygonProperty:Polygon:srid=32615,centroid:Point,url:java.net.URL,uuid:UUID";
        SimpleFeatureType ft = DataUtilities.createType("TestType", typeSpec);
        return new CatalogBuilder(new CatalogPlugin()).getAttributes(ft, info);
    }

    public @Test void testCoverage() throws Exception {
        catalogInfoRoundtripTest(data.coverageA);
    }

    public @Test void testWmsLayer() throws Exception {
        catalogInfoRoundtripTest(data.wmsLayerA);
    }

    public @Test void testWtmsLayer() throws Exception {
        catalogInfoRoundtripTest(data.wmtsLayerA);
    }

    public @Test void testLayer() throws Exception {
        LayerInfo layer = data.layerFeatureTypeA;
        layer.getStyles().add(data.style1);
        layer.getStyles().add(data.style2);
        catalogInfoRoundtripTest(layer);
    }

    public @Test void testLayerGroup() throws Exception {
        LayerGroupInfo lg = data.layerGroup1;
        lg.setTitle("LG Title");
        lg.setAbstract("LG abstract");
        lg.setInternationalTitle(
                data.createInternationalString(
                        Locale.ENGLISH, "english title", Locale.CANADA_FRENCH, "titre anglais"));
        lg.setInternationalAbstract(
                data.createInternationalString(
                        Locale.ENGLISH,
                        "english abstract",
                        Locale.CANADA_FRENCH,
                        "résumé anglais"));
        catalogInfoRoundtripTest(lg);
    }

    public @Test void testLayerGroupWorkspace() throws Exception {
        data.layerGroup1.setWorkspace(data.workspaceC);
        catalogInfoRoundtripTest(data.layerGroup1);
    }

    public @Test void testStyle() throws Exception {
        StyleInfo style1 = data.style1;
        style1.setFormatVersion(SLDHandler.VERSION_10);
        style1.setFormat(SLDHandler.FORMAT);

        StyleInfo style2 = data.style2;
        style1.setFormatVersion(SLDHandler.VERSION_11);
        style1.setFormat(SLDHandler.FORMAT);

        catalogInfoRoundtripTest(style1);
        catalogInfoRoundtripTest(style2);
    }

    public @Test void testStyleWorkspace() throws Exception {
        data.style1.setWorkspace(data.workspaceA);
        data.style2.setWorkspace(data.workspaceB);
        catalogInfoRoundtripTest(data.style1);
        catalogInfoRoundtripTest(data.style2);
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

    public @Test void testPatch() throws Exception {
        testPatch("nullvalue", null);
        testPatch("int", Integer.MAX_VALUE);
        testPatch("long", Long.MAX_VALUE);
        testPatch("date", new Date(10_000_000));
        testPatch("string", "string value");
        // some CatalogInfo properties, , shall be converted to references
        testPatch("workspace", data.workspaceA);
        testPatch("namespace", data.namespaceA);
        testPatch("dataStore", data.dataStoreA);
        testPatch("coverageStore", data.coverageStoreA);
        testPatch("layer", data.layerFeatureTypeA);
        testPatch("style", data.style1);
        // some Config Info properties, shall be converted to references
        testPatch("global", data.global);
        testPatch("logging", data.logging);
        // testPatch("settings", testData.workspaceASettings);
        testPatch("wmsService", data.wmsService);

        // some Info properties that are actually value-objects and shall be serialized
        testPatch("attribution", attInfo("attributionInfo1"));
        testPatch("contact", contact("acme"));
    }

    public @Test void testPatchWithModificationProxy() throws Exception {
        testPatch("workspace", ModificationProxy.create(data.workspaceA, WorkspaceInfo.class));
        testPatch("namespace", ModificationProxy.create(data.namespaceA, NamespaceInfo.class));
        testPatch("dataStore", ModificationProxy.create(data.dataStoreA, DataStoreInfo.class));
        testPatch(
                "coverageStore",
                ModificationProxy.create(data.coverageStoreA, CoverageStoreInfo.class));
        testPatch("layer", ModificationProxy.create(data.layerFeatureTypeA, LayerInfo.class));
        testPatch("style", ModificationProxy.create(data.style1, StyleInfo.class));
        // some Config Info properties, shall be converted to references
        testPatch("global", ModificationProxy.create(data.global, GeoServerInfo.class));
        testPatch("logging", ModificationProxy.create(data.logging, LoggingInfo.class));
        // testPatch("settings", testData.workspaceASettings);
        testPatch("wmsService", ModificationProxy.create(data.wmsService, WMSInfo.class));
    }

    public @Test void testPatchWithListProperty() throws Exception {
        testPatch("nullvalue", newArrayList(null, null));
        testPatch("int", newArrayList(Integer.MAX_VALUE, Integer.MIN_VALUE));
        testPatch("long", newArrayList(Long.MAX_VALUE, Long.MIN_VALUE));
        testPatch("date", newArrayList(new Date(10_000_000), new Date(11_000_000)));
        testPatch("string", newArrayList("string1", "string2"));
        // some CatalogInfo properties, , shall be converted to references
        testPatch("workspaces", newArrayList(data.workspaceA, data.workspaceB));
        testPatch("namespaces", newArrayList(data.namespaceA, data.namespaceB));
        testPatch("stores", newArrayList(data.dataStoreA, data.coverageStoreA));
        testPatch("layers", newArrayList(data.layerFeatureTypeA));
        testPatch("styles", newArrayList(data.style1, data.style2));

        testPatch("serviceInfos", newArrayList(data.wmsService, data.wfsService));

        testPatch(
                "attribution", newArrayList(attInfo("attributionInfo1"), attInfo("attribution2")));
        testPatch("contact", newArrayList(contact("org1"), contact("org2")));
    }

    public @Test void testPatchWithSetProperty() throws Exception {
        Set<WorkspaceInfo> workspaces =
                newLinkedHashSet(newArrayList(data.workspaceA, data.workspaceB));
        Set<NamespaceInfo> namespaces =
                newLinkedHashSet(newArrayList(data.namespaceA, data.namespaceB));
        Set<StoreInfo> stores =
                newLinkedHashSet(newArrayList(data.dataStoreA, data.coverageStoreA));
        Set<LayerInfo> layers = newLinkedHashSet(newArrayList(data.layerFeatureTypeA));
        Set<StyleInfo> styles = newLinkedHashSet(newArrayList(data.style1, data.style2));
        Set<ServiceInfo> services =
                newLinkedHashSet(newArrayList(data.wmsService, data.wfsService));
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
        log.info(encoded);
        Patch decoded = objectMapper.readValue(encoded, Patch.class);

        Patch resolved = proxyResolver.resolve(decoded);
        assertEquals(patch, resolved);
    }

    public @Test void testFilterWithInfoLiterals() throws JsonProcessingException {
        testFilterLiteral(data.workspaceA);
        testFilterLiteral(ModificationProxy.create(data.workspaceA, WorkspaceInfo.class));

        testFilterLiteral(data.namespaceA);
        testFilterLiteral(ModificationProxy.create(data.namespaceA, NamespaceInfo.class));

        testFilterLiteral(data.dataStoreA);
        testFilterLiteral(ModificationProxy.create(data.dataStoreA, DataStoreInfo.class));

        testFilterLiteral(data.coverageStoreA);
        testFilterLiteral(ModificationProxy.create(data.coverageStoreA, CoverageStoreInfo.class));

        testFilterLiteral(data.wmsStoreA);
        testFilterLiteral(ModificationProxy.create(data.wmsStoreA, WMSStoreInfo.class));

        testFilterLiteral(data.wmtsStoreA);
        testFilterLiteral(ModificationProxy.create(data.wmtsStoreA, WMTSStoreInfo.class));

        testFilterLiteral(data.featureTypeA);
        testFilterLiteral(ModificationProxy.create(data.featureTypeA, FeatureTypeInfo.class));

        testFilterLiteral(data.coverageA);
        testFilterLiteral(ModificationProxy.create(data.coverageA, CoverageInfo.class));

        testFilterLiteral(data.wmsLayerA);
        testFilterLiteral(ModificationProxy.create(data.wmsLayerA, WMSLayerInfo.class));

        testFilterLiteral(data.wmtsLayerA);
        testFilterLiteral(ModificationProxy.create(data.wmtsLayerA, WMTSLayerInfo.class));

        testFilterLiteral(data.layerFeatureTypeA);
        testFilterLiteral(ModificationProxy.create(data.layerFeatureTypeA, LayerInfo.class));

        testFilterLiteral(data.layerGroup1);
        testFilterLiteral(ModificationProxy.create(data.layerGroup1, LayerGroupInfo.class));

        testFilterLiteral(data.style1);
        testFilterLiteral(ModificationProxy.create(data.style1, StyleInfo.class));
    }

    private <T> T testFilterLiteral(T value) throws JsonProcessingException {
        Class<? extends Object> expectedDecodedType = value.getClass();
        if (value instanceof Proxy) {
            T unwrap = ModificationProxy.unwrap(value);
            expectedDecodedType = unwrap.getClass();
        }

        PropertyIsEqualTo filter = equals("literalTestProp", value);
        PropertyIsEqualTo decodedFilter = roundTrip(filter, Filter.class);
        assertEquals(filter.getExpression1(), decodedFilter.getExpression1());
        // can't trust the equals() implementation on the provided object, make some basic checks
        // and return the decoded object
        Literal decodedExp = (Literal) decodedFilter.getExpression2();
        Object decodedValue = decodedExp.getValue();
        assertNotNull(decodedValue);
        assertThat(decodedValue, instanceOf(expectedDecodedType));

        filter = equals("collectionProp", Arrays.asList(value, value));
        decodedFilter = roundTrip(filter, Filter.class);
        assertEquals(filter.getExpression1(), decodedFilter.getExpression1());
        Expression decodedListLiteral = decodedFilter.getExpression2();
        assertTrue(decodedListLiteral instanceof Literal);

        decodedValue = ((Literal) decodedListLiteral).getValue();
        assertTrue(decodedValue instanceof List);

        @SuppressWarnings("unchecked")
        List<T> decodedList = (List<T>) decodedValue;
        assertEquals(2, decodedList.size());
        assertThat(decodedList.get(0), instanceOf(expectedDecodedType));
        assertThat(decodedList.get(1), instanceOf(expectedDecodedType));
        return decodedList.get(0);
    }

    private PropertyIsEqualTo equals(String propertyName, Object literal) {
        return ff.equals(ff.property(propertyName), ff.literal(literal));
    }

    private <T> T roundTrip(T orig, Class<? super T> clazz) throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();
        String encoded = writer.writeValueAsString(orig);
        log.info(encoded);
        @SuppressWarnings("unchecked")
        T decoded = (T) objectMapper.readValue(encoded, clazz);
        return decoded;
    }

    public @Test void testValueKeywordInfo() throws JsonProcessingException {
        KeywordInfo keyword = new org.geoserver.catalog.Keyword("value");
        keyword.setLanguage("en");
        keyword.setVocabulary("bad");

        assertEquals(keyword, roundTrip(keyword, KeywordInfo.class));
        assertEquals(keyword, testFilterLiteral(keyword));
    }

    public @Test void testValueCoordinateReferenceSystemGeographicLatLon() throws Exception {
        CoordinateReferenceSystem wgs84LatLon = CRS.decode("EPSG:4326", false);
        testValueCoordinateReferenceSystem(wgs84LatLon);
    }

    public @Test void testValueCoordinateReferenceSystemGeographicLonLat() throws Exception {
        CoordinateReferenceSystem wgs84LonLat = CRS.decode("EPSG:4326", true);
        testValueCoordinateReferenceSystem(wgs84LonLat);
    }

    public @Test void testValueCoordinateReferenceSystemProjected() throws Exception {
        CoordinateReferenceSystem webMercator = CRS.decode("EPSG:3857", true);
        testValueCoordinateReferenceSystem(webMercator);
    }

    public @Test void testValueCoordinateReferenceSystemCustomCRS() throws Exception {
        String customWKT =
                "PROJCS[ \"UTM Zone 10, Northern Hemisphere\",\n"
                        + "  GEOGCS[\"GRS 1980(IUGG, 1980)\",\n"
                        + "    DATUM[\"unknown\","
                        + "       SPHEROID[\"GRS80\",6378137,298.257222101],"
                        + "       TOWGS84[0,0,0,0,0,0,0]"
                        + "    ],\n"
                        + "    PRIMEM[\"Greenwich\",0],\n"
                        + "    UNIT[\"degree\",0.0174532925199433]\n"
                        + "  ],\n"
                        + "  PROJECTION[\"Transverse_Mercator\"],\n"
                        + "  PARAMETER[\"latitude_of_origin\",0],\n"
                        + "  PARAMETER[\"central_meridian\",-123],\n"
                        + "  PARAMETER[\"scale_factor\",0.9996],\n"
                        + "  PARAMETER[\"false_easting\",1640419.947506562],\n"
                        + "  PARAMETER[\"false_northing\",0],\n"
                        + "  UNIT[\"Foot (International)\",0.3048]\n"
                        + "]";

        CoordinateReferenceSystem crs = CRS.parseWKT(customWKT);
        testValueCoordinateReferenceSystem(crs);
    }

    private void testValueCoordinateReferenceSystem(CoordinateReferenceSystem crs)
            throws Exception {
        CoordinateReferenceSystem decoded = roundTrip(crs, CoordinateReferenceSystem.class);
        assertTrue(CRS.equalsIgnoreMetadata(crs, decoded));
        decoded = testFilterLiteral(crs);
        assertTrue(CRS.equalsIgnoreMetadata(crs, decoded));
    }

    /**
     * Does not perform equals check, for value types that don't implement {@link
     * Object#equals(Object)} or have misbehaving implementations
     */
    private <T> T testValue(final T value, Class<T> type) throws Exception {
        T decoded = roundTrip(value, type);
        decoded = testFilterLiteral(value);
        return decoded;
    }

    private <T> void testValueWithEquals(final T value, Class<T> type) throws Exception {
        T decoded = roundTrip(value, type);
        assertEquals(value, decoded);
        decoded = testFilterLiteral(value);
        assertEquals(value, decoded);
    }

    public @Test void testValueNumberRange() throws Exception {
        testValueWithEquals(NumberRange.create(Double.MIN_VALUE, 0d), NumberRange.class);
        testValueWithEquals(NumberRange.create(0L, false, Long.MAX_VALUE, true), NumberRange.class);
        testValueWithEquals(
                NumberRange.create(Integer.MIN_VALUE, true, Integer.MAX_VALUE, false),
                NumberRange.class);
    }

    public @Test void testValueMeasure() throws Exception {
        testValueWithEquals(new Measure(1000, SI.METRE), Measure.class);
        testValueWithEquals(new Measure(.75, SI.RADIAN_PER_SECOND), Measure.class);
    }

    public @Test void testValueReferencedEnvelope() throws Exception {
        CoordinateReferenceSystem wgs84LatLon = CRS.decode("EPSG:4326", false);
        CoordinateReferenceSystem wgs84LonLat = CRS.decode("EPSG:4326", true);

        testValueWithEquals(
                new ReferencedEnvelope(-180, 180, -90, 90, wgs84LonLat), ReferencedEnvelope.class);
        testValueWithEquals(
                new ReferencedEnvelope(-90, 90, -180, 180, wgs84LatLon), ReferencedEnvelope.class);
    }

    public @Test void testValueGridGeometry2D() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope env = new ReferencedEnvelope(-180, 180, -90, 90, crs);
        GridEnvelope range = new GeneralGridEnvelope(new int[] {0, 0}, new int[] {1024, 768});
        GridGeometry2D gridGeometry = new GridGeometry2D(range, env);
        testValueWithEquals(gridGeometry, GridGeometry.class);
    }

    public @Test void testValueAuthorityURLInfo() throws Exception {
        AuthorityURL info = new AuthorityURL();
        info.setHref("href");
        info.setName("name");
        testValueWithEquals(info, AuthorityURLInfo.class);
    }

    public @Test void testValueCoverageDimensionInfo() throws Exception {
        CoverageDimensionImpl c = new CoverageDimensionImpl();
        c.setDescription("description");
        c.setDimensionType(SampleDimensionType.UNSIGNED_1BIT);
        c.setId("id");
        c.setName("name");
        c.setNullValues(Arrays.asList(0.0)); // , Double.NEGATIVE_INFINITY,
        // Double.POSITIVE_INFINITY));
        c.setRange(NumberRange.create(0.0, 255.0));
        c.setUnit("unit");
        testValueWithEquals(c, CoverageDimensionInfo.class);
    }

    public @Test void testValueDimensionInfo() throws Exception {
        DimensionInfoImpl di = new DimensionInfoImpl();
        di.setAcceptableInterval("searchRange");
        di.setAttribute("attribute");
        DimensionDefaultValueSetting defaultValue = new DimensionDefaultValueSetting();
        defaultValue.setReferenceValue("referenceValue");
        defaultValue.setStrategyType(Strategy.MAXIMUM);
        di.setDefaultValue(defaultValue);
        di.setEnabled(true);
        di.setNearestMatchEnabled(true);
        di.setResolution(BigDecimal.TEN);
        di.setUnits("units");
        di.setUnitSymbol("unitSymbol");
        di.setPresentation(DimensionPresentation.DISCRETE_INTERVAL);

        // bad equals implementation on DimensionInfoImpl
        DimensionInfo actual = testValue(di, DimensionInfo.class);
        assertEquals(di.getAcceptableInterval(), actual.getAcceptableInterval());
        assertEquals(di.getAttribute(), actual.getAttribute());
        assertEquals(
                di.getDefaultValue().getReferenceValue(),
                actual.getDefaultValue().getReferenceValue());
        assertEquals(
                di.getDefaultValue().getStrategyType(), actual.getDefaultValue().getStrategyType());
        assertEquals(di.isEnabled(), actual.isEnabled());
        assertEquals(di.isNearestMatchEnabled(), actual.isNearestMatchEnabled());
        assertEquals(di.isRawNearestMatchEnabled(), actual.isRawNearestMatchEnabled());
        assertEquals(di.getResolution(), actual.getResolution());
        assertEquals(di.getUnits(), actual.getUnits());
        assertEquals(di.getUnitSymbol(), actual.getUnitSymbol());
        assertEquals(di.getPresentation(), actual.getPresentation());
    }

    public @Test void testValueDataLinkInfo() throws Exception {
        DataLinkInfoImpl dl = new DataLinkInfoImpl();
        dl.setAbout("about");
        dl.setContent("content");
        dl.setId("id");
        dl.setType("type");
        testValueWithEquals(dl, DataLinkInfo.class);
    }

    public @Test void testValueLayerIdentifierInfo() throws Exception {
        org.geoserver.catalog.impl.LayerIdentifier li = new LayerIdentifier();
        li.setAuthority("authorityName");
        li.setIdentifier("identifier");
        testValueWithEquals(li, LayerIdentifierInfo.class);
    }

    public @Test void testValueLegendInfo() throws Exception {
        LegendInfoImpl l = new LegendInfoImpl();
        l.setFormat("format");
        l.setHeight(10);
        l.setWidth(20);
        l.setId("id");
        l.setOnlineResource("onlineResource");
        // LegendInfoImpl does not implement equals
        LegendInfo parsed = testValue(l, LegendInfo.class);
        assertEquals(l.getFormat(), parsed.getFormat());
        assertEquals(l.getHeight(), parsed.getHeight());
        assertEquals(l.getWidth(), parsed.getWidth());
        assertEquals(l.getId(), parsed.getId());
        assertEquals(l.getOnlineResource(), parsed.getOnlineResource());
    }

    public @Test void testValueMetadataLinkInfo() throws Exception {
        MetadataLinkInfoImpl link = new MetadataLinkInfoImpl();
        link.setAbout("about");
        link.setContent("content");
        link.setId("id");
        link.setMetadataType("metadataType");
        link.setType("type");
        testValueWithEquals(link, MetadataLinkInfo.class);
    }

    public @Test void testValueVirtualTable() throws Exception {
        VirtualTable vt = new VirtualTable("testvt", "select * from test;", true);
        testValueWithEquals(vt, VirtualTable.class);
    }

    public @Test void testQuery() throws Exception {
        Arrays.stream(ClassMappings.values())
                .map(ClassMappings::getInterface)
                .filter(CatalogInfo.class::isAssignableFrom)
                .forEach(this::testQuery);
    }

    @SuppressWarnings("unchecked")
    private void testQuery(Class<?> clazz) {
        Class<? extends CatalogInfo> type = (Class<? extends CatalogInfo>) clazz;
        try {
            Query<?> query = Query.all(type);
            Query<?> parsed = testValue(query, Query.class);
            assertNotNull(parsed);
            assertQueryEquals(query, parsed);
            Filter filter =
                    equals("some.property.name", Arrays.asList("some literal 1", "some literal 2"));
            query =
                    Query.valueOf(
                            type,
                            filter,
                            2000,
                            1000,
                            ff.sort("name", SortOrder.ASCENDING),
                            ff.sort("type", SortOrder.DESCENDING));
            parsed = testValue(query, Query.class);
            assertNotNull(parsed);
            assertQueryEquals(query, parsed);
        } catch (Exception e) {
            fail(e);
        }
    }

    // needed instead of Query.equals(query), no equals() implementation in
    // org.geotools.filter.SortByImpl nor Filter...
    private void assertQueryEquals(Query<?> query, Query<?> parsed) {
        assertEquals(query.getType(), parsed.getType());
        assertEquals(query.getCount(), parsed.getCount());
        assertEquals(query.getOffset(), parsed.getOffset());

        Filter f1 = query.getFilter();
        Filter f2 = parsed.getFilter();
        if (f1 == Filter.INCLUDE) assertEquals(Filter.INCLUDE, f2);
        else {
            PropertyIsEqualTo p1 = (PropertyIsEqualTo) f1;
            PropertyIsEqualTo p2 = (PropertyIsEqualTo) f2;
            assertEquals(p1.getExpression1(), p2.getExpression1());
            assertEquals(p1.getExpression2(), p2.getExpression2());
        }
        assertEquals(query.getSortBy(), parsed.getSortBy());
    }
}
