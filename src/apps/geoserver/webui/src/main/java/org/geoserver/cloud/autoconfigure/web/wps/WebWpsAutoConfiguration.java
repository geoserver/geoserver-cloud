/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wps;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.wcs.WCSCoreConfiguration;
import org.geoserver.configuration.core.web.wps.WebWPSConfiguration;
import org.geoserver.configuration.extension.wps.WPSCoreConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports {@link WCSCoreConfiguration} because the WPS core beans depend on the WCS coverage cleaner. The WCS pages can
 * be disabled while the WPS pages are enabled.
 *
 * @see WebWPSConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wps.web.WPSAdminPage")
@ConditionalOnProperty( // enabled by default
        prefix = WebWpsAutoConfiguration.GEOSERVER_WEBUI_WPS,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({WebWPSConfiguration.class, WPSCoreConfiguration.class, WCSCoreConfiguration.class})
public class WebWpsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String GEOSERVER_WEBUI_WPS = "geoserver.web-ui.wps";

    @Override
    public String getConfigPrefix() {
        return GEOSERVER_WEBUI_WPS;
    }
}
