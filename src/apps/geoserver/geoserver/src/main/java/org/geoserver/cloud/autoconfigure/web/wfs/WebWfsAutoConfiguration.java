/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wfs;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.web.wfs.WebWFSConfiguration;
import org.geoserver.configuration.core.wfs.WFS1xConfiguration;
import org.geoserver.configuration.core.wfs.WFS2xConfiguration;
import org.geoserver.configuration.core.wfs.WFSCoreConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see WebWFSConfiguration
 * @see WFSCoreConfiguration
 * @see WFS1xConfiguration
 * @see WFS2xConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wfs.web.WFSAdminPage")
@ConditionalOnProperty( // enabled by default
        prefix = WebWfsAutoConfiguration.GEOSEVER_WEBUI_WFS,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({WebWFSConfiguration.class, WFSCoreConfiguration.class, WFS1xConfiguration.class, WFS2xConfiguration.class})
public class WebWfsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String GEOSEVER_WEBUI_WFS = "geoserver.web-ui.wfs";

    @Override
    public String getConfigPrefix() {
        return GEOSEVER_WEBUI_WFS;
    }
}
