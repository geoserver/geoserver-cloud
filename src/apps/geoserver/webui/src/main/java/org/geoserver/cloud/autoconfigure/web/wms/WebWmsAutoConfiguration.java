/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.web.wms;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.web.wms.WebWMSConfiguration;
import org.geoserver.configuration.core.wms.WMSCoreMinimalConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see WebWMSConfiguration
 * @see WMSCoreMinimalConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wms.web.WMSAdminPage")
@ConditionalOnProperty( // enabled by default
        name = "geoserver.web-ui.wms.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({WebWMSConfiguration.class, WMSCoreMinimalConfiguration.class})
public class WebWmsAutoConfiguration extends AbstractWebUIAutoConfiguration {
    @Getter
    private final String configPrefix = "geoserver.web-ui.wms";
}
