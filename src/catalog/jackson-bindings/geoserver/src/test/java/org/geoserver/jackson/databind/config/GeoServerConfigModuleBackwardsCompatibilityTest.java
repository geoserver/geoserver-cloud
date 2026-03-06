/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.jackson.databind.catalog.BackwardsCompatibilityTestSupport;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wps.WPSInfo;
import org.junit.jupiter.api.Test;

/**
 * Verifies that JSON produced by Jackson 2's property naming convention can be deserialized for GeoServer configuration
 * objects. Tests focus on fields where Jackson 2's {@code legacyManglePropertyName} lowercases leading uppercase
 * characters.
 *
 * <p>Key Jackson 2 property name mappings tested:
 *
 * <ul>
 *   <li>{@code Service.Abstract} &rarr; {@code "abstract"}
 *   <li>{@code WmsService.SRS} &rarr; {@code "srs"}
 *   <li>{@code WmsService.BBOXForEachCRS} &rarr; {@code "bboxforEachCRS"}
 *   <li>{@code WmsService.GetMapMimeTypes} &rarr; {@code "getMapMimeTypes"}
 *   <li>{@code WmsService.GetMapMimeTypeCheckingEnabled} &rarr; {@code "getMapMimeTypeCheckingEnabled"}
 *   <li>{@code WmsService.GetFeatureInfoMimeTypes} &rarr; {@code "getFeatureInfoMimeTypes"}
 *   <li>{@code WmsService.GetFeatureInfoMimeTypeCheckingEnabled} &rarr; {@code "getFeatureInfoMimeTypeCheckingEnabled"}
 *   <li>{@code WfsService.GML} &rarr; {@code "gml"}
 *   <li>{@code WfsService.SRS} &rarr; {@code "srs"}
 *   <li>{@code WcsService.GMLPrefixing} &rarr; {@code "gmlprefixing"}
 *   <li>{@code WcsService.LatLon} &rarr; {@code "latLon"}
 *   <li>{@code WcsService.SRS} &rarr; {@code "srs"}
 * </ul>
 */
class GeoServerConfigModuleBackwardsCompatibilityTest extends BackwardsCompatibilityTestSupport {

    @Test
    void testGeoServerInfo() throws Exception {
        String json =
                """
                {
                  "@type": "GeoServerInfo",
                  "id": "gs1",
                  "settings": {
                    "@type": "SettingsInfo",
                    "id": "settings1",
                    "title": "GeoServer",
                    "charset": "UTF-8",
                    "numDecimals": 8,
                    "verbose": false,
                    "verboseExceptions": false,
                    "localWorkspaceIncludesPrefix": false,
                    "showCreatedTimeColumnsInAdminList": false,
                    "showModifiedTimeColumnsInAdminList": false,
                    "useHeadersProxyURL": false,
                    "isShowModifiedUserInAdminList": false
                  },
                  "updateSequence": 0,
                  "featureTypeCacheSize": 0,
                  "globalServices": true,
                  "trailingSlashMatch": false
                }
                """;
        GeoServerInfo gs = decode(json, GeoServerInfo.class);
        assertThat(gs.getSettings()).isNotNull();
        assertThat(gs.getSettings().getTitle()).isEqualTo("GeoServer");
    }

    @Test
    void testSettingsInfo() throws Exception {
        String json =
                """
                {
                  "@type": "SettingsInfo",
                  "id": "settings2",
                  "title": "Test Settings",
                  "charset": "UTF-8",
                  "numDecimals": 4,
                  "verbose": false,
                  "verboseExceptions": false,
                  "localWorkspaceIncludesPrefix": false,
                  "showCreatedTimeColumnsInAdminList": false,
                  "showModifiedTimeColumnsInAdminList": false,
                  "useHeadersProxyURL": false,
                  "isShowModifiedUserInAdminList": false
                }
                """;
        SettingsInfo settings = decode(json, SettingsInfo.class);
        assertThat(settings.getTitle()).isEqualTo("Test Settings");
        assertThat(settings.getCharset()).isEqualTo("UTF-8");
    }

    @Test
    void testContactInfo() throws Exception {
        String json =
                """
                {
                  "contactOrganization": "Test Org",
                  "contactPerson": "John Doe",
                  "contactEmail": "john@test.org",
                  "contactVoice": "+1-555-0123",
                  "address": "123 Test St",
                  "addressCity": "Test City",
                  "addressCountry": "US"
                }
                """;
        ContactInfo contact = decode(json, ContactInfo.class);
        assertThat(contact.getContactOrganization()).isEqualTo("Test Org");
        assertThat(contact.getContactPerson()).isEqualTo("John Doe");
    }

