/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.experimental.UtilityClass;
import org.yaml.snakeyaml.DumperOptions.Version;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * @since 1.0
 */
@UtilityClass
public class ObjectMapperUtil {

    public static ObjectMapper newObjectMapper() {
        return newObjectMapper(null);
    }

    public static ObjectMapper newYAMLObjectMapper() {
        YAMLFactory yamlFactory = YAMLFactory.builder() //
                .yamlVersionToWrite(Version.V1_1) //
                .disable(YAMLWriteFeature.USE_NATIVE_TYPE_ID) //
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER) //
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES) //
                .build();
        return newObjectMapper(yamlFactory);
    }

    public static ObjectMapper newObjectMapper(TokenStreamFactory jsonFactory) {
        ObjectMapper objectMapper = new JsonMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.disable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        objectMapper.findAndRegisterModules();

        return objectMapper;
    }
}
