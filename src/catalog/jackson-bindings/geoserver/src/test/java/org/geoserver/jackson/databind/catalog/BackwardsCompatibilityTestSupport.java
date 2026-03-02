/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeEach;
import tools.jackson.databind.ObjectMapper;

/**
 * Base class for backwards-compatibility tests that verify deserialization from known JSON text
 * blocks produced by Jackson 2.
 *
 * <p>These tests ensure that JSON property names produced by Jackson 2's {@code
 * legacyManglePropertyName} behavior (which lowercases leading uppercase characters in field names)
 * are correctly deserialized. This is important for maintaining wire-format compatibility when
 * upgrading to Jackson 3, which preserves field name casing.
 */
public abstract class BackwardsCompatibilityTestSupport {

    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperUtil.newObjectMapper();
    }

    protected <T> T decode(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }

    protected void assertFilterIsInclude(org.geotools.jackson.databind.filter.dto.Filter filter) {
        assertThat(filter).isInstanceOf(org.geotools.jackson.databind.filter.dto.Filter.IncludeFilter.class);
    }

    protected void assertFilterIsNative(org.geotools.jackson.databind.filter.dto.Filter filter, String expectedNative) {
        assertThat(filter).isInstanceOf(org.geotools.jackson.databind.filter.dto.Filter.NativeFilter.class);
        assertThat(((org.geotools.jackson.databind.filter.dto.Filter.NativeFilter) filter).getNative())
                .isEqualTo(expectedNative);
    }
}
