/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.gwc.wmts.WMTSInfoImpl;
import org.geoserver.jackson.databind.catalog.ProxyUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that all GeoServer config ({@link GeoServerInfo}, etc) object types can be sent over the
 * wire and parsed back using jackson, thanks to {@link GeoServerConfigModule} jackcon-databind
 * module
 */
public class GeoServerConfigModuleTest {

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
        catalog = new CatalogPlugin();
        geoserver = new GeoServerImpl();
        testData =
                CatalogTestData.initialized(() -> catalog, () -> geoserver)
                        .initConfig(false)
                        .initialize();
        proxyResolver = new ProxyUtils(catalog, geoserver);

        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.findAndRegisterModules();
    }

    private <T extends Info> void roundtripTest(@NonNull T orig) throws JsonProcessingException {
        ObjectWriter writer = objectMapper.writer();
        writer = writer.withDefaultPrettyPrinter();
        String encoded = writer.writeValueAsString(orig);

        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) ClassMappings.fromImpl(orig.getClass()).getInterface();

        T decoded = objectMapper.readValue(encoded, type);
        // This is the client code's responsibility, the Deserializer returns "resolving proxy"
        // proxies for Info references
        decoded = proxyResolver.resolve(decoded);

        OwsUtils.resolveCollections(orig);
        OwsUtils.resolveCollections(decoded);
        assertEquals(orig, decoded);
        testData.assertInternationalStringPropertiesEqual(orig, decoded);
        if (orig instanceof SettingsInfo) {
            // SettingsInfoImpl's equals() doesn't check workspace
            assertEquals(
                    ((SettingsInfo) orig).getWorkspace(), ((SettingsInfo) decoded).getWorkspace());
        }
    }

    public @Test void geoServerInfo() throws Exception {
        roundtripTest(testData.global);
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
        roundtripTest(testData.wcsService);
    }

    public @Test void wfsServiceInfo() throws Exception {
        roundtripTest(testData.wfsService);
    }

    public @Test void wpsServiceInfo() throws Exception {
        roundtripTest(testData.wpsService);
    }

    public @Test void wmtsServiceInfo() throws Exception {
        WMTSInfo wmtsService = testData.createService("wmts", WMTSInfoImpl::new);
        roundtripTest(wmtsService);
    }
}
