/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.resource;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.configuration.webui.WebResourceBrowserConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Spring auto-configuration to enable <a href=
 * "https://docs.geoserver.org/latest/en/user/configuration/tools/resource/browser.html">Resource
 * Browser</a> extension in the Web UI.
 *
 * <p>Enabled by default depending on whether the extension is in the classpath, can be disabled
 * through the {@code geoserver.web.resource-browser.enabled=false} property.
 *
 * @see WebResourceBrowserConfiguration
 */
@ConditionalOnClass(org.geoserver.web.resources.PageResourceBrowser.class)
@ConditionalOnProperty(
    prefix = "geoserver.web.resource-browser",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Import(WebResourceBrowserConfiguration.class)
@Slf4j
public class WebResourceBrowserAutoConfiguration {

    public WebResourceBrowserAutoConfiguration() {
        log.info("geoserver.web.resource-browser enabled");
    }
}
