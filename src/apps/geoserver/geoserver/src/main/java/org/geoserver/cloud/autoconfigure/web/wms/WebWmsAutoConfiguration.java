/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wms;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.web.wms.WebWMSConfiguration;
import org.geoserver.configuration.core.wms.WMS11Configuration;
import org.geoserver.configuration.core.wms.WMS13Configuration;
import org.geoserver.configuration.core.wms.WMSCoreConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** @see WebWMSConfiguration */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wms.web.WMSAdminPage")
@ConditionalOnProperty(
        prefix = WebWmsAutoConfiguration.GEOSERVER_WEBUI_WMS,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({WebWMSConfiguration.class, WMSCoreConfiguration.class, WMS11Configuration.class, WMS13Configuration.class})
public class WebWmsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String GEOSERVER_WEBUI_WMS = "geoserver.web-ui.wms";

    @Override
    public String getConfigPrefix() {
        return GEOSERVER_WEBUI_WMS;
    }
}
