/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.gwc.wmts.WMTSInfoImpl;
import org.geoserver.jackson.databind.catalog.ProxyUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.WFSInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that all GeoServer config ({@link GeoServerInfo}, etc) object types can be sent over the
 * wire and parsed back using jackson, thanks to {@link GeoServerConfigModule} jackcon-databind
 * module
 */
@Slf4j
public abstract class GeoServerConfigModuleTest {

    private boolean debug = Boolean.valueOf(System.getProperty("debug", "false"));

    protected void print(String logmsg, Object... args) {
        if (debug) log.debug(logmsg, args);
    }

    private ObjectMapper objectMapper;

    private Catalog catalog;
    private CatalogTestData testData;
    private GeoServer geoserver;
    private ProxyUtils proxyResolver;

    public static @BeforeAll void oneTimeSetup() {
        // avoid the chatty warning logs due to catalog looking up a bean of type
        // GeoServerConfigurationLock
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    protected abstract ObjectMapper newObjectMapper();

    public @BeforeEach void before() {
        objectMapper = newObjectMapper();
        catalog = new CatalogPlugin();
        geoserver = new GeoServerImpl();
        testData =
                CatalogTestData.initialized(() -> catalog, () -> geoserver)
                        .initConfig(false)
                        .initialize();
        proxyResolver = new ProxyUtils(catalog, geoserver);
    }

    private <T extends Info> void roundtripTest(@NonNull final T orig)
            throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();
        String encoded = writer.writeValueAsString(orig);
        print("encoded: {}", encoded);

        ClassMappings cm = ClassMappings.fromImpl(orig.getClass());
        @SuppressWarnings("unchecked")
        Class<T> type = cm == null ? (Class<T>) orig.getClass() : (Class<T>) cm.getInterface();

        T decoded = objectMapper.readValue(encoded, type);
        print("decoded: {}", decoded);
        // This is the client code's responsibility, the Deserializer returns "resolving proxy"
        // proxies for Info references
        T resolved = proxyResolver.resolve(decoded);
        print("resolved: {}", resolved);

        OwsUtils.resolveCollections(orig);
        OwsUtils.resolveCollections(resolved);
        assertEquals(orig, resolved);
        testData.assertInternationalStringPropertiesEqual(orig, resolved);
        if (orig instanceof SettingsInfo) {
            // SettingsInfoImpl's equals() doesn't check workspace
            assertEquals(
                    ((SettingsInfo) orig).getWorkspace(), ((SettingsInfo) resolved).getWorkspace());
        }
    }

    public @Test void geoServerInfo() throws Exception {
        GeoServerInfo global = testData.global;
        global.setJAI(null);
        roundtripTest(global);
    }

    public @Test void settingsInfo() throws Exception {
        roundtripTest(testData.workspaceASettings);
    }

    public @Test void loggingInfo() throws Exception {
        roundtripTest(testData.logging);
    }

    public @Test void wmsServiceInfo() throws Exception {
        roundtripTest(testData.wmsService);
    }

    public @Test void wcsServiceInfo() throws Exception {
        WCSInfo wcsService = testData.wcsService;
        // WCSInfoImpl.equals() is broken, set a value for this property to workaround it
        wcsService.setMaxRequestedDimensionValues(10);
        roundtripTest(wcsService);
    }

    public @Test void wfsServiceInfo() throws Exception {
        WFSInfo wfs = testData.wfsService;
        wfs.getGML().put(WFSInfo.Version.V_10, testData.faker().gmlInfo());
        wfs.getGML().put(WFSInfo.Version.V_20, testData.faker().gmlInfo());
        roundtripTest(wfs);
    }

    public @Test void wpsServiceInfo() throws Exception {
        roundtripTest(testData.wpsService);
    }

    public @Test void wmtsServiceInfo() throws Exception {
        WMTSInfo wmtsService = testData.faker().serviceInfo("wmts", WMTSInfoImpl::new);
        roundtripTest(wmtsService);
    }

    public @Test void contactInfo() throws Exception {
        ContactInfo contact = testData.faker().contactInfo();
        roundtripTest(contact);
    }

    public @Test void setingsInfo() throws Exception {
        roundtripTest(testData.faker().settingsInfo(null));
    }

    public @Test void setingsInfoWithWorkspace() throws Exception {
        SettingsInfo settings = testData.faker().settingsInfo(testData.workspaceA);
        roundtripTest(settings);
    }
}
