/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import static com.google.common.collect.Lists.newArrayList;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.geoserver.jackson.databind.catalog.mapper.ValueMappers;
import org.geoserver.jackson.databind.config.dto.mapper.GeoServerConfigMapper;
import org.geoserver.jackson.databind.mapper.SharedMappers;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import si.uom.SI;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Verifies that {@link Patch patches} can be JSON round-tripped. As a reference, it should cover as
 * much of {@link SharedMappers}, {@link ValueMappers}, {@link GeoServerConfigMapper}, and {@link
 * CatalogInfoMapper} as possible.
 */
@Slf4j
public class PatchSerializationTest {

    private boolean debug = Boolean.valueOf(System.getProperty("debug", "false"));

    protected void print(String logmsg, Object... args) {
        if (debug) log.debug(logmsg, args);
    }

    public static ObjectMapper objectMapper;

    private Catalog catalog;
    private CatalogTestData data;
    private GeoServer geoserver;
    private ProxyUtils proxyResolver;

    public static @BeforeAll void oneTimeSetup() {
        // avoid the chatty warning logs due to catalog looking up a bean of type
        // GeoServerConfigurationLock
        GeoServerExtensionsHelper.setIsSpringContext(false);
        objectMapper = ObjectMapperUtil.newObjectMapper();
    }

    public @BeforeEach void before() {
        catalog = new CatalogPlugin();
        geoserver = new GeoServerImpl();
        data = CatalogTestData.initialized(() -> catalog, () -> geoserver).initialize();
        proxyResolver = new ProxyUtils(catalog, geoserver);
    }

    private List<AttributeTypeInfo> createTestAttributes(FeatureTypeInfo info)
            throws SchemaException {
        String typeSpec =
                "name:string,id:String,polygonProperty:Polygon:srid=32615,centroid:Point,url:java.net.URL,uuid:UUID";
        SimpleFeatureType ft = DataUtilities.createType("TestType", typeSpec);
        return new CatalogBuilder(new CatalogPlugin()).getAttributes(ft, info);
    }

    public @Test void simpleTypes() throws Exception {
        testPatch("nullvalue", null);
        testPatch("int", Integer.MAX_VALUE);
        testPatch("long", Long.MAX_VALUE);
        testPatch("date", new Date(10_000_000));
        testPatch("string", "string value");
    }

    public @Test void workspace() throws Exception {
        testPatch("workspace", data.workspaceA);
    }

    public @Test void namespaceInfo() throws Exception {
        testPatch("namespace", data.namespaceA);
    }

    public @Test void dataStoreInfo() throws Exception {
        testPatch("dataStore", data.dataStoreA);
    }

    public @Test void coverageStoreInfo() throws Exception {
        testPatch("coverageStore", data.coverageStoreA);
    }

    public @Test void wmsStoreInfo() throws Exception {
        testPatch("wms", data.wmsStoreA);
    }

    public @Test void wmtsStoreInfo() throws Exception {
        testPatch("wmts", data.wmtsStoreA);
    }

    public @Test void featureTypeInfo() throws Exception {
        testPatch("ft", data.featureTypeA);
    }

    public @Test void coverageInfo() throws Exception {
        testPatch("coverage", data.coverageA);
    }

    public @Test void wmsLayerInfo() throws Exception {
        testPatch("wmsl", data.wmsLayerA);
    }

    public @Test void wmtsLayerInfo() throws Exception {
        testPatch("wmtsl", data.wmtsLayerA);
    }

    public @Test void layerInfo() throws Exception {
        testPatch("layer", data.layerFeatureTypeA);
    }

    public @Test void layerGroupInfo() throws Exception {
        testPatch("layer", data.layerGroup1);
    }

