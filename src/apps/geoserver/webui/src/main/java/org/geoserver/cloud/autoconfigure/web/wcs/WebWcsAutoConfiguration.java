/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wcs;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.wcs.WCSConfiguration;
import org.geoserver.configuration.core.web.wcs.WebWCSConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wcs.web.WCSAdminPage")
@ConditionalOnProperty( // enabled by default
        name = "geoserver.web-ui.wcs.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({WCSConfiguration.class, WebWCSConfiguration.class})
public class WebWcsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    @Getter
    private final String configPrefix = "geoserver.web-ui.wcs";
}
