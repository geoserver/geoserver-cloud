/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.experimental.UtilityClass;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * @since 1.0
 */
@UtilityClass
public class ObjectMapperUtil {

    public static ObjectMapper newObjectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .changeDefaultPropertyInclusion(v -> v.withValueInclusion(Include.NON_EMPTY))
                .disable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                // helps when reverting to a prior version
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public static ObjectMapper newYAMLObjectMapper() {
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .disable(YAMLWriteFeature.USE_NATIVE_TYPE_ID)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .build();

        return YAMLMapper.builder(yamlFactory)
                .findAndAddModules()
                .changeDefaultPropertyInclusion(v -> v.withValueInclusion(Include.NON_EMPTY))
                .disable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build();
    }
}
