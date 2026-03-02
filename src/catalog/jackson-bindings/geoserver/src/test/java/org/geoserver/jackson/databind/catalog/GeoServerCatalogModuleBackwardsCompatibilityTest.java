/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.jackson.databind.dto.CoordinateReferenceSystemDto;
import org.junit.jupiter.api.Test;

/**
 * Verifies that JSON produced by Jackson 2's property naming convention (lowercase leading
 * characters) can be deserialized correctly. These tests use hardcoded JSON text blocks with the
 * Jackson 2 property names and the {@code @type} property for type discrimination.
 *
 * <p>Key Jackson 2 property name mappings tested:
 *
 * <ul>
 *   <li>{@code CRS.WKT} &rarr; {@code "wkt"}
 *   <li>{@code Resource.SRS} &rarr; {@code "srs"}
 *   <li>{@code Resource.Abstract} &rarr; {@code "abstract"}
 *   <li>{@code Namespace.URI} &rarr; {@code "uri"}
 *   <li>{@code CoverageStore.URL} &rarr; {@code "url"}
 *   <li>{@code Published.Abstract} &rarr; {@code "abstract"}
 *   <li>{@code LayerGroupStyle.Abstract} &rarr; {@code "abstract"}
 *   <li>{@code Filter.NativeFilter.Native} &rarr; {@code "native"}
 * </ul>
 */
class GeoServerCatalogModuleBackwardsCompatibilityTest extends BackwardsCompatibilityTestSupport {

    @Test
    void testWorkspaceInfo() throws Exception {
        String json =
                """
                {
                  "@type": "WorkspaceInfo",
                  "id": "ws1",
                  "name": "testWs",
                  "isolated": false
                }
                """;
        WorkspaceInfo ws = decode(json, WorkspaceInfo.class);
        assertThat(ws.getName()).isEqualTo("testWs");
        assertThat(ws.isIsolated()).isFalse();
    }

    @Test
    void testNamespaceInfo_uri() throws Exception {
        String json =
                """
                {
                  "@type": "NamespaceInfo",
                  "id": "ns1",
                  "name": "testNs",
                  "uri": "http://test.namespace.com",
                  "isolated": false
                }
                """;
        NamespaceInfo ns = decode(json, NamespaceInfo.class);
        assertThat(ns.getName()).isEqualTo("testNs");
        assertThat(ns.getURI()).isEqualTo("http://test.namespace.com");
        assertThat(ns.isIsolated()).isFalse();
    }

    @Test
    void testDataStoreInfo() throws Exception {
        String json =
                """
                {
                  "@type": "DataStoreInfo",
                  "id": "ds1",
                  "name": "testDs",
                  "workspace": "ws1",
                  "enabled": true,
                  "connectionParameters": {},
                  "disableOnConnFailure": false
                }
                """;
        DataStoreInfo ds = decode(json, DataStoreInfo.class);
        assertThat(ds.getName()).isEqualTo("testDs");
        assertThat(ds.isEnabled()).isTrue();
    }

    @Test
    void testCoverageStoreInfo_url() throws Exception {
        String json =
                """
                {
                  "@type": "CoverageStoreInfo",
                  "id": "cs1",
                  "name": "testCs",
                  "workspace": "ws1",
                  "enabled": true,
                  "connectionParameters": {},
                  "disableOnConnFailure": false,
                  "url": "file:///data/raster.tif"
                }
                """;
        CoverageStoreInfo cs = decode(json, CoverageStoreInfo.class);
        assertThat(cs.getName()).isEqualTo("testCs");
        assertThat(cs.getURL()).isEqualTo("file:///data/raster.tif");
    }

    @Test
    void testWMSStoreInfo() throws Exception {
        String json =
                """
                {
                  "@type": "WMSStoreInfo",
                  "id": "wmsst1",
                  "name": "testWmsStore",
                  "workspace": "ws1",
                  "enabled": true,
                  "connectionParameters": {},
                  "disableOnConnFailure": false,
                  "capabilitiesURL": "http://example.com/wms?service=WMS&version=1.1.1&request=GetCapabilities",
                  "maxConnections": 6,
                  "readTimeout": 60,
                  "connectTimeout": 30
                }
                """;
        WMSStoreInfo store = decode(json, WMSStoreInfo.class);
        assertThat(store.getName()).isEqualTo("testWmsStore");
    }

