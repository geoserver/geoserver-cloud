/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.demo;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.web.tools.ReprojectionConsoleConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.web.demo.SRSListPage")
@ConditionalOnProperty( // enabled by default
        prefix = WebDemosAutoConfiguration.CONFIG_PREFIX,
        name = "reprojection-console",
        havingValue = "true",
        matchIfMissing = true)
@Import(ReprojectionConsoleConfiguration.class)
public class ReprojectionConsoleAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = WebDemosAutoConfiguration.CONFIG_PREFIX + ".reprojection-console";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
