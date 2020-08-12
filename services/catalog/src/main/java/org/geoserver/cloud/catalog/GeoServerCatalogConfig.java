/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog;

import org.geoserver.cloud.autoconfigure.jdbcconfig.JDBCConfigAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JDBCConfigAutoConfiguration.class)
public class GeoServerCatalogConfig {}
