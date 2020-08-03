/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.wms;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public @Controller class WMSController {

    private @Autowired Dispatcher geoserverDispatcher;

    private @Autowired org.geoserver.ows.ClasspathPublisher classPathPublisher;

    /**
     * Serve only WMS schemas and related resources from classpath.
     *
     * <p>E.g.:
     *
     * <ul>
     *   <li>{@code /schemas/wms/1.3.0/capabilities_1_3_0.xsd}
     *   <li>{@code /schemas/wms/1.1.1/WMS_MS_Capabilities.dtd}
     *   <li>{@code /openlayers/**}
     *   <li>{@code /openlayers3/**}
     * </ul>
     */
    @RequestMapping(
        method = RequestMethod.GET,
        path = {"/schemas/wms/**", "/openlayers/**", "/openlayers3/**"}
    )
    public void getStaticResource(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        classPathPublisher.handleRequest(request, response);
    }

    @RequestMapping(
        method = {GET, POST},
        path = {"/wms", "/{workspace}/wms", "/ows", "/{workspace}/ows"}
    )
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
