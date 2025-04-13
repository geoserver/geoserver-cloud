/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import org.geoserver.cloud.autoconfigure.core.GeoServerWebMvcMainAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.DemosAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.extension.ExtensionsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.security.SecurityAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.tools.ToolsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wcs.WcsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wfs.WfsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wms.WmsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wps.WpsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@AutoConfiguration(after = {GeoServerWebMvcMainAutoConfiguration.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Import({ //
    WebCoreConfiguration.class, // this one is mandatory
    SecurityAutoConfiguration.class,
    WfsAutoConfiguration.class,
    WmsAutoConfiguration.class,
    WcsAutoConfiguration.class,
    WpsAutoConfiguration.class,
    ExtensionsAutoConfiguration.class,
    DemosAutoConfiguration.class,
    ToolsAutoConfiguration.class
})
public class WebUIApplicationAutoConfiguration {

    @Bean
    WebUIInitializer webUIDefaultsInitializer(Environment env) {
        return new WebUIInitializer(env);
    }
}
