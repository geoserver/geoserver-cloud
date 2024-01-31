/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import static com.google.common.collect.Lists.newArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.cog.CogSettings;
import org.geoserver.cog.CogSettings.RangeReaderType;
import org.geoserver.cog.CogSettingsStore;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.geoserver.jackson.databind.catalog.mapper.GeoServerValueObjectsMapper;
import org.geoserver.jackson.databind.config.dto.mapper.GeoServerConfigMapper;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import si.uom.SI;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Verifies that {@link Patch patches} can be JSON round-tripped. As a reference, it should cover as
 * much of {@link SharedMappers}, {@link GeoServerValueObjectsMapper}, {@link
 * GeoServerConfigMapper}, and {@link CatalogInfoMapper} as possible.
 */
@Slf4j
public abstract class PatchSerializationTest {

    protected void print(String logmsg, Object... args) {
        boolean debug = Boolean.getBoolean("debug");
        if (debug) log.info(logmsg, args);
    }

    public ObjectMapper objectMapper;

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
        proxyResolver = new ProxyUtils(() -> catalog, Optional.of(geoserver));
    }

    protected abstract ObjectMapper newObjectMapper();

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

    @Test
    void simpleTypes() throws Exception {
        testPatch("nullvalue", null);
        testPatch("int", Integer.MAX_VALUE);
        testPatch("long", Long.MAX_VALUE);
        testPatch("date", new Date(10_000_000));
        testPatch("string", "string value");
    }

    @Test
    void arrayTypes_scalar() throws Exception {
        testPatch("bytea", new byte[] {0x01, 0x02, 0x03});
        testPatch("booleana", new boolean[] {false, true, false});
        testPatch("chararray", new char[] {'a', 'b', 'c', 'd'});
    }

    @Test
    void arrayTypes_non_scalar() throws Exception {
        testPatch(
                "ns_array",
                new NamespaceInfo[] {data.namespaceA, data.namespaceB, data.namespaceC});
    }

    @Test
    void workspace() throws Exception {
        testPatch("workspace", data.workspaceA);
    }

    @Test
    void namespaceInfo() throws Exception {
        testPatch("namespace", data.namespaceA);
    }

    @Test
    void dataStoreInfo() throws Exception {
        testPatch("dataStore", data.dataStoreA);
    }

    @Test
    void coverageStoreInfo() throws Exception {
        testPatch("coverageStore", data.coverageStoreA);
    }

    @Test
    void wmsStoreInfo() throws Exception {
        testPatch("wms", data.wmsStoreA);
    }

    @Test
    void wmtsStoreInfo() throws Exception {
        testPatch("wmts", data.wmtsStoreA);
    }

    @Test
    void featureTypeInfo() throws Exception {
        testPatch("ft", data.featureTypeA);
    }

    @Test
    void coverageInfo() throws Exception {
        testPatch("coverage", data.coverageA);
    }

    @Test
    void wmsLayerInfo() throws Exception {
        testPatch("wmsl", data.wmsLayerA);
    }

    @Test
    void wmtsLayerInfo() throws Exception {
        testPatch("wmtsl", data.wmtsLayerA);
    }

    @Test
    void layerInfo() throws Exception {
        testPatch("layer", data.layerFeatureTypeA);
    }

    @Test
    void layerGroupInfo() throws Exception {
        testPatch("layer", data.layerGroup1);
    }

    @Test
    void layerInfo_references() throws Exception {
        LayerInfo layer = data.layerFeatureTypeA;
        StyleInfo s1 = data.faker().styleInfo("s1");
        StyleInfo s2 = data.faker().styleInfo("s2");
        catalog.add(s1);
        catalog.add(s2);

        layer.setDefaultStyle(data.style2);
        layer.getStyles().add(s1);
        layer.getStyles().add(s2);
        catalog.save(layer);

        final Patch patch =
                new Patch()
                        .with("styles", layer.getStyles())
                        .with("defaultStyle", layer.getDefaultStyle());

        final Patch resolved = testPatch(patch);
        Set<StyleInfo> styles = resolved.get("styles").orElseThrow().value();
        StyleInfo defaultStyle = resolved.get("defaultStyle").orElseThrow().value();
        assertModificationProxy(defaultStyle);
        assertCatalogSet(defaultStyle);
        styles.forEach(this::assertModificationProxy);
        styles.forEach(this::assertCatalogSet);

        final Patch unresolved = roundtrip(patch);
        styles = unresolved.get("styles").orElseThrow().value();
        defaultStyle = unresolved.get("defaultStyle").orElseThrow().value();
        assertResolvingProxy(defaultStyle);
        styles.forEach(this::assertResolvingProxy);
    }

    @Test
    void layerInfo_value_object_properties() throws Exception {
        LayerInfo layer = data.layerFeatureTypeA;

        layer.setLegend(data.faker().legendInfo());
        layer.setAttribution(data.faker().attributionInfo());

        layer.getAuthorityURLs().add(data.faker().authorityURLInfo());
        layer.getAuthorityURLs().add(data.faker().authorityURLInfo());

        layer.getIdentifiers().add(data.faker().layerIdentifierInfo());
        layer.getIdentifiers().add(data.faker().layerIdentifierInfo());

        Patch patch =
                new Patch()
                        .with("legend", layer.getLegend())
                        .with("attribution", layer.getAttribution())
                        .with("authorityURLs", layer.getAuthorityURLs())
                        .with("identifiers", layer.getIdentifiers());
        final Patch roundtripped = roundtrip(patch);

        LegendInfo legend = roundtripped.get("legend").orElseThrow().value();
        assertValueObject(legend, LegendInfo.class);
    }

    @Test
    void styleInfo() throws Exception {
        testPatch("style", data.style1);
    }

    @Test
    void geoserverInfo() throws Exception {
        testPatch("global", data.global);
    }

    @Test
    void loggingInfo() throws Exception {
        testPatch("logging", data.logging);
    }

    @Test
    void settingsInfo() throws Exception {
        testPatch("settings", data.faker().settingsInfo(null));
    }

    @Test
    void settingsInfo_workspace() throws Exception {
        Patch resolved = testPatch("settings", data.faker().settingsInfo(data.workspaceA));
        SettingsInfo setting = resolved.get("settings").orElseThrow().value();
        assertModificationProxy(setting.getWorkspace());
    }

    @Test
    void wmsInfo() throws Exception {
        testPatch("wmsService", data.wmsService);
    }

    @Test
    void wcsInfo() throws Exception {
        // WCSInfoImpl.equals is broken, we're still checking it's sent as a reference
        testPatchNoEquals(patch("wcsService", data.wcsService));
    }

    @Test
    void wfsInfo() throws Exception {
        testPatch("wfsService", data.wfsService);
    }

    @Test
    void wpsInfo() throws Exception {
        Patch patch = patch("wpsService", data.wpsService);
        // WPSInfoImpl.equals is broken, we're still checking it's sent as a reference
        testPatchNoEquals(patch);
    }

    @Test
    void attributionInfo() throws Exception {
        testPatch("attribution", data.faker().attributionInfo());
    }

    @Test
    void contactInfo() throws Exception {
        testPatch("contact", data.faker().contactInfo());
    }

    @Test
    void keywordInfo() throws Exception {
        CatalogFaker faker = data.faker();
        testPatch("kw", faker.keywordInfo());
    }

    @Test
    void keywordInfo_list() throws Exception {
        CatalogFaker faker = data.faker();
        testPatch("keywords", Arrays.asList(faker.keywordInfo(), null, faker.keywordInfo()));
    }

    @Test
    void name() throws Exception {
        org.geotools.api.feature.type.Name name = new NameImpl("localname");
        testPatch("name", name);
    }

    @Test
    void name_with_ns() throws Exception {
        org.geotools.api.feature.type.Name name = new NameImpl("http://name.space", "localname");
        testPatch("name", name);
    }

    @Test
    void version() throws Exception {
        testPatch("version", new org.geotools.util.Version("1.0.1"));
    }

    @Test
    void version_list() throws Exception {
        testPatch(
                "version",
                List.of(
                        new org.geotools.util.Version("1.0.1"),
                        new org.geotools.util.Version("1.0.2")));
    }

    @Test
    void class_property() throws Exception {
        testPatch("binding", org.geotools.util.Version.class);
    }

    @Test
    void metadataLinkInfo() throws Exception {
        testPatch("metadataLink", data.faker().metadataLink());
    }

    @Test
    void coordinateReferenceSystem() throws Exception {
        final boolean longitudeFirst = true;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", longitudeFirst);
        Patch resolved = testPatchNoEquals(patch("crs", crs));
        assertEquals(List.of("crs"), resolved.getPropertyNames());
        CoordinateReferenceSystem roundtripped =
                resolved.getValue("crs").map(CoordinateReferenceSystem.class::cast).orElseThrow();
        assertTrue(CRS.equalsIgnoreMetadata(crs, roundtripped));
    }

    @Test
    void referencedEnvelope() throws Exception {
        final boolean longitudeFirst = true;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", longitudeFirst);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 1000, -1, -1000, crs);
        testPatch("bounds", env);
    }

    @Test
    void numberRange() throws Exception {
        testPatch("range", NumberRange.create(-1, 1));
        // testPatch("range", NumberRange.create(-1.1f, 1.1f));
        // testPatch("range", NumberRange.create((short) 10, (short) 15));
        testPatch("range", NumberRange.create(Double.MIN_VALUE, 1.01));
    }

    @Test
    void measure() throws Exception {
        testPatch("meters", new Measure(1000, SI.METRE));
        testPatch("radians", new Measure(.75, SI.RADIAN_PER_SECOND));
    }

    @Test
    void gridGeometry() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope env = new ReferencedEnvelope(-180, 180, -90, 90, crs);
        GridEnvelope range = new GeneralGridEnvelope(new int[] {0, 0}, new int[] {1024, 768});
        GridGeometry2D gridGeometry = new GridGeometry2D(range, env);
        testPatch("gridGeometry", gridGeometry);
    }

    @Test
    void modificationProxy_workspace() throws Exception {
        testPatch("workspace", forceProxy(data.workspaceA, WorkspaceInfo.class));
    }

    @Test
    void modificationProxy_namespace() throws Exception {
        testPatch("namespace", forceProxy(data.namespaceA, NamespaceInfo.class));
    }

    @Test
    void modificationProxy_dataStore() throws Exception {
        testPatch("dataStore", forceProxy(data.dataStoreA, DataStoreInfo.class));
    }

    @Test
    void modificationProxy_coverageStore() throws Exception {
        testPatch("coverageStore", forceProxy(data.coverageStoreA, CoverageStoreInfo.class));
    }

    @Test
    void modificationProxy_layer() throws Exception {
        testPatch("layer", forceProxy(data.layerFeatureTypeA, LayerInfo.class));
    }

    @Test
    void modificationProxy_style() throws Exception {
        testPatch("style", forceProxy(data.style1, StyleInfo.class));
    }

    @Test
    void modificationProxy_geoserverInfo() throws Exception {
        testPatch("global", forceProxy(data.global, GeoServerInfo.class));
    }

    @Test
    void modificationProxy_wmsInfo() throws Exception {
        testPatch("wmsService", forceProxy(data.wmsService, WMSInfo.class));
    }

    @Test
    void modificationProxy_settingsInfo() throws Exception {
        testPatch(
                "settings",
                forceProxy(data.faker().settingsInfo(data.workspaceA), SettingsInfo.class));
    }

    /** Though ContactInfo is a value object, webui will save a ModificationProxy */
    @Test
    void modificationProxy_settingsInfo_with_proxy_contact_info() throws Exception {
        WorkspaceInfo workspaceProxy = forceProxy(data.workspaceA, WorkspaceInfo.class);
        SettingsInfo settings = data.faker().settingsInfo(workspaceProxy);
        ContactInfo contactInfo = data.faker().contactInfo();
        ContactInfo contactProxy = forceProxy(contactInfo, ContactInfo.class);
        SettingsInfo settingsProxy = forceProxy(settings, SettingsInfo.class);
        settingsProxy.setContact(contactProxy);

        final Patch sent = patch("settings", settingsProxy);
        final SettingsInfo received = testPatchNoEquals(sent).get("settings").orElseThrow().value();
        assertModificationProxy(workspaceProxy, received.getWorkspace());
        ContactInfo contact = received.getContact();
        assertNotAProxy(contact);
        assertEquals(contactInfo, contact);
    }

    /** Though ContactInfo is a value object, webui will save a ModificationProxy */
    @Test
    void modificationProxy_contactInfo() throws Exception {
        ContactInfo contactInfo = data.faker().contactInfo();
        Patch received = testPatch("contact", forceProxy(contactInfo, ContactInfo.class));
        assertValueObject(received.get("contact").orElseThrow().getValue(), ContactInfo.class);
    }

    @Test
    void testPatchWithListProperty() throws Exception {
        testPatch("nullvalue", newArrayList(null, null));
        testPatch("int", List.of(Integer.MAX_VALUE, Integer.MIN_VALUE));
        testPatch("long", List.of(Long.MAX_VALUE, Long.MIN_VALUE));
        testPatch("date", List.of(new Date(10_000_000), new Date(11_000_000)));
        testPatch("string", List.of("string1", "string2"));
        // some CatalogInfo properties, , shall be converted to references
        testPatch("workspaces", List.of(data.workspaceA, data.workspaceB));
        testPatch("namespaces", List.of(data.namespaceA, data.namespaceB));
        testPatch("stores", List.of(data.dataStoreA, data.coverageStoreA));
        testPatch("layers", List.of(data.layerFeatureTypeA));
        testPatch("styles", List.of(data.style1, data.style2));
        testPatch(
                "attribution",
                List.of(data.faker().attributionInfo(), data.faker().attributionInfo()));
        testPatch("contact", List.of(data.faker().contactInfo(), data.faker().contactInfo()));

        testPatch("serviceInfos", List.of(data.wmsService));
        // REVISIT: WFSInfoImpl.equals is broken
        // testPatch("serviceInfos", List.of(data.wmsService, data.wfsService));
        // REVISIT: WCSInfoImpl.equals is broken
        // testPatch("serviceInfos", List.of(data.wcsService));
    }

    @Test
    void attributeTypeInfo_list() throws Exception {
        FeatureTypeInfo ft = data.featureTypeA;
        List<AttributeTypeInfo> attributes = createTestAttributes(ft);

        Patch roundTrippedAndResolved = testPatch("attributes", attributes);
        List<AttributeTypeInfo> rtripAtts =
                roundTrippedAndResolved.get("attributes").orElseThrow().value();

        for (AttributeTypeInfo att : rtripAtts) {
            assertModificationProxy(ft, att.getFeatureType());
        }
    }

    @Test
    void testPatchWithSetProperty() throws Exception {
        WorkspaceInfo wsa = catalog.getWorkspace(data.workspaceA.getId());
        WorkspaceInfo wsb = catalog.getWorkspace(data.workspaceB.getId());
        Set<WorkspaceInfo> workspaces = Set.of(wsa, wsb);

        Set<NamespaceInfo> namespaces = Set.of(data.namespaceA, data.namespaceB);
        Set<StoreInfo> stores = Set.of(data.dataStoreA, data.coverageStoreA);
        Set<LayerInfo> layers = Set.of(data.layerFeatureTypeA);
        Set<StyleInfo> styles = Set.of(data.style1, data.style2);

        WMSInfo s1 = geoserver.getService(data.wmsService.getId(), WMSInfo.class);
        WFSInfo s2 = geoserver.getService(data.wfsService.getId(), WFSInfo.class);
        Set<ServiceInfo> services = Set.of(s1, s2);

        Set<AttributionInfo> attributionInfos =
                Set.of(data.faker().attributionInfo(), data.faker().attributionInfo());
        Set<ContactInfo> contactInfos =
                Set.of(data.faker().contactInfo(), data.faker().contactInfo());

        testPatch("workspaces", workspaces);
        testPatch("namespaces", namespaces);
        testPatch("stores", stores);
        testPatch("layers", layers);
        testPatch("styles", styles);
        testPatch("attribution", attributionInfos);
        testPatch("contact", contactInfos);

        testPatch("serviceInfos", Set.of(data.wmsService));
        testPatch("serviceInfos", services);
    }

    @Test
    void simpleInternationalString() throws Exception {
        InternationalString simpleI18n = new SimpleInternationalString("simpleI18n");
        Patch patch = new Patch();
        patch.add("simpleI18n", simpleI18n);

        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();
        String encoded = writer.writeValueAsString(patch);
        print("encoded: {}", encoded);
        Patch decoded = objectMapper.readValue(encoded, Patch.class);
        Patch expected = new Patch();
        expected.add("simpleI18n", new GrowableInternationalString(simpleI18n.toString()));
        assertEquals(expected, decoded);
    }

    @Test
    void authorityURLInfo() throws Exception {
        testPatch("authorityURL", data.faker().authorityURLInfo());
    }

    @Test
    void coverageAccessInfo() throws Exception {
        CoverageAccessInfo coverageInfo = new CoverageAccessInfoImpl();
        coverageInfo.setCorePoolSize(10);
        coverageInfo.setQueueType(QueueType.UNBOUNDED);

        testPatch("coverageInfo", coverageInfo);
    }

    @Test
    void jaiInfo() throws Exception {
        JAIInfo jaiInfo = data.faker().jaiInfo();
        testPatch("jaiInfo", jaiInfo);
    }

    @Test
    void wmsInterpolation() throws Exception {
        WMSInterpolation v = WMSInterpolation.Bicubic;
        testPatch("defaultWmsInterpolation", v);
    }

    @Test
    void publishedType() throws Exception {
        PublishedType v = PublishedType.REMOTE;
        testPatch("publishedType", v);
    }

    @Test
    void growableInternationalString() throws Exception {
        GrowableInternationalString growableI18n = new GrowableInternationalString("default lang");
        growableI18n.add(Locale.forLanguageTag("es-AR"), "en argentino");
        growableI18n.add(Locale.forLanguageTag("es"), "en español");
        testPatch("growableI18n", growableI18n);
    }

    @Test
    void coverageDimensionInfo() throws Exception {
        testPatch("coverageDimensionInfo", data.faker().coverageDimensionInfo());
    }

    @Test
    void dimensionInfo() throws Exception {
        // equals is broken
        DimensionInfo expected = data.faker().dimensionInfo();
        Patch patch = testPatchNoEquals(patch("dimensionInfo", expected));
        DimensionInfo actual =
                patch.getValue("dimensionInfo").map(DimensionInfo.class::cast).orElseThrow();

        assertEquals(expected.getAcceptableInterval(), actual.getAcceptableInterval());
        assertEquals(expected.getAttribute(), actual.getAttribute());
        assertEquals(
                expected.getDefaultValue().getReferenceValue(),
                actual.getDefaultValue().getReferenceValue());
        assertEquals(
                expected.getDefaultValue().getStrategyType(),
                actual.getDefaultValue().getStrategyType());
        assertEquals(expected.isEnabled(), actual.isEnabled());
        assertEquals(expected.isNearestMatchEnabled(), actual.isNearestMatchEnabled());
        assertEquals(expected.isRawNearestMatchEnabled(), actual.isRawNearestMatchEnabled());
        assertEquals(expected.getResolution(), actual.getResolution());
        assertEquals(expected.getUnits(), actual.getUnits());
        assertEquals(expected.getUnitSymbol(), actual.getUnitSymbol());
        assertEquals(expected.getPresentation(), actual.getPresentation());
    }

    @Test
    void dataLinkInfo() throws Exception {
        testPatch("dl", data.faker().dataLinkInfo());
    }

    @Test
    void layerIdentifierInfo() throws Exception {
        testPatch("layerIdentifier", data.faker().layerIdentifierInfo());
    }

    @Test
    void metadataMap() throws Exception {
        MetadataMap md = data.faker().metadataMap("k1", "v1", "k2", 2);
        Patch patch = testPatch("metadata", md);
        assertThat(patch.get("metadata").orElseThrow().getValue()).isInstanceOf(HashMap.class);
        CoverageStoreInfo s = new CoverageStoreInfoImpl(null);
        patch.applyTo(s);
        assertEquals(md, s.getMetadata());
    }

    @Test
    void metadataMapWithCogSettings() throws Exception {
        CogSettings cog = new CogSettings();
        cog.setRangeReaderSettings(RangeReaderType.Azure);
        cog.setUseCachingStream(true);

        CogSettingsStore cogs = new CogSettingsStore(cog);
        cogs.setUsername(null);
        cogs.setPassword(null);

        metadataMapWithCogSettings(cog, cogs);

        cogs.setUsername("user");
        cogs.setUsername("pwd");
        metadataMapWithCogSettings(cog, cogs);
    }

    protected void metadataMapWithCogSettings(CogSettings cog, CogSettingsStore cogs)
            throws JsonProcessingException {

        MetadataMap mdm = new MetadataMap(Map.of("cogSettings", cog, "cogSettingsStore", cogs));

        Patch patch = testPatchNoEquals(patch("metadata", mdm));
        assertThat(patch.get("metadata").orElseThrow().getValue()).isInstanceOf(HashMap.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> decoded =
                (Map<String, Object>) patch.get("metadata").orElseThrow().getValue();

        assertThat(decoded.get("cogSettings"), CoreMatchers.instanceOf(CogSettings.class));
        assertThat(
                decoded.get("cogSettingsStore"), CoreMatchers.instanceOf(CogSettingsStore.class));
        var c = (CogSettings) decoded.get("cogSettings");
        var s = (CogSettingsStore) decoded.get("cogSettingsStore");
        assertNotNull(c);
        assertNotNull(s);

        assertEquals(cog.getRangeReaderSettings(), c.getRangeReaderSettings());
        assertEquals(cog.isUseCachingStream(), c.isUseCachingStream());

        assertEquals(cogs.getRangeReaderSettings(), s.getRangeReaderSettings());
        assertEquals(cogs.isUseCachingStream(), s.isUseCachingStream());
        assertEquals(cogs.getUsername(), s.getUsername());
        assertEquals(cogs.getPassword(), s.getPassword());
    }

    private Patch testPatch(String name, Object value) throws Exception {
        Patch patch = patch(name, value);
        return testPatch(patch);
    }

    private Patch testPatch(Patch patch) throws JsonProcessingException, JsonMappingException {
        Patch resolved = testPatchNoEquals(patch);

        assertEquals(patch, resolved);
        return resolved;
    }

    private Patch testPatchNoEquals(Patch patch)
            throws JsonProcessingException, JsonMappingException {

        Patch decoded = roundtrip(patch);
        Patch resolved = resolve(decoded);
        print("resolved: {}", resolved);

        Object patchValue = patch.getPatches().get(0).getValue();
        boolean encodeByReference = ProxyUtils.encodeByReference(patchValue);
        if (encodeByReference) {
            Info decodedValue = resolved.getPatches().get(0).value();
            Info decodedUnwrapped = assertModificationProxy(decodedValue);
            Info orig = ModificationProxy.unwrap(patch.getPatches().get(0).value());
            assertThat(decodedUnwrapped).isSameAs(orig);
        }

        return resolved;
    }

    private Patch patch(String name, Object value) {
        Patch patch = new Patch();
        patch.add(name, value);
        return patch;
    }

    protected Patch resolve(Patch decoded) {
        Patch resolved = proxyResolver.resolve(decoded);
        return resolved;
    }

    private Patch roundtrip(Patch patch) throws JsonProcessingException, JsonMappingException {
        Object patchValue = patch.getPatches().get(0).getValue();
        boolean encodeByReference = ProxyUtils.encodeByReference(patchValue);

        String encoded = asJson(patch);
        print("encoded: {}", encoded);

        Patch decoded = objectMapper.readValue(encoded, Patch.class);
        print("decoded: {}", asJson(decoded));

        Object roundtrippedValue = decoded.getPatches().get(0).getValue();
        if (encodeByReference) {
            String typeName = typeName(roundtrippedValue);
            Class<? extends Info> type = ProxyUtils.referenceTypeOf(patchValue).orElseThrow();
            Supplier<String> desc =
                    () -> {
                        return String.format(
                                "Patch value of type %s shall be encoded as reference, got value %s",
                                type.getCanonicalName(), typeName);
                    };
            assertThat(typeName).as(desc).isEqualTo("ResolvingProxy");
        } else {
            assertNotAProxy(roundtrippedValue);
        }

        return decoded;
    }

    protected String asJson(Patch patch) throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();
        String encoded = writer.writeValueAsString(patch);
        return encoded;
    }

    private <T extends Info> T forceProxy(T info, Class<T> iface) {
        info = ModificationProxy.unwrap(info);
        return (T) ModificationProxy.create(info, iface);
    }

    protected String typeName(Object mp) {
        if (mp == null) return null;
        if (mp instanceof Info info) {
            if (ProxyUtils.isResolvingProxy(info)) return "ResolvingProxy";
            if (ProxyUtils.isModificationProxy(info)) return "ModificationProxy";
        }
        return mp.getClass().getCanonicalName();
    }

    private void assertCatalogSet(Info value) {
        Info real = ModificationProxy.unwrap(value);
        Catalog assignedCatalog = (Catalog) OwsUtils.get(real, "catalog");
        assertSame(this.catalog, assignedCatalog);
    }

    protected void assertNotAProxy(Object value) {
        if (value instanceof Info info) {
            assertThat(ProxyUtils.isResolvingProxy(info))
                    .as(
                            () ->
                                    String.format(
                                            "%s should not be a ResolvingProxy",
                                            info.getId(), typeName(info)))
                    .isFalse();
            assertThat(ProxyUtils.isModificationProxy(info))
                    .as(
                            () ->
                                    String.format(
                                            "%s should not be a ModificationProxy",
                                            info.getId(), typeName(info)))
                    .isFalse();
        }
    }

    protected <I extends Info> I assertModificationProxy(I info) {
        assertThat(ProxyUtils.isModificationProxy(info))
                .as(
                        () ->
                                String.format(
                                        "%s should be a ModificationProxy, got %s",
                                        info.getId(), typeName(info)))
                .isTrue();

        I real = ModificationProxy.unwrap(info);
        assertNotNull(real);
        return real;
    }

    protected <I extends Info> I assertModificationProxy(I expected, I actual) {
        I actualUnwrapped = assertModificationProxy(actual);
        I expectedUnwrapped = ModificationProxy.unwrap(expected);
        assertSame(expectedUnwrapped, actualUnwrapped);
        return actualUnwrapped;
    }

    protected void assertResolvingProxy(Info info) {
        assertThat(ProxyUtils.isResolvingProxy(info))
                .as(
                        () ->
                                String.format(
                                        "%s should be a ResolvingProxy, got %s",
                                        info.getId(), typeName(info)))
                .isTrue();
    }

    private void assertValueObject(Object valueObject, Class<?> valueType) {
        if (valueObject instanceof Info info) {
            Supplier<String> msg =
                    () ->
                            String.format(
                                    "expected pure value object of type %s, got %s",
                                    valueType.getCanonicalName(), typeName(valueObject));
            assertThat(ProxyUtils.isResolvingProxy(info)).as(msg).isFalse();
            assertThat(ProxyUtils.isModificationProxy(info)).as(msg).isFalse();
        }
        assertThat(valueObject).isInstanceOf(valueType);
    }
}
