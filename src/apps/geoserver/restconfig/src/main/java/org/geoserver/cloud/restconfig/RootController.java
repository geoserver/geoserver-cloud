/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

public @Controller class RootController {

    @GetMapping("/")
    public RedirectView redirectRootToGetCapabilities() {
        return new RedirectView("/rest");
    }
}
