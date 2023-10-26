/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.cog.CogSettings.RangeReaderType;
import org.geoserver.cog.CogSettingsStore;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.VirtualTable;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import si.uom.SI;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Verifies that all {@link CatalogInfo} can be sent over the wire and parsed back using jackson,
 * thanks to {@link GeoServerCatalogModule} jackcon-databind module
 */
@Slf4j
public abstract class GeoServerCatalogModuleTest {

    private FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected void print(String logmsg, Object... args) {
        boolean debug = Boolean.getBoolean("debug");
        if (debug) log.info(logmsg, args);
    }

    private ObjectMapper objectMapper;

    private Catalog catalog;
    private CatalogTestData data;
    private GeoServer geoserver;
    private ProxyUtils proxyResolver;

    public static @BeforeAll void oneTimeSetup() {
        // avoid the chatty warning logs due to catalog looking up a bean of type
        // GeoServerConfigurationLock
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    public @BeforeEach void before() {
        objectMapper = newObjectMapper();
        catalog = new CatalogPlugin();
        geoserver = new GeoServerImpl();
        geoserver.setCatalog(catalog);
        data = CatalogTestData.initialized(() -> catalog, () -> geoserver).initialize();
        proxyResolver = new ProxyUtils(catalog, Optional.of(geoserver));
    }

    protected abstract ObjectMapper newObjectMapper();

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> T catalogInfoRoundtripTest(final T orig)
            throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();

        T unproxied = ModificationProxy.unwrap(orig);

        ClassMappings classMappings = ClassMappings.fromImpl(unproxied.getClass());

        Class<T> abstractType = (Class<T>) classMappings.getInterface();

        String encoded = writer.writeValueAsString(orig);
        print(encoded);
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

        if (Proxy.isProxyClass(orig.getClass())) {
            ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(orig);
            proxy.commit();
            unproxied = (T) proxy.getProxyObject();
        }

        data.assertEqualsLenientConnectionParameters(unproxied, decoded);
        data.assertInternationalStringPropertiesEqual(unproxied, decoded);

        return decoded;
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

    public @Test void testCoverageStore_COG() throws Exception {
        CoverageStoreInfo store = data.coverageStoreA;
        CogSettingsStore cogSettings = new CogSettingsStore();
        cogSettings.setRangeReaderSettings(RangeReaderType.Azure);
        cogSettings.setUseCachingStream(true);
        store.getMetadata().put("cogSettings", cogSettings);

        CoverageStoreInfo decoded = catalogInfoRoundtripTest(store);

        Serializable deserializedCogSettings = decoded.getMetadata().get("cogSettings");
        assertThat(deserializedCogSettings, CoreMatchers.instanceOf(CogSettingsStore.class));
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
                data.faker()
                        .internationalString(
                                Locale.ENGLISH,
                                "english title",
                                Locale.CANADA_FRENCH,
                                "titre anglais"));
        ft.setInternationalAbstract(
                data.faker()
                        .internationalString(
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
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(ft);
        builder.add("boola", boolean[].class);
        builder.add("bytea", byte[].class);
        builder.add("shorta", short[].class);
        builder.add("inta", int[].class);
        builder.add("longa", long[].class);
        builder.add("floata", float[].class);
        builder.add("doublea", double[].class);
        builder.add("stringa", String[].class);
        ft = builder.buildFeatureType();
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
                data.faker()
                        .internationalString(
                                Locale.ENGLISH,
                                "english title",
                                Locale.CANADA_FRENCH,
                                "titre anglais"));
        lg.setInternationalAbstract(
                data.faker()
                        .internationalString(
                                Locale.ENGLISH,
                                "english abstract",
                                Locale.CANADA_FRENCH,
                                "résumé anglais"));

        LayerGroupStyle lgs = new LayerGroupStyleImpl();
        lgs.setId("lgsid");
        lgs.setTitle("Lgs Title");
        lgs.setAbstract("Lgs Abstract");
        lgs.setInternationalTitle(
                data.faker()
                        .internationalString(
                                Locale.ITALIAN, "Italian title", Locale.FRENCH, "French title"));
        lgs.setInternationalAbstract(
                data.faker()
                        .internationalString(
                                Locale.ITALIAN,
                                "Italian abstract",
                                Locale.FRENCH,
                                "French abstract"));

        lgs.setLayers(Arrays.asList(data.createLayer(data.coverageA, data.style1)));
        lgs.setStyles(Arrays.asList(data.createStyle("test-style")));

        lg.setLayerGroupStyles(Arrays.asList(lgs));

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

    public @Test void testFilterWithInfoLiterals() throws JsonProcessingException {
        testFilterLiteral(forceNonProxy(data.workspaceA));
        testFilterLiteral(forceProxy(data.workspaceA));

        testFilterLiteral(forceNonProxy(data.namespaceA));
        testFilterLiteral(forceProxy(data.namespaceA));

        testFilterLiteral(forceNonProxy(data.dataStoreA));
        testFilterLiteral(forceProxy(data.dataStoreA));

        testFilterLiteral(forceNonProxy(data.coverageStoreA));
        testFilterLiteral(forceProxy(data.coverageStoreA));

        testFilterLiteral(forceNonProxy(data.wmsStoreA));
        testFilterLiteral(forceProxy(data.wmsStoreA));

        testFilterLiteral(forceNonProxy(data.wmtsStoreA));
        testFilterLiteral(forceProxy(data.wmtsStoreA));

        testFilterLiteral(forceNonProxy(data.featureTypeA));
        testFilterLiteral(forceProxy(data.featureTypeA));

        testFilterLiteral(forceNonProxy(data.coverageA));
        testFilterLiteral(forceProxy(data.coverageA));

        testFilterLiteral(forceNonProxy(data.wmsLayerA));
        testFilterLiteral(forceProxy(data.wmsLayerA));

        testFilterLiteral(forceNonProxy(data.wmtsLayerA));
        testFilterLiteral(forceProxy(data.wmtsLayerA));

        testFilterLiteral(forceNonProxy(data.layerFeatureTypeA));
        testFilterLiteral(forceProxy(data.layerFeatureTypeA));

        testFilterLiteral(forceNonProxy(data.layerGroup1));
        testFilterLiteral(forceProxy(data.layerGroup1));

        testFilterLiteral(forceNonProxy(data.style1));
        testFilterLiteral(forceProxy(data.style1));
    }

    @SuppressWarnings("unchecked")
    private <T extends Info> T forceProxy(T info) {
        if (!Proxy.isProxyClass(info.getClass())) {
            Class<? extends Info> iface = ClassMappings.fromImpl(info.getClass()).getInterface();
            return (T) ModificationProxy.create(info, iface);
        }
        return info;
    }

    private <T extends CatalogInfo> T forceNonProxy(T info) {
        return ModificationProxy.unwrap(info);
    }

    private <T> T testFilterLiteral(T value) throws JsonProcessingException {
        Class<? extends Object> expectedDecodedType = value.getClass();
        if (value instanceof Proxy) {
            T unwrap = ModificationProxy.unwrap(value);
            expectedDecodedType = unwrap.getClass();
        }

        return testFilterLiteral(value, expectedDecodedType);
    }

    protected <T> T testFilterLiteral(T value, Class<? extends Object> expectedDecodedType)
            throws JsonProcessingException {
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
        return roundTrip(orig, clazz, clazz);
    }

    private <T, V> V roundTrip(T orig, Class<? super T> source, Class<? super V> target)
            throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();
        String encoded = writer.writeValueAsString(orig);
        print("encoded: {}", encoded);
        @SuppressWarnings("unchecked")
        V decoded = (V) objectMapper.readValue(encoded, target);
        print("decoded: {}", decoded);
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
        CoverageDimensionInfo cdi = data.faker().coverageDimensionInfo();
        testValueWithEquals(cdi, CoverageDimensionInfo.class);
    }

    public @Test void testValueDimensionInfo() throws Exception {
        DimensionInfo di = data.faker().dimensionInfo();

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
        DataLinkInfo dl = data.faker().dataLinkInfo();
        testValueWithEquals(dl, DataLinkInfo.class);
    }

    public @Test void testValueLayerIdentifierInfo() throws Exception {
        org.geoserver.catalog.impl.LayerIdentifier li = data.faker().layerIdentifierInfo();
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
        testValueWithEquals(data.faker().metadataLink(), MetadataLinkInfo.class);
    }

    public @Test void testValueVirtualTable() throws Exception {
        VirtualTable vt = new VirtualTable("testvt", "select * from test;", true);
        testValueWithEquals(vt, VirtualTable.class);
    }

    public @Test void testValueMetadataMap() throws Exception {
        MetadataMap mdm = new MetadataMap();
        mdm.put("k1", "v1");
        mdm.put("k2", "v2");
        mdm.put("k3", null);

        var decoded = roundTrip(mdm, MetadataMap.class);
        assertEquals(mdm, decoded);
        decoded = testFilterLiteral(mdm, Map.class);
        assertEquals(mdm, decoded);
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
