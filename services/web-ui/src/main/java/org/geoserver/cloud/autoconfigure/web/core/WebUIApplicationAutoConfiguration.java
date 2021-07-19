/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.core;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.core.GeoServerWebMvcMainAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.DemosAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.extension.ExtensionsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.security.SecurityAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.tools.ToolsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wcs.WcsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wfs.WfsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wms.WmsAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.wps.WpsAutoConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInfo.WebUIMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@AutoConfigureAfter({GeoServerWebMvcMainAutoConfiguration.class})
@Import({ //
    WebCoreConfiguration.class, // this one is mandatory
    SecurityAutoConfiguration.class,
    WfsAutoConfiguration.class,
    WmsAutoConfiguration.class,
    WcsAutoConfiguration.class,
    WpsAutoConfiguration.class,
    ExtensionsAutoConfiguration.class,
    DemosAutoConfiguration.class,
    ToolsAutoConfiguration.class
})
@Slf4j
public class WebUIApplicationAutoConfiguration {

    private @Autowired GeoServer geoServer;
    private @Autowired Environment environment;

    public @PostConstruct void setDefaults() {
        GeoServerInfo global = geoServer.getGlobal();
        WebUIMode webUIMode = global.getWebUIMode();
        if (!WebUIMode.DO_NOT_REDIRECT.equals(webUIMode)) {
            log.info("Forcing web-ui mode to DO_NOT_REDIRECT, was {}", webUIMode);
            global.setWebUIMode(WebUIMode.DO_NOT_REDIRECT);
            geoServer.save(global);
        }

        Boolean hidefs =
                environment.getProperty(
                        "geoserver.web-ui.file-browser.hide-file-system", Boolean.class);
        if (Boolean.TRUE.equals(hidefs)) {
            log.info("Setting GEOSERVER_FILEBROWSER_HIDEFS=true System Property");
            System.setProperty("GEOSERVER_FILEBROWSER_HIDEFS", "true");
        }
    }
}
