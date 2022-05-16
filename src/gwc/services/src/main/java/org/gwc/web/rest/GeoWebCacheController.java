/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.web.rest;

import org.geoserver.gwc.dispatch.GeoServerGWCDispatcherController;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.controller.GeoWebCacheDispatcherController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Modified top-level dispatcher controller for use by GeoServer. Same as {@link
 * GeoWebCacheDispatcherController}, except the "/service/**" endpoint is excluded. This is handled
 * seperately by the GeoServer Dispatcher.
 *
 * <p>Copied from {@link GeoServerGWCDispatcherController}
 */
@Controller
@RequestMapping("/gwc")
public class GeoWebCacheController {

    private @Autowired GeoWebCacheDispatcher gwcDispatcher;

    @RequestMapping(
            path = {
                "",
                "/home",
                "/demo/**",
                "/proxy/**",
            })
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        gwcDispatcher.handleRequest(request, response);
    }
}