    @Test
    void testLoggingInfo() throws Exception {
        String json =
                """
                {
                  "@type": "LoggingInfo",
                  "id": "log1",
                  "level": "DEFAULT_LOGGING.properties",
                  "location": "logs/geoserver.log",
                  "stdOutLogging": true
                }
                """;
        LoggingInfo logging = decode(json, LoggingInfo.class);
        assertThat(logging.getLevel()).isEqualTo("DEFAULT_LOGGING.properties");
        assertThat(logging.isStdOutLogging()).isTrue();
    }

    @Test
    void testWmsServiceInfo_abstract_and_srs() throws Exception {
        String json =
                """
                {
                  "@type": "WMSInfo",
                  "id": "wms1",
                  "name": "WMS",
                  "title": "GeoServer WMS",
                  "citeCompliant": false,
                  "enabled": true,
                  "verbose": false,
                  "srs": ["EPSG:4326", "EPSG:3857"],
                  "maxBuffer": 0,
                  "maxRequestMemory": 0,
                  "maxRenderingTime": 0,
                  "maxRenderingErrors": 0,
                  "featuresReprojectionDisabled": false,
                  "maxRequestedDimensionValues": 0,
                  "remoteStyleMaxRequestTime": 0,
                  "remoteStyleTimeout": 0,
                  "defaultGroupStyleEnabled": false,
                  "autoEscapeTemplateValues": false,
                  "dynamicStylingDisabled": false,
                  "transformFeatureInfoDisabled": false,
                  "abstract": "A WMS service"
                }
                """;
        WMSInfo wms = decode(json, WMSInfo.class);
        assertThat(wms.getAbstract()).isEqualTo("A WMS service");
        assertThat(wms.getSRS()).containsExactly("EPSG:4326", "EPSG:3857");
    }

    @Test
    void testWmsServiceInfo_bboxForEachCRS() throws Exception {
        String json =
                """
                {
                  "@type": "WMSInfo",
                  "id": "wms2",
                  "name": "WMS",
                  "enabled": true,
                  "citeCompliant": false,
                  "verbose": false,
                  "bboxforEachCRS": true,
                  "maxBuffer": 0,
                  "maxRequestMemory": 0,
                  "maxRenderingTime": 0,
                  "maxRenderingErrors": 0,
                  "featuresReprojectionDisabled": false,
                  "maxRequestedDimensionValues": 0,
                  "remoteStyleMaxRequestTime": 0,
                  "remoteStyleTimeout": 0,
                  "defaultGroupStyleEnabled": false,
                  "autoEscapeTemplateValues": false,
                  "dynamicStylingDisabled": false,
                  "transformFeatureInfoDisabled": false
                }
                """;
        WMSInfo wms = decode(json, WMSInfo.class);
        assertThat(wms.isBBOXForEachCRS()).isTrue();
    }

    @Test
    void testWmsServiceInfo_getMapMimeTypes() throws Exception {
        String json =
                """
                {
                  "@type": "WMSInfo",
                  "id": "wms3",
                  "name": "WMS",
                  "enabled": true,
                  "citeCompliant": false,
                  "verbose": false,
                  "getMapMimeTypeCheckingEnabled": true,
                  "getMapMimeTypes": ["image/png", "image/jpeg"],
                  "getFeatureInfoMimeTypeCheckingEnabled": true,
                  "getFeatureInfoMimeTypes": ["text/html", "application/json"],
                  "maxBuffer": 0,
                  "maxRequestMemory": 0,
                  "maxRenderingTime": 0,
                  "maxRenderingErrors": 0,
                  "featuresReprojectionDisabled": false,
                  "maxRequestedDimensionValues": 0,
                  "remoteStyleMaxRequestTime": 0,
                  "remoteStyleTimeout": 0,
                  "defaultGroupStyleEnabled": false,
                  "autoEscapeTemplateValues": false,
                  "dynamicStylingDisabled": false,
                  "transformFeatureInfoDisabled": false
                }
                """;
        WMSInfo wms = decode(json, WMSInfo.class);
        assertThat(wms.isGetMapMimeTypeCheckingEnabled()).isTrue();
        assertThat(wms.getGetMapMimeTypes()).contains("image/png", "image/jpeg");
        assertThat(wms.isGetFeatureInfoMimeTypeCheckingEnabled()).isTrue();
        assertThat(wms.getGetFeatureInfoMimeTypes()).contains("text/html", "application/json");
    }

