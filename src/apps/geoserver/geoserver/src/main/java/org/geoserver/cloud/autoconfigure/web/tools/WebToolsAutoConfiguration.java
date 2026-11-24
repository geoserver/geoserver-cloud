/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.tools;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = WebToolsAutoConfiguration.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({ResourceBrowserAutoConfiguration.class, CatalogBulkLoadToolAutoConfiguration.class})
public class WebToolsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.web-ui.tools";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
