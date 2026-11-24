/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.demo;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.web.demo.WCSRequestBuilderConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** @see WCSRequestBuilderConfiguration */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wcs.web.demo.WCSRequestBuilder")
@ConditionalOnProperty( // enabled by default
        prefix = WebDemosAutoConfiguration.CONFIG_PREFIX,
        name = "wcs-request-builder",
        havingValue = "true",
        matchIfMissing = true)
@Import(WCSRequestBuilderConfiguration.class)
public class WcsRequestBuilderAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = WebDemosAutoConfiguration.CONFIG_PREFIX + ".wcs-request-builder";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
