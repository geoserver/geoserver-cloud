package org.geoserver.cloud.wcs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public @Controller class WPSController {

    private @Autowired Dispatcher geoserverDispatcher;

    @RequestMapping(method = RequestMethod.GET, path = "/wps")
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
