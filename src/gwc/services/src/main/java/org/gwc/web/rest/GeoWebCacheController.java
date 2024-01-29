/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.web.rest;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.gwc.dispatch.GeoServerGWCDispatcherController;
import org.geoserver.ows.Dispatcher;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.controller.GeoWebCacheDispatcherController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
@RequiredArgsConstructor
public class GeoWebCacheController {

    private final @NonNull Dispatcher geoserverDispatcher;

    private final @NonNull GeoWebCacheDispatcher geoWebCacheDispatcher;

    private final @NonNull VirtualServiceVerifier virtualServiceVerifier;

    @GetMapping(path = {"/gwc", "/gwc/home", "/gwc/demo/**", "/gwc/proxy/**"})
    public void handleGet(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        geoWebCacheDispatcher.handleRequest(request, response);
    }

    @GetMapping(
            path = {
                "/{namespace}/gwc",
                "/{namespace}/gwc/home",
                "/{namespace}/gwc/demo/**",
                "/{namespace}/gwc/proxy/**"
            })
    public void handlePrefixedNamespaceGet(
            @PathVariable String namespace,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {
        virtualServiceVerifier.checkVirtualService(namespace);
        geoWebCacheDispatcher.handleRequest(request, response);
    }
}
