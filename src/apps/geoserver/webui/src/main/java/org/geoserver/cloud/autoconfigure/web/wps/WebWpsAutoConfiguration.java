/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wps;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.wcs.WCSCoreConfiguration;
import org.geoserver.configuration.core.web.wps.WebWPSConfiguration;
import org.geoserver.configuration.extension.wps.WPSCoreConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wps.web.WPSAdminPage")
@ConditionalOnProperty( // enabled by default
        name = "geoserver.web-ui.wps.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({WCSCoreConfiguration.class, WPSCoreConfiguration.class, WebWPSConfiguration.class})
public class WebWpsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    @Getter
    private final String configPrefix = "geoserver.web-ui.wps";
}
