/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Redirects requests to the application root to the GeoWebCache GetCapabilities endpoint.
 *
 * <p>Only registered when the web UI service is not enabled, since the web UI application provides its own root
 * redirect.
 */
@Controller
class RootRedirectController {

    @GetMapping("/")
    public RedirectView redirectRootToGetCapabilities() {
        RedirectView redirect = new RedirectView("/gwc");
        redirect.setContextRelative(true);
        return redirect;
    }
}
