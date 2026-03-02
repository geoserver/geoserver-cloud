/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto;
import org.junit.jupiter.api.Test;

/**
 * Verifies that hardcoded JSON produced by Jackson 2's property naming convention can be
 * deserialized correctly into {@link Patch} objects. This ensures backwards compatibility when
 * upgrading to Jackson 3.
 *
 * <p>Each test uses a hardcoded JSON text block captured from Jackson 2 serialization output, then
 * calls {@link #decode(String, Class)} to verify deserialization.
 *
 * <p>Note: CatalogInfo and ServiceInfo objects used as Patch values are encoded as {@link
 * ResolvingProxyDto} (by type and id), not as full objects. After deserialization, these are converted
 * to proxy Info objects by the {@link org.geoserver.jackson.databind.mapper.PatchMapper}.
 */
class PatchSerializationBackwardsCompatibilityTest extends BackwardsCompatibilityTestSupport {

    @Test
    void patchWithSimpleStringValue() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "abstract",
                      "value": {
                        "Literal": {
                          "type": "java.lang.String",
                          "value": "Test abstract value"
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("abstract");
        assertThat(patch.getValue("abstract")).hasValue("Test abstract value");
    }

    @Test
    void patchWithListOfStrings() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "srs",
                      "value": {
                        "Literal": {
                          "type": "java.util.List",
                          "contentType": "java.lang.String",
                          "value": ["EPSG:4326", "EPSG:3857"]
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("srs");
        assertThat(patch.getValue("srs")).hasValue(List.of("EPSG:4326", "EPSG:3857"));
    }

    @Test
    void patchWithEnumValue() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "type",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.catalog.PublishedType",
                          "value": "VECTOR"
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("type");
        assertThat(patch.getValue("type")).hasValue(PublishedType.VECTOR);
    }

    @Test
    void patchWithFeatureTypeInfo_encodedAsReference() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "resource",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto",
                          "value": {
                            "type": "FEATURETYPE",
                            "id": "ft1"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("resource");
        assertThat(patch.getValue("resource").orElseThrow()).isInstanceOf(FeatureTypeInfo.class);
    }

    @Test
    void patchWithNamespaceInfo_encodedAsReference() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "namespace",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto",
                          "value": {
                            "type": "NAMESPACE",
                            "id": "ns1"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("namespace");
        assertThat(patch.getValue("namespace").orElseThrow()).isInstanceOf(NamespaceInfo.class);
    }

    @Test
    void patchWithCoverageStoreInfo_encodedAsReference() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "store",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto",
                          "value": {
                            "type": "COVERAGESTORE",
                            "id": "cs1"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("store");
        assertThat(patch.getValue("store").orElseThrow()).isInstanceOf(CoverageStoreInfo.class);
    }

    @Test
    void patchWithLayerInfo_encodedAsReference() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "layer",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto",
                          "value": {
                            "type": "LAYER",
                            "id": "layer1"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("layer");
        assertThat(patch.getValue("layer").orElseThrow()).isInstanceOf(LayerInfo.class);
    }

    @Test
    void patchWithMetadataMap() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "metadata",
                      "value": {
                        "Literal": {
                          "type": "java.util.Map",
                          "value": {
                            "k1": {
                              "Literal": {
                                "type": "java.lang.String",
                                "value": "v1"
                              }
                            },
                            "k2": {
                              "Literal": {
                                "type": "java.lang.String",
                                "value": "v2"
                              }
                            }
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("metadata");
        Object value = patch.getValue("metadata").orElseThrow();
        assertThat(value).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        assertThat(map).containsEntry("k1", "v1").containsEntry("k2", "v2");
    }

    @Test
    void patchWithWmsServiceInfo_encodedAsReference() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "wmsService",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto",
                          "value": {
                            "type": "SERVICE",
                            "id": "wms-id"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("wmsService");
        assertThat(patch.getValue("wmsService").orElseThrow()).isInstanceOf(ServiceInfo.class);
    }

    @Test
    void patchWithWfsServiceInfo_encodedAsReference() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "wfsService",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto",
                          "value": {
                            "type": "SERVICE",
                            "id": "wfs-id"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("wfsService");
        assertThat(patch.getValue("wfsService").orElseThrow()).isInstanceOf(ServiceInfo.class);
    }

    @Test
    void patchWithDimensionInfo() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "dimensionInfo",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.catalog.DimensionInfo",
                          "value": {
                            "enabled": true,
                            "attribute": "attribute",
                            "presentation": "DISCRETE_INTERVAL",
                            "resolution": 222.4066,
                            "units": "metre",
                            "unitSymbol": "m",
                            "nearestMatchEnabled": false,
                            "rawNearestMatchEnabled": false,
                            "acceptableInterval": "searchRange",
                            "defaultValueStrategy": "MAXIMUM",
                            "defaultValueReferenceValue": "referenceValue"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("dimensionInfo");
        Object value = patch.getValue("dimensionInfo").orElseThrow();
        assertThat(value).isInstanceOf(DimensionInfo.class);
        DimensionInfo dim = (DimensionInfo) value;
        assertThat(dim.isEnabled()).isTrue();
        assertThat(dim.getAttribute()).isEqualTo("attribute");
        assertThat(dim.getUnits()).isEqualTo("metre");
    }

    @Test
    void patchWithContactInfo() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "contact",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.config.ContactInfo",
                          "value": {
                            "id": "contact-1",
                            "address": "123 Main St",
                            "addressCity": "Springfield",
                            "addressCountry": "US",
                            "addressDeliveryPoint": "Suite 100",
                            "addressPostalCode": "12345",
                            "addressState": "IL",
                            "contactFacsimile": "555-0102",
                            "contactOrganization": "Test Org",
                            "contactPerson": "John Doe",
                            "contactVoice": "555-0101",
                            "onlineResource": "www.example.com"
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("contact");
        Object value = patch.getValue("contact").orElseThrow();
        assertThat(value).isInstanceOf(ContactInfo.class);
        ContactInfo contact = (ContactInfo) value;
        assertThat(contact.getContactPerson()).isEqualTo("John Doe");
        assertThat(contact.getContactOrganization()).isEqualTo("Test Org");
        assertThat(contact.getAddressCity()).isEqualTo("Springfield");
    }

    @Test
    void patchWithAttributionInfo() throws Exception {
        String json =
                """
                {
                  "Patch": {
                    "patches": [{
                      "name": "attribution",
                      "value": {
                        "Literal": {
                          "type": "org.geoserver.catalog.impl.AttributionInfoImpl",
                          "value": {
                            "id": "attr-1",
                            "title": "Test Attribution",
                            "href": "www.example.com",
                            "logoWidth": 345,
                            "logoHeight": 327
                          }
                        }
                      }
                    }]
                  }
                }
                """;
        Patch patch = decode(json, Patch.class);
        assertThat(patch.getPropertyNames()).containsExactly("attribution");
        Object value = patch.getValue("attribution").orElseThrow();
        assertThat(value).isInstanceOf(AttributionInfo.class);
        AttributionInfo attr = (AttributionInfo) value;
        assertThat(attr.getTitle()).isEqualTo("Test Attribution");
        assertThat(attr.getLogoWidth()).isEqualTo(345);
        assertThat(attr.getLogoHeight()).isEqualTo(327);
    }
}
