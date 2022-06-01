/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.ows.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.ows.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class OWSController {

    private @Autowired Dispatcher geoserverDispatcher;

    private @Autowired VirtualServiceVerifier virtualServiceVerifier;

    @RequestMapping(
            method = {GET, POST},
            path = "/ows")
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }

    @RequestMapping(
            method = {GET, POST},
            path = "/{virtualService}/ows")
    public void handleVirtualService(
            @PathVariable(name = "virtualService") String virtualService,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService);

        geoserverDispatcher.handleRequest(request, response);
    }

    @RequestMapping(
            method = {GET, POST},
            path = "/{virtualService}/{layer}/ows")
    public void handleVirtualServiceLayer(
            @PathVariable(name = "virtualService") String virtualService,
            @PathVariable(name = "layer") String layer,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService, layer);
        geoserverDispatcher.handleRequest(request, response);
    }
}