    @Test
    void testWfsServiceInfo_abstract_gml_srs() throws Exception {
        String json =
                """
                {
                  "@type": "WFSInfo",
                  "id": "wfs1",
                  "name": "WFS",
                  "title": "GeoServer WFS",
                  "citeCompliant": false,
                  "enabled": true,
                  "verbose": false,
                  "maxFeatures": 0,
                  "featureBounding": false,
                  "canonicalSchemaLocation": false,
                  "encodeFeatureMember": false,
                  "hitsIgnoreMaxFeatures": false,
                  "includeWFSRequestDumpFile": false,
                  "simpleConversionEnabled": false,
                  "getFeatureOutputTypeCheckingEnabled": false,
                  "disableStoredQueriesManagement": false,
                  "gml": {
                    "V_10": {
                      "srsNameStyle": "XML",
                      "overrideGMLAttributes": false
                    },
                    "V_20": {
                      "srsNameStyle": "URN2",
                      "overrideGMLAttributes": false
                    }
                  },
                  "srs": ["EPSG:4326", "EPSG:3857"],
                  "abstract": "A WFS service"
                }
                """;
        WFSInfo wfs = decode(json, WFSInfo.class);
        assertThat(wfs.getAbstract()).isEqualTo("A WFS service");
        assertThat(wfs.getGML()).isNotNull();
        assertThat(wfs.getGML()).containsKey(WFSInfo.Version.V_10);
        assertThat(wfs.getGML()).containsKey(WFSInfo.Version.V_20);
        assertThat(wfs.getSRS()).containsExactly("EPSG:4326", "EPSG:3857");
    }

    @Test
    void testWcsServiceInfo_abstract_gmlprefixing_latlon_srs() throws Exception {
        String json =
                """
                {
                  "@type": "WCSInfo",
                  "id": "wcs1",
                  "name": "WCS",
                  "title": "GeoServer WCS",
                  "citeCompliant": false,
                  "enabled": true,
                  "verbose": false,
                  "maxInputMemory": 0,
                  "maxOutputMemory": 0,
                  "subsamplingEnabled": false,
                  "maxRequestedDimensionValues": 0,
                  "defaultDeflateCompressionLevel": 0,
                  "gmlprefixing": true,
                  "latLon": true,
                  "srs": ["EPSG:4326", "EPSG:3857"],
                  "abstract": "A WCS service"
                }
                """;
        WCSInfo wcs = decode(json, WCSInfo.class);
        assertThat(wcs.getAbstract()).isEqualTo("A WCS service");
        assertThat(wcs.isGMLPrefixing()).isTrue();
        assertThat(wcs.isLatLon()).isTrue();
        assertThat(wcs.getSRS()).containsExactly("EPSG:4326", "EPSG:3857");
    }

    @Test
    void testWpsServiceInfo_abstract() throws Exception {
        String json =
                """
                {
                  "@type": "WPSInfo",
                  "id": "wps1",
                  "name": "WPS",
                  "title": "GeoServer WPS",
                  "citeCompliant": false,
                  "enabled": true,
                  "verbose": false,
                  "connectionTimeout": 0.0,
                  "resourceExpirationTimeout": 0,
                  "maxSynchronousProcesses": 1,
                  "maxAsynchronousProcesses": 1,
                  "maxComplexInputSize": 0,
                  "maxAsynchronousExecutionTime": 0,
                  "maxSynchronousExecutionTime": 0,
                  "remoteInputDisabled": false,
                  "abstract": "A WPS service"
                }
                """;
        WPSInfo wps = decode(json, WPSInfo.class);
        assertThat(wps.getAbstract()).isEqualTo("A WPS service");
        assertThat(wps.getName()).isEqualTo("WPS");
    }

    @Test
    void testWmsServiceInfo_full() throws Exception {
        String json =
                """
                {
                  "@type": "WMSInfo",
                  "id": "wms4",
                  "name": "WMS",
                  "title": "GeoServer WMS",
                  "citeCompliant": false,
                  "enabled": true,
                  "verbose": false,
                  "srs": ["EPSG:4326", "EPSG:3857", "EPSG:900913"],
                  "getMapMimeTypeCheckingEnabled": true,
                  "getMapMimeTypes": ["image/png", "image/jpeg", "image/gif"],
                  "getFeatureInfoMimeTypeCheckingEnabled": false,
                  "bboxforEachCRS": false,
                  "maxBuffer": 25,
                  "maxRequestMemory": 65536,
                  "maxRenderingTime": 60,
                  "maxRenderingErrors": 1000,
                  "featuresReprojectionDisabled": false,
                  "maxRequestedDimensionValues": 100,
                  "remoteStyleMaxRequestTime": 60000,
                  "remoteStyleTimeout": 30000,
                  "defaultGroupStyleEnabled": true,
                  "autoEscapeTemplateValues": true,
                  "dynamicStylingDisabled": false,
                  "transformFeatureInfoDisabled": false,
                  "abstract": "Full WMS test"
                }
                """;
        WMSInfo wms = decode(json, WMSInfo.class);
        assertThat(wms.getAbstract()).isEqualTo("Full WMS test");
        assertThat(wms.getSRS()).hasSize(3);
        assertThat(wms.isGetMapMimeTypeCheckingEnabled()).isTrue();
        assertThat(wms.getGetMapMimeTypes()).hasSize(3);
        assertThat(wms.isBBOXForEachCRS()).isFalse();
        assertThat(wms.getMaxBuffer()).isEqualTo(25);
    }
}
