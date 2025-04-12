/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInfo.WebUIMode;
import org.geoserver.config.GeoServerInitializer;
import org.springframework.core.env.Environment;

@Slf4j
@RequiredArgsConstructor
class WebUIInitializer implements GeoServerInitializer {

    private final @NonNull Environment environment;

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        GeoServerInfo global = geoServer.getGlobal();
        WebUIMode webUIMode = global.getWebUIMode();
        if (!WebUIMode.DO_NOT_REDIRECT.equals(webUIMode)) {
            log.info("Forcing web-ui mode to DO_NOT_REDIRECT, was {}", webUIMode);
            global.setWebUIMode(WebUIMode.DO_NOT_REDIRECT);
            geoServer.save(global);
        }

        Boolean hidefs = environment.getProperty("geoserver.web-ui.file-browser.hide-file-system", Boolean.class);
        if (Boolean.TRUE.equals(hidefs)) {
            log.info("Setting GEOSERVER_FILEBROWSER_HIDEFS=true System Property");
            System.setProperty("GEOSERVER_FILEBROWSER_HIDEFS", "true");
        }
    }
}
