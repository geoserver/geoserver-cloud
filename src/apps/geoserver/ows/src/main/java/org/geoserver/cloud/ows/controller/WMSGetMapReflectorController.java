/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.ows.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import org.geoserver.ows.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public @Controller class WMSGetMapReflectorController {

    private @Autowired Dispatcher geoserverDispatcher;

    @RequestMapping(
            method = {GET},
            path = {"/wms/reflect", "/{workspace}/wms/reflect"})
    public void getMapReflect(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        geoserverDispatcher.handleRequest(request, response);
    }
}
