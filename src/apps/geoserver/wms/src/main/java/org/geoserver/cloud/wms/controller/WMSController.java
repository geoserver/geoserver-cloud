/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.controller;

import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.ows.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public @Controller class WMSController {

    private @Autowired Dispatcher geoserverDispatcher;

    private @Autowired org.geoserver.ows.ClasspathPublisher classPathPublisher;

    private @Autowired VirtualServiceVerifier virtualServiceVerifier;

    @GetMapping("/")
    public RedirectView redirectRootToGetCapabilities() {
        return new RedirectView("/wms?SERVICE=WMS&REQUEST=GetCapabilities");
    }

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
    @GetMapping(path = {"/schemas/wms/**"})
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
    @GetMapping(path = {"/openlayers/**", "/openlayers3/**"})
    public void getStaticResource(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    @GetMapping(path = {"/wms", "/ows"})
    public void handleGet(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        dispatch(request, response);
    }

    @PostMapping(path = {"/wms", "/ows"})
    public void handlePost(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        dispatch(request, response);
    }

    @GetMapping(path = {"/{virtualService}/wms", "/{virtualService}/ows"})
    public void handleVirtualServiceGet(
            @PathVariable(name = "virtualService") String virtualService,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService);
        dispatch(request, response);
    }

    @PostMapping(path = {"/{virtualService}/wms", "/{virtualService}/ows"})
    public void handleVirtualServicePost(
            @PathVariable(name = "virtualService") String virtualService,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService);
        dispatch(request, response);
    }

    @GetMapping(path = {"/{virtualService}/{layer}/wms", "/{virtualService}/{layer}/ows"})
    public void handleVirtualServiceLayerGet(
            @PathVariable(name = "virtualService") String virtualService,
            @PathVariable(name = "layer") String layer,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService, layer);
        dispatch(request, response);
    }

    @PostMapping(path = {"/{virtualService}/{layer}/wms", "/{virtualService}/{layer}/ows"})
    public void handleVirtualServiceLayerPost(
            @PathVariable(name = "virtualService") String virtualService,
            @PathVariable(name = "layer") String layer,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService, layer);
        dispatch(request, response);
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
