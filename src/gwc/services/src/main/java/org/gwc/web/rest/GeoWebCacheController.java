/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.web.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.gwc.dispatch.GeoServerGWCDispatcherController;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.controller.GeoWebCacheDispatcherController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Modified top-level dispatcher controller for use by GeoServer. Same as {@link
 * GeoWebCacheDispatcherController}, except the "/service/**" endpoint is excluded. This is handled
 * separately by the GeoServer Dispatcher.
 *
 * <p>Copied from {@link GeoServerGWCDispatcherController}
 */
@Controller
@RequestMapping("/gwc")
@RequiredArgsConstructor
public class GeoWebCacheController {

    private final @NonNull GeoWebCacheDispatcher gwcDispatcher;

    @GetMapping(
            path = {
                "",
                "/home",
                "/demo/**",
                "/proxy/**",
            })
    public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        gwcDispatcher.handleRequest(request, response);
    }
}
