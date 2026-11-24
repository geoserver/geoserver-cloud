/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wcs;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.wcs.WCS20Configuration;
import org.geoserver.configuration.core.wcs.WCSCoreConfiguration;
import org.geoserver.configuration.core.web.wcs.WebWCSConfiguration;
import org.geoserver.configuration.extension.wcs.WCS10Configuration;
import org.geoserver.configuration.extension.wcs.WCS11Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** @see WebWCSConfiguration */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wcs.web.WCSAdminPage")
@ConditionalOnProperty( // enabled by default
        prefix = WebWcsAutoConfiguration.GEOSERVER_WEBUI_WCS,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({
    WebWCSConfiguration.class,
    WCSCoreConfiguration.class,
    WCS10Configuration.class,
    WCS11Configuration.class,
    WCS20Configuration.class
})
public class WebWcsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String GEOSERVER_WEBUI_WCS = "geoserver.web-ui.wcs";

    @Override
    public String getConfigPrefix() {
        return GEOSERVER_WEBUI_WCS;
    }
}
