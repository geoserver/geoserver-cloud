/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geotools.jackson.databind.util.ObjectMapperUtil;

/**
 * @since 1.0
 */
class GeoSeververConfigModule_JsonTest extends GeoServerConfigModuleTest {

    protected @Override ObjectMapper newObjectMapper() {
        return ObjectMapperUtil.newObjectMapper();
    }
}
