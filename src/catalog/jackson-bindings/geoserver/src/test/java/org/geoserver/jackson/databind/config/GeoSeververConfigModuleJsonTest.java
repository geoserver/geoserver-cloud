/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geoserver.config.GeoServerInfo;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;

/**
 * @since 1.0
 */
class GeoSeververConfigModuleJsonTest extends GeoServerConfigModuleTest {

    protected @Override ObjectMapper newObjectMapper() {
        return ObjectMapperUtil.newObjectMapper();
    }

    /**
     * Test backward compatibility: verify that old JSON documents with "jai" field
     * can be deserialized to the new "imageProcessing" field
     */
    @Test
    void geoServerInfo_backwardCompatibility_jaiField() throws Exception {
        // JSON with old "jai" field name (pre-2.28.0 format)
        String oldFormatJson =
                """
                {
                  "@type": "GeoServerInfo",
                  "id": "test-id",
                  "jai": {
                    "allowInterpolation": true,
                    "recycling": false,
                    "tilePriority": 5,
                    "tileThreads": 7,
                    "memoryCapacity": 0.5,
                    "memoryThreshold": 0.75,
                    "pngEncoderType": "PNGJ"
                  },
                  "updateSequence": 123,
                  "featureTypeCacheSize": 100,
                  "globalServices": true,
                  "trailingSlashMatch": false
                }
                """;

        GeoServerInfo decoded = objectMapper.readValue(oldFormatJson, GeoServerInfo.class);

        // Verify that the old "jai" field was correctly mapped to imageProcessing
        assertEquals("test-id", decoded.getId());
        var imageProcessing = decoded.getImageProcessing();

        // Verify the imageProcessing object was deserialized correctly
        assertNotNull(imageProcessing, "imageProcessing should not be null");
        assertEquals(5, imageProcessing.getTilePriority());
        assertEquals(7, imageProcessing.getTileThreads());
        assertEquals(0.5, imageProcessing.getMemoryCapacity(), 0.001);
        assertEquals(0.75, imageProcessing.getMemoryThreshold(), 0.001);
    }

    /**
     * Test backward compatibility: verify that old JSON documents with "xmlExternalEntitiesEnabled" field
     * can be deserialized though the propery has been deprecated in 2.28.0
     */
    @Test
    void geoServerInfo_backwardCompatibility_xmlExternalEntitiesEnabled() throws Exception {
        // JSON with old "jai" field name (pre-2.28.0 format)
        String oldFormatJson =
                """
                {
                  "@type": "GeoServerInfo",
                  "id": "test-id",
                  "updateSequence": 123,
                  "featureTypeCacheSize": 100,
                  "globalServices": true,
                  "trailingSlashMatch": false,
                  "xmlExternalEntitiesEnabled": true
                }
                """;

        GeoServerInfo decoded = objectMapper.readValue(oldFormatJson, GeoServerInfo.class);
        assertEquals("test-id", decoded.getId());
        assertEquals(123, decoded.getUpdateSequence());
        assertEquals(100, decoded.getFeatureTypeCacheSize());
    }
}