    public @Test void layerInfo_references() throws Exception {
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

    public @Test void layerInfo_value_object_properties() throws Exception {
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

    public @Test void styleInfo() throws Exception {
        testPatch("style", data.style1);
    }

    public @Test void geoserverInfo() throws Exception {
        testPatch("global", data.global);
    }

    public @Test void loggingInfo() throws Exception {
        testPatch("logging", data.logging);
    }

    public @Test void settingsInfo() throws Exception {
        testPatch("settings", data.faker().settingsInfo(null));
    }

    public @Test void settingsInfo_workspace() throws Exception {
        Patch resolved = testPatch("settings", data.faker().settingsInfo(data.workspaceA));
        SettingsInfo setting = resolved.get("settings").orElseThrow().value();
        assertModificationProxy(setting.getWorkspace());
    }

    public @Test void wmsInfo() throws Exception {
        testPatch("wmsService", data.wmsService);
    }

    public @Test void wcsInfo() throws Exception {
        // WCSInfoImpl.equals is broken, we're still checking it's sent as a reference
        testPatchNoEquals(patch("wcsService", data.wcsService));
    }

    public @Test void wfsInfo() throws Exception {
        testPatch("wfsService", data.wfsService);
    }

    public @Test void wpsInfo() throws Exception {
        Patch patch = patch("wpsService", data.wpsService);
        // WPSInfoImpl.equals is broken, we're still checking it's sent as a reference
        testPatchNoEquals(patch);
    }

    public @Test void attributionInfo() throws Exception {
        testPatch("attribution", data.faker().attributionInfo());
    }

    public @Test void contactInfo() throws Exception {
        testPatch("contact", data.faker().contactInfo());
    }

    public @Test void keywordInfo() throws Exception {
        CatalogFaker faker = data.faker();
        testPatch("kw", faker.keywordInfo());
    }

    public @Test void keywordInfo_list() throws Exception {
        CatalogFaker faker = data.faker();
        testPatch("keywords", Arrays.asList(faker.keywordInfo(), null, faker.keywordInfo()));
    }

    public @Test void name() throws Exception {
        org.opengis.feature.type.Name name = new NameImpl("localname");
        testPatch("name", name);
    }

    public @Test void name_with_ns() throws Exception {
        org.opengis.feature.type.Name name = new NameImpl("http://name.space", "localname");
        testPatch("name", name);
    }

    public @Test void version() throws Exception {
        testPatch("version", new org.geotools.util.Version("1.0.1"));
    }

    public @Test void version_list() throws Exception {
        testPatch(
                "version",
                List.of(
                        new org.geotools.util.Version("1.0.1"),
                        new org.geotools.util.Version("1.0.2")));
    }

    public @Test void class_property() throws Exception {
        testPatch("binding", org.geotools.util.Version.class);
    }

    public @Test void metadataLinkInfo() throws Exception {
        testPatch("metadataLink", data.faker().metadataLink());
    }

    public @Test void coordinateReferenceSystem() throws Exception {
        final boolean longitudeFirst = true;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", longitudeFirst);
        Patch resolved = testPatchNoEquals(patch("crs", crs));
        assertEquals(List.of("crs"), resolved.getPropertyNames());
        CoordinateReferenceSystem roundtripped =
                resolved.getValue("crs").map(CoordinateReferenceSystem.class::cast).orElseThrow();
        assertTrue(CRS.equalsIgnoreMetadata(crs, roundtripped));
    }

    public @Test void referencedEnvelope() throws Exception {
        final boolean longitudeFirst = true;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", longitudeFirst);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 1000, -1, -1000, crs);
        testPatch("bounds", env);
    }

    public @Test void numberRange() throws Exception {
        testPatch("range", NumberRange.create(-1, 1));
        // testPatch("range", NumberRange.create(-1.1f, 1.1f));
        // testPatch("range", NumberRange.create((short) 10, (short) 15));
        testPatch("range", NumberRange.create(Double.MIN_VALUE, 1.01));
    }

    public @Test void measure() throws Exception {
        testPatch("meters", new Measure(1000, SI.METRE));
        testPatch("radians", new Measure(.75, SI.RADIAN_PER_SECOND));
    }

    public @Test void gridGeometry() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope env = new ReferencedEnvelope(-180, 180, -90, 90, crs);
        GridEnvelope range = new GeneralGridEnvelope(new int[] {0, 0}, new int[] {1024, 768});
        GridGeometry2D gridGeometry = new GridGeometry2D(range, env);
        testPatch("gridGeometry", gridGeometry);
    }

    public @Test void testPatchWithModificationProxy() throws Exception {
        testPatch("workspace", forceProxy(data.workspaceA));
        testPatch("namespace", forceProxy(data.namespaceA));
        testPatch("dataStore", forceProxy(data.dataStoreA));
        testPatch("coverageStore", forceProxy(data.coverageStoreA));
        testPatch("layer", forceProxy(data.layerFeatureTypeA));
        testPatch("style", forceProxy(data.style1));
        testPatch("global", forceProxy(data.global));
        testPatch("logging", forceProxy(data.logging));
        testPatch("settings", forceProxy(data.workspaceASettings));
        testPatch("wmsService", forceProxy(data.wmsService));
    }

    public @Test void testPatchWithListProperty() throws Exception {
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

    public @Test void attributeTypeInfo_list() throws Exception {
        FeatureTypeInfo ft = data.featureTypeA;
        List<AttributeTypeInfo> attributes = createTestAttributes(ft);

        Patch roundTrippedAndResolved = testPatch("attributes", attributes);
        List<AttributeTypeInfo> rtripAtts =
                roundTrippedAndResolved.get("attributes").orElseThrow().value();

        for (AttributeTypeInfo att : rtripAtts) {
            assertModificationProxy(ft, att.getFeatureType());
        }
    }

    public @Test void testPatchWithSetProperty() throws Exception {
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

    public @Test void simpleInternationalString() throws Exception {
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

    public @Test void authorityURLInfo() throws Exception {
        testPatch("authorityURL", data.faker().authorityURLInfo());
    }

    public @Test void coverageAccessInfo() throws Exception {
        CoverageAccessInfo coverageInfo = new CoverageAccessInfoImpl();
        coverageInfo.setCorePoolSize(10);
        coverageInfo.setQueueType(QueueType.UNBOUNDED);

        testPatch("coverageInfo", coverageInfo);
    }

    public @Test void jaiInfo() throws Exception {
        JAIInfo jaiInfo = data.faker().jaiInfo();
        testPatch("jaiInfo", jaiInfo);
    }

    public @Test void growableInternationalString() throws Exception {
        GrowableInternationalString growableI18n = new GrowableInternationalString("default lang");
        growableI18n.add(Locale.forLanguageTag("es-AR"), "en argentino");
        growableI18n.add(Locale.forLanguageTag("es"), "en español");
        testPatch("growableI18n", growableI18n);
    }

    public @Test void coverageDimensionInfo() throws Exception {
        testPatch("coverageDimensionInfo", data.faker().coverageDimensionInfo());
    }

    public @Test void dimensionInfo() throws Exception {
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

    public @Test void dataLinkInfo() throws Exception {
        testPatch("dl", data.faker().dataLinkInfo());
    }

    public @Test void layerIdentifierInfo() throws Exception {
        testPatch("layerIdentifier", data.faker().layerIdentifierInfo());
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

    @SuppressWarnings("unchecked")
    private <T extends Info> T forceProxy(T info) {
        if (!Proxy.isProxyClass(info.getClass())) {
            Class<? extends Info> iface = ClassMappings.fromImpl(info.getClass()).getInterface();
            return (T) ModificationProxy.create(info, iface);
        }
        return info;
    }

    protected String typeName(Object mp) {
        if (mp == null) return null;
        if (mp instanceof Info) {
            Info info = (Info) mp;
            if (proxyResolver.isResolvingProxy(info)) return "ResolvingProxy";
            if (proxyResolver.isModificationProxy(info)) return "ModificationProxy";
        }
        return mp.getClass().getCanonicalName();
    }

    private void assertCatalogSet(Info value) {
        Info real = ModificationProxy.unwrap(value);
        Catalog assignedCatalog = (Catalog) OwsUtils.get(real, "catalog");
        assertSame(this.catalog, assignedCatalog);
    }

    protected void assertNotAProxy(Object value) {
        if (value instanceof Info) {
            Info info = (Info) value;
            assertThat(proxyResolver.isResolvingProxy(info))
                    .as(
                            () ->
                                    String.format(
                                            "%s should not be a ResolvingProxy",
                                            info.getId(), typeName(info)))
                    .isFalse();
            assertThat(proxyResolver.isModificationProxy(info))
                    .as(
                            () ->
                                    String.format(
                                            "%s should not be a ModificationProxy",
                                            info.getId(), typeName(info)))
                    .isFalse();
        }
    }

    protected <I extends Info> I assertModificationProxy(I info) {
        assertThat(proxyResolver.isModificationProxy(info))
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
        assertThat(proxyResolver.isResolvingProxy(info))
                .as(
                        () ->
                                String.format(
                                        "%s should be a ResolvingProxy, got %s",
                                        info.getId(), typeName(info)))
                .isTrue();
    }

    private void assertValueObject(Object valueObject, Class<?> valueType) {
        if (valueObject instanceof Info) {
            Supplier<String> msg =
                    () ->
                            String.format(
                                    "expected pure value object of type %s, got %s",
                                    valueType.getCanonicalName(), typeName(valueObject));
            assertThat(proxyResolver.isResolvingProxy((Info) valueObject)).as(msg).isFalse();
            assertThat(proxyResolver.isModificationProxy((Info) valueObject)).as(msg).isFalse();
        }
        assertThat(valueObject).isInstanceOf(valueType);
    }
}
