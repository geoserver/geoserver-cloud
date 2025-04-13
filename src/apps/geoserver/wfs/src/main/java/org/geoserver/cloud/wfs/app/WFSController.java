/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wfs.app;

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

@Controller
@RequiredArgsConstructor
public class WFSController {

    private final @NonNull Dispatcher geoserverDispatcher;

    private final @NonNull org.geoserver.ows.ClasspathPublisher classPathPublisher;

    private final @NonNull VirtualServiceVerifier virtualServiceVerifier;

    @GetMapping("/")
    public RedirectView redirectRootToGetCapabilities() {
        return new RedirectView("/wfs?SERVICE=WFS&REQUEST=GetCapabilities");
    }

    /** Serve only WFS schemas from classpath (e.g. {@code /schemas/wfs/2.0/wfs.xsd}) */
    @GetMapping(path = "/schemas/wfs/**")
    public void getSchema(HttpServletRequest request, HttpServletResponse response) throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    /**
     * Serve satic resources from classpath.
     *
     * <p>I.e.:
     *
     * <ul>
     *   <li>{@code /webresources/wfs/**}
     * </ul>
     */
    @GetMapping(path = {"/webresources/wfs/**"})
    public void getStaticResource(HttpServletRequest request, HttpServletResponse response) throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    @GetMapping(path = {"/wfs", "/ows"})
    public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        dispatch(request, response);
    }

    @PostMapping(path = {"/wfs", "/ows"})
    public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        dispatch(request, response);
    }

    @GetMapping(path = {"/{virtualService}/wfs", "/{virtualService}/ows"})
    public void handleVirtualServiceGet(
            @PathVariable(name = "virtualService") String virtualService,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService);
        dispatch(request, response);
    }

    @PostMapping(path = {"/{virtualService}/wfs", "/{virtualService}/ows"})
    public void handleVirtualServicePost(
            @PathVariable(name = "virtualService") String virtualService,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService);
        dispatch(request, response);
    }

    @GetMapping(path = {"/{virtualService}/{layer}/wfs", "/{virtualService}/{layer}/ows"})
    public void handleVirtualServiceLayerGet(
            @PathVariable(name = "virtualService") String virtualService,
            @PathVariable(name = "layer") String layer,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        virtualServiceVerifier.checkVirtualService(virtualService, layer);
        dispatch(request, response);
    }

    @PostMapping(path = {"/{virtualService}/{layer}/wfs", "/{virtualService}/{layer}/ows"})
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
