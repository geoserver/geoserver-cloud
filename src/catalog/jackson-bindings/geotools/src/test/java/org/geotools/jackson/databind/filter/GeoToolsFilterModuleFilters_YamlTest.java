/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geotools.jackson.databind.util.ObjectMapperUtil;

/**
 * @since 1.0
 */
class GeoToolsFilterModuleFilters_YamlTest extends GeoToolsFilterModuleFiltersTest {

    protected @Override ObjectMapper newObjectMapper() {
        return ObjectMapperUtil.newYAMLObjectMapper();
    }
}