    @Test
    void testWMTSStoreInfo() throws Exception {
        String json =
                """
                {
                  "@type": "WMTSStoreInfo",
                  "id": "wmtsst1",
                  "name": "testWmtsStore",
                  "workspace": "ws1",
                  "enabled": true,
                  "connectionParameters": {},
                  "disableOnConnFailure": false,
                  "capabilitiesURL": "http://example.com/wmts?service=WMTS&request=GetCapabilities",
                  "maxConnections": 6,
                  "readTimeout": 60,
                  "connectTimeout": 30
                }
                """;
        WMTSStoreInfo store = decode(json, WMTSStoreInfo.class);
        assertThat(store.getName()).isEqualTo("testWmtsStore");
    }

    @Test
    void testFeatureTypeInfo_abstract_and_srs() throws Exception {
        String json =
                """
                {
                  "@type": "FeatureTypeInfo",
                  "id": "ft1",
                  "name": "testFt",
                  "namespace": "ns1",
                  "store": "ds1",
                  "nativeName": "test_table",
                  "title": "Test Feature Type",
                  "abstract": "A test feature type",
                  "srs": "EPSG:4326",
                  "enabled": true,
                  "serviceConfiguration": false,
                  "maxFeatures": 0,
                  "numDecimals": 0,
                  "padWithZeros": false,
                  "forcedDecimal": false,
                  "overridingServiceSRS": false,
                  "skipNumberMatched": false,
                  "circularArcPresent": false,
                  "encodeMeasures": false
                }
                """;
        FeatureTypeInfo ft = decode(json, FeatureTypeInfo.class);
        assertThat(ft.getName()).isEqualTo("testFt");
        assertThat(ft.getAbstract()).isEqualTo("A test feature type");
        assertThat(ft.getSRS()).isEqualTo("EPSG:4326");
    }

    @Test
    void testCoverageInfo_abstract_and_srs() throws Exception {
        String json =
                """
                {
                  "@type": "CoverageInfo",
                  "id": "cov1",
                  "name": "testCoverage",
                  "namespace": "ns1",
                  "store": "cs1",
                  "nativeName": "test_coverage",
                  "title": "Test Coverage",
                  "abstract": "A test coverage",
                  "srs": "EPSG:4326",
                  "enabled": true,
                  "serviceConfiguration": false
                }
                """;
        CoverageInfo c = decode(json, CoverageInfo.class);
        assertThat(c.getName()).isEqualTo("testCoverage");
        assertThat(c.getAbstract()).isEqualTo("A test coverage");
        assertThat(c.getSRS()).isEqualTo("EPSG:4326");
    }

    @Test
    void testWMSLayerInfo_abstract_and_srs() throws Exception {
        String json =
                """
                {
                  "@type": "WMSLayerInfo",
                  "id": "wmsl1",
                  "name": "testWmsLayer",
                  "namespace": "ns1",
                  "store": "wmsst1",
                  "nativeName": "remote_layer",
                  "title": "Test WMS Layer",
                  "abstract": "A remote WMS layer",
                  "srs": "EPSG:4326",
                  "enabled": true,
                  "serviceConfiguration": false,
                  "metadataBBoxRespected": false
                }
                """;
        WMSLayerInfo layer = decode(json, WMSLayerInfo.class);
        assertThat(layer.getName()).isEqualTo("testWmsLayer");
        assertThat(layer.getAbstract()).isEqualTo("A remote WMS layer");
        assertThat(layer.getSRS()).isEqualTo("EPSG:4326");
    }

    @Test
    void testWMTSLayerInfo_abstract_and_srs() throws Exception {
        String json =
                """
                {
                  "@type": "WMTSLayerInfo",
                  "id": "wmtsl1",
                  "name": "testWmtsLayer",
                  "namespace": "ns1",
                  "store": "wmtsst1",
                  "nativeName": "remote_wmts_layer",
                  "title": "Test WMTS Layer",
                  "abstract": "A remote WMTS layer",
                  "srs": "EPSG:4326",
                  "enabled": true,
                  "serviceConfiguration": false
                }
                """;
        WMTSLayerInfo layer = decode(json, WMTSLayerInfo.class);
        assertThat(layer.getName()).isEqualTo("testWmtsLayer");
        assertThat(layer.getAbstract()).isEqualTo("A remote WMTS layer");
        assertThat(layer.getSRS()).isEqualTo("EPSG:4326");
    }

