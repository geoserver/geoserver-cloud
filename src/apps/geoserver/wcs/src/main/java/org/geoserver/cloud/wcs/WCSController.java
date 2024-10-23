/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.wcs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.ows.Dispatcher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

@RequiredArgsConstructor
public @Controller class WCSController {

    private final @NonNull Dispatcher geoserverDispatcher;

    private final @NonNull org.geoserver.ows.ClasspathPublisher classPathPublisher;

    private final @NonNull VirtualServiceVerifier virtualServiceVerifier;

    @GetMapping("/")
    public RedirectView redirectRootToGetCapabilities() {
        return new RedirectView("/wcs?SERVICE=WCS&REQUEST=GetCapabilities");
    }

    /** Serve only WCS schemas from classpath (e.g. {@code /schemas/wcs/1.1.1/wcsAll.xsd}) */
    @GetMapping(path = "/schemas/wcs/**")
    public void getSchema(HttpServletRequest request, HttpServletResponse response) throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    @GetMapping(path = {"/wcs", "/ows"})
    public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        dispatch(request, response);
    }

    @PostMapping(path = {"/wcs", "/ows"})
    public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        dispatch(request, response);
    }

    @GetMapping(path = {"/{virtualService}/wcs", "/{virtualService}/ows"})
    public void handleVirtualService(
            @PathVariable(name = "virtualService") String virtualService,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService);
        dispatch(request, response);
    }

    @PostMapping(path = {"/{virtualService}/wcs", "/{virtualService}/ows"})
    public void handleVirtualServicePost(
            @PathVariable(name = "virtualService") String virtualService,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService);
        dispatch(request, response);
    }

    @GetMapping(path = {"/{virtualService}/{layer}/wcs", "/{virtualService}/{layer}/ows"})
    public void handleVirtualServiceLayer(
            @PathVariable(name = "virtualService") String virtualService,
            @PathVariable(name = "layer") String layer,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService, layer);
        dispatch(request, response);
    }

    @PostMapping(path = {"/{virtualService}/{layer}/wcs", "/{virtualService}/{layer}/ows"})
    public void handleVirtualServiceLayerPost(
            @PathVariable(name = "virtualService") String virtualService,
            @PathVariable(name = "layer") String layer,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService, layer);
        dispatch(request, response);
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
