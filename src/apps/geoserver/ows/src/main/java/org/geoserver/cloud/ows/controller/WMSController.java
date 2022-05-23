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
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public @Controller class WMSController {

    private @Autowired Dispatcher geoserverDispatcher;

    private @Autowired org.geoserver.ows.ClasspathPublisher classPathPublisher;

    private @Autowired VirtualServiceVerifier virtualServiceVerifier;

    /**
     * Serve only WMS schemas from classpath.
     *
     * <p>E.g.:
     *
     * <ul>
     *   <li>{@code /schemas/wms/1.3.0/capabilities_1_3_0.xsd}
     *   <li>{@code /schemas/wms/1.1.1/WMS_MS_Capabilities.dtd}
     * </ul>
     */
    @RequestMapping(
            method = RequestMethod.GET,
            path = {"/schemas/wms/**"})
    public void getWmsSchema(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    /**
     * Serve openlayers resources from classpath.
     *
     * <p>I.e.:
     *
     * <ul>
     *   <li>{@code /openlayers/**}
     *   <li>{@code /openlayers3/**}
     * </ul>
     */
    @RequestMapping(
            method = RequestMethod.GET,
            path = {"/openlayers/**", "/openlayers3/**"})
    public void getStaticResource(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    @RequestMapping(
            method = {GET, POST},
            path = "/wms")
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }

    @RequestMapping(
            method = {GET, POST},
            path = "/{virtualService}/wms")
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
            path = "/{virtualService}/{layer}/wms")
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