    @Test
    void testLayerInfo_abstract() throws Exception {
        String json =
                """
                {
                  "@type": "LayerInfo",
                  "id": "layer1",
                  "name": "testLayer",
                  "title": "Test Layer",
                  "abstract": "A test layer",
                  "enabled": true,
                  "advertised": true,
                  "resource": "ft1",
                  "defaultStyle": "style1",
                  "type": "VECTOR"
                }
                """;
        LayerInfo layer = decode(json, LayerInfo.class);
        assertThat(layer.getId()).isEqualTo("layer1");
        assertThat(layer.getType()).isEqualTo(org.geoserver.catalog.PublishedType.VECTOR);
    }

    @Test
    void testLayerGroupInfo_abstract_and_layerGroupStyleAbstract() throws Exception {
        String json =
                """
                {
                  "@type": "LayerGroupInfo",
                  "id": "lg1",
                  "name": "testLayerGroup",
                  "title": "Test LG",
                  "abstract": "A test layer group",
                  "enabled": true,
                  "advertised": true,
                  "mode": "SINGLE",
                  "layers": ["layer1", "layer2"],
                  "styles": ["style1", "style2"],
                  "layerGroupStyles": [{
                    "id": "lgs1",
                    "title": "LGS Title",
                    "abstract": "LGS Abstract",
                    "layers": ["layer1"],
                    "styles": ["style1"]
                  }]
                }
                """;
        LayerGroupInfo lg = decode(json, LayerGroupInfo.class);
        assertThat(lg.getName()).isEqualTo("testLayerGroup");
        assertThat(lg.getAbstract()).isEqualTo("A test layer group");
        assertThat(lg.getLayerGroupStyles()).hasSize(1);
        assertThat(lg.getLayerGroupStyles().get(0).getAbstract()).isEqualTo("LGS Abstract");
    }

    @Test
    void testStyleInfo() throws Exception {
        String json =
                """
                {
                  "@type": "StyleInfo",
                  "id": "style1",
                  "name": "testStyle",
                  "format": "sld",
                  "filename": "testStyle.sld"
                }
                """;
        StyleInfo style = decode(json, StyleInfo.class);
        assertThat(style.getName()).isEqualTo("testStyle");
        assertThat(style.getFormat()).isEqualTo("sld");
    }

    @Test
    void testCRS_wkt() throws Exception {
        String json =
                """
                {
                  "srs": "EPSG:4326",
                  "wkt": "GEOGCS[\\"WGS 84\\"]"
                }
                """;
        CoordinateReferenceSystemDto crs = decode(json, CoordinateReferenceSystemDto.class);
        assertThat(crs.getSrs()).isEqualTo("EPSG:4326");
        assertThat(crs.getWKT()).isEqualTo("GEOGCS[\"WGS 84\"]");
    }

    @Test
    void testCRS_srsOnly() throws Exception {
        String json = """
                {
                  "srs": "EPSG:3857"
                }
                """;
        CoordinateReferenceSystemDto crs = decode(json, CoordinateReferenceSystemDto.class);
        assertThat(crs.getSrs()).isEqualTo("EPSG:3857");
    }

    @Test
    void testFeatureTypeInfo_with_nativeCRS() throws Exception {
        String json =
                """
                {
                  "@type": "FeatureTypeInfo",
                  "id": "ft2",
                  "name": "testFt",
                  "namespace": "ns1",
                  "store": "ds1",
                  "nativeName": "test_table",
                  "title": "Test FT",
                  "abstract": "Abstract text",
                  "nativeCRS": {
                    "srs": "EPSG:4326",
                    "wkt": "GEOGCS[\\"WGS 84\\"]"
                  },
                  "srs": "EPSG:4326",
                  "enabled": true,
                  "serviceConfiguration": false,
                  "maxFeatures": 0,
                  "numDecimals": 0,
                  "padWithZeros": false,
                  "forcedDecimal": false,
                  "overridingServiceSRS": false,
                  "skipNumberMatched": false,
                  "circularArcPresent": false,
                  "encodeMeasures": false
                }
                """;
        FeatureTypeInfo ft = decode(json, FeatureTypeInfo.class);
        assertThat(ft.getAbstract()).isEqualTo("Abstract text");
        assertThat(ft.getSRS()).isEqualTo("EPSG:4326");
        assertThat(ft.getNativeCRS()).isNotNull();
    }

