/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Dispatcher;
import org.geowebcache.GeoWebCacheDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/gwc")
public class GeoWebCacheController {

    private @Autowired GeoWebCacheDispatcher gwcDispatcher;
    private @Autowired Dispatcher geoserverDispatcher;

    @RequestMapping(
        method = {GET, POST},
        path = "/**"
    )
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        gwcDispatcher.handleRequest(request, response);
    }

    @RequestMapping(
        method = {GET, POST},
        path = {"/service/**"}
    )
    public void serviceRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
