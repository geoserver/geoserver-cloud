/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.tools;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.tools.WebToolsAutoConfiguration.CatalogBulkLoadToolConfiguration;
import org.geoserver.cloud.autoconfigure.web.tools.WebToolsAutoConfiguration.ReprojectionConsoleConfiguration;
import org.geoserver.cloud.autoconfigure.web.tools.WebToolsAutoConfiguration.ResourceBrowserConfiguration;
import org.geoserver.configuration.core.web.tools.WebToolsCatalogBulkLoadToolConfiguration;
import org.geoserver.configuration.core.web.tools.WebToolsReprojectionConsoleConfiguration;
import org.geoserver.configuration.extension.resourcebrowser.WebToolsResourceBrowserConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "geoserver.web-ui.tools.enabled", havingValue = "true", matchIfMissing = true)
@Import({
    ResourceBrowserConfiguration.class,
    CatalogBulkLoadToolConfiguration.class,
    ReprojectionConsoleConfiguration.class
})
public class WebToolsAutoConfiguration {

    /**
     * Configuration to enable the <a href=
     * "https://docs.geoserver.org/latest/en/user/configuration/tools/resource/browser.html">Resource
     * Browser</a> extension in the Web UI.
     *
     * @see WebToolsResourceBrowserConfiguration
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.geoserver.web.resources.PageResourceBrowser")
    @ConditionalOnProperty( // enabled by default
            name = "geoserver.web-ui.tools.resource-browser",
            havingValue = "true",
            matchIfMissing = true)
    @Import(WebToolsResourceBrowserConfiguration.class)
    static class ResourceBrowserConfiguration extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.tools.resource-browser";
    }

    /**
     * @see WebToolsReprojectionConsoleConfiguration
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.geoserver.web.catalogstresstool.CatalogStressTester")
    @ConditionalOnProperty( // enabled by default
            name = "geoserver.web-ui.tools.reprojection-console",
            havingValue = "true",
            matchIfMissing = true)
    @Import(WebToolsReprojectionConsoleConfiguration.class)
    static class ReprojectionConsoleConfiguration extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.tools.reprojection-console";
    }

    /**
     * @see WebToolsCatalogBulkLoadToolConfiguration
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.geoserver.web.catalogstresstool.CatalogStressTester")
    @ConditionalOnProperty( // enabled by default
            name = "geoserver.web-ui.tools.catalog-bulk-load",
            havingValue = "true",
            matchIfMissing = true)
    @Import(WebToolsCatalogBulkLoadToolConfiguration.class)
    static class CatalogBulkLoadToolConfiguration extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.tools.catalog-bulk-load";
    }
}