    @Test
    void testFeatureTypeInfo_with_keywords_and_metadata() throws Exception {
        String json =
                """
                {
                  "@type": "FeatureTypeInfo",
                  "id": "ft3",
                  "name": "testFt",
                  "namespace": "ns1",
                  "store": "ds1",
                  "nativeName": "test_table",
                  "title": "Test FT",
                  "abstract": "My abstract",
                  "keywords": [
                    {"value": "keyword1", "language": "en"},
                    {"value": "keyword2"}
                  ],
                  "srs": "EPSG:4326",
                  "enabled": true,
                  "serviceConfiguration": false,
                  "metadata": {
                    "MetadataMap": {
                      "k1": {
                        "Literal": {
                          "type": "java.lang.String",
                          "value": "v1"
                        }
                      }
                    }
                  },
                  "maxFeatures": 0,
                  "numDecimals": 0,
                  "padWithZeros": false,
                  "forcedDecimal": false,
                  "overridingServiceSRS": false,
                  "skipNumberMatched": false,
                  "circularArcPresent": false,
                  "encodeMeasures": false
                }
                """;
        FeatureTypeInfo ft = decode(json, FeatureTypeInfo.class);
        assertThat(ft.getAbstract()).isEqualTo("My abstract");
        assertThat(ft.getKeywords()).hasSize(2);
        assertThat(ft.getMetadata()).containsKey("k1");
    }

    @Test
    void testNativeFilter_native() throws Exception {
        String json =
                """
                {
                  "NativeFilter": {
                    "native": "SELECT * FROM test"
                  }
                }
                """;
        org.geotools.jackson.databind.filter.dto.FilterDto filter =
                decode(json, org.geotools.jackson.databind.filter.dto.FilterDto.class);
        assertFilterIsNative(filter, "SELECT * FROM test");
    }

    @Test
    void testLayerGroupInfo_with_internationalStrings() throws Exception {
        String json =
                """
                {
                  "@type": "LayerGroupInfo",
                  "id": "lg2",
                  "name": "i18nLg",
                  "title": "LG Title",
                  "abstract": "LG Abstract",
                  "enabled": true,
                  "advertised": true,
                  "mode": "SINGLE",
                  "layers": ["layer1"],
                  "styles": ["style1"],
                  "internationalTitle": {
                    "en": "English Title",
                    "fr": "French Title"
                  },
                  "internationalAbstract": {
                    "en": "English Abstract",
                    "fr": "French Abstract"
                  }
                }
                """;
        LayerGroupInfo lg = decode(json, LayerGroupInfo.class);
        assertThat(lg.getAbstract()).isEqualTo("LG Abstract");
    }

    @Test
    void testCoverageInfo_with_grid() throws Exception {
        String json =
                """
                {
                  "@type": "CoverageInfo",
                  "id": "cov2",
                  "name": "testCov",
                  "namespace": "ns1",
                  "store": "cs1",
                  "nativeName": "test_cov",
                  "title": "Test Coverage",
                  "abstract": "Coverage abstract",
                  "srs": "EPSG:4326",
                  "enabled": true,
                  "serviceConfiguration": false,
                  "nativeFormat": "GeoTIFF",
                  "grid": {
                    "low": [0, 0],
                    "high": [1024, 768],
                    "transform": [0.3515625, 0.0, 0.0, -0.234375, -179.82421875, 89.8828125],
                    "crs": {
                      "srs": "EPSG:4326"
                    }
                  }
                }
                """;
        CoverageInfo c = decode(json, CoverageInfo.class);
        assertThat(c.getAbstract()).isEqualTo("Coverage abstract");
        assertThat(c.getSRS()).isEqualTo("EPSG:4326");
        assertThat(c.getNativeFormat()).isEqualTo("GeoTIFF");
    }

    @Test
    void testCoverageStoreInfo_url_with_metadata() throws Exception {
        String json =
                """
                {
                  "@type": "CoverageStoreInfo",
                  "id": "cs2",
                  "name": "testCs",
                  "workspace": "ws1",
                  "enabled": true,
                  "connectionParameters": {},
                  "disableOnConnFailure": false,
                  "url": "file:///data/raster.tif",
                  "metadata": {
                    "MetadataMap": {
                      "indexingEnabled": {
                        "Literal": {
                          "type": "java.lang.Boolean",
                          "value": "true"
                        }
                      }
                    }
                  }
                }
                """;
        CoverageStoreInfo cs = decode(json, CoverageStoreInfo.class);
        assertThat(cs.getURL()).isEqualTo("file:///data/raster.tif");
        assertThat(cs.getMetadata()).containsKey("indexingEnabled");
    }
}
