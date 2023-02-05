/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geoserver.cog.CogSettings;
import org.geoserver.cog.CogSettings.RangeReaderType;
import org.geoserver.cog.CogSettingsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test suite for {@link CogSettingsModule} */
class CogSettingsModuleTest {

    private ObjectMapper objectMapper;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void testSPI() {
        assertThat(objectMapper.getRegisteredModuleIds())
                .as("ObjectMapper.findAndRegisterModules() did not find CogSettingsModule")
                .contains("CogSettingsModule");
    }

    @Test
    void testCogSettings() throws JsonProcessingException {
        CogSettings cs = new CogSettings();
        cs.setUseCachingStream(true);

        for (RangeReaderType rtype : RangeReaderType.values()) {
            cs.setRangeReaderSettings(rtype);
            String serialized = objectMapper.writeValueAsString(cs);
            CogSettings read = objectMapper.readValue(serialized, CogSettings.class);
            assertThat(read).isNotInstanceOf(CogSettingsStore.class);
            assertSettings(cs, read);
        }
    }

    @Test
    void testCogSettingsStore() throws JsonProcessingException {
        CogSettingsStore cs = new CogSettingsStore();
        cs.setUseCachingStream(true);

        testCogSettingsStore(cs);

        cs.setUsername("");
        testCogSettingsStore(cs);

        cs.setPassword("");
        testCogSettingsStore(cs);

        cs.setUsername("user");
        cs.setPassword("secret");
        testCogSettingsStore(cs);
    }

    protected void testCogSettingsStore(CogSettingsStore cs)
            throws JsonProcessingException, JsonMappingException {

        String serialized = objectMapper.writeValueAsString(cs);
        CogSettingsStore read = objectMapper.readValue(serialized, CogSettingsStore.class);
        assertSettingsStore(cs, read);
    }

    private void assertSettings(
            org.geoserver.cog.CogSettings expected, org.geoserver.cog.CogSettings actual) {
        assertThat(actual).isNotSameAs(expected);
        // org.geoserver.cog.CogSettings does not implement equals
        assertThat(actual.isUseCachingStream()).isEqualTo(expected.isUseCachingStream());
        assertThat(actual.isUseCachingStream()).isEqualTo(expected.isUseCachingStream());
    }

    private void assertSettingsStore(
            org.geoserver.cog.CogSettingsStore expected,
            org.geoserver.cog.CogSettingsStore actual) {
        assertSettings(expected, actual);
        // org.geoserver.cog.CogSettingsStore does not implement equals
        assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
        assertThat(actual.getPassword()).isEqualTo(expected.getPassword());
    }
}
