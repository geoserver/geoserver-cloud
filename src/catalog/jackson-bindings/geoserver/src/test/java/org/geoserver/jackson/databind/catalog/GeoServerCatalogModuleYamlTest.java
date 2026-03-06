/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import org.geotools.jackson.databind.util.ObjectMapperUtil;
import tools.jackson.databind.ObjectMapper;

/** @since 1.0 */
class GeoServerCatalogModuleYamlTest extends GeoServerCatalogModuleTest {

    protected @Override ObjectMapper newObjectMapper() {
        return ObjectMapperUtil.newYAMLObjectMapper();
    }
}
