/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.tools;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.web.tools.CatalogBulkLoadToolConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** @see CatalogBulkLoadToolConfiguration */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.web.catalogstresstool.CatalogStressTester")
@ConditionalOnProperty( // enabled by default
        prefix = WebToolsAutoConfiguration.CONFIG_PREFIX,
        name = "catalog-bulk-load",
        havingValue = "true",
        matchIfMissing = true)
@Import(CatalogBulkLoadToolConfiguration.class)
public class CatalogBulkLoadToolAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = WebToolsAutoConfiguration.CONFIG_PREFIX + ".catalog-bulk-load";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
