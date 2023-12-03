/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.web.tms;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.ows.Dispatcher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping(
        path = {
            "/gwc/service/tms",
            "/{virtualservice}/gwc/service/tms",
            "/{virtualservice}/{layer}/gwc/service/tms"
        })
@RequiredArgsConstructor
public class TMSController {

    private final @NonNull Dispatcher geoserverDispatcher;

    @GetMapping(path = "/**")
    public void serviceRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
