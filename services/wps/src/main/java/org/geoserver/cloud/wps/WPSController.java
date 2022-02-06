/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.wps;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.RedirectView;

public @Controller class WPSController {

    private @Autowired Dispatcher geoserverDispatcher;

    private @Autowired org.geoserver.ows.ClasspathPublisher classPathPublisher;

    @GetMapping("/")
    public RedirectView redirectRootToGetCapabilities() {
        return new RedirectView("/wps?SERVICE=WPS&REQUEST=GetCapabilities");
    }

    /** Serve only WPS schemas from classpath (e.g. {@code /schemas/wps/1.0.0/wpsAll.xsd}) */
    @RequestMapping(method = RequestMethod.GET, path = "/schemas/wps/**")
    public void getSchema(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    @RequestMapping(
        method = {GET, POST},
        path = {"/wps", "/{workspace}/wps", "/ows", "/{workspace}/ows"}
    )
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
