/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.controller.kml;

import org.geoserver.kml.KMLReflector;
import org.geoserver.wms.icons.IconService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller for KML icons at {@literal /kml/icon/**}.
 *
 * <p>Replaces {@code kmlURLMapping} in {@literal gs-kml.jar!/applicationContext.xml}
 *
 * <p>Note the upstream URL mappings for {@literal /kml} and {@literal /kml/**} seem to be not used
 * at all, as all kml service requests are actually handled by the {@link KMLReflector}, so we're
 * not adding those endpoints here.
 *
 * @since 1.0
 */
public @Controller class KMLIconsController {
    // <bean id="kmlURLMapping" class="org.geoserver.ows.OWSHandlerMapping">
    // <constructor-arg ref="catalog" />
    // <property name="alwaysUseFullPath" value="true" />
    // <property name="mappings">
    // <props>
    // <prop key="/kml/icon/**/*">kmlIconService</prop>
    // <prop key="/kml">dispatcher</prop>
    // <prop key="/kml/*">dispatcher</prop>
    // </props>
    // </property>
    // </bean>

    private @Autowired @Qualifier("kmlIconService") IconService kmlIconService;

    @RequestMapping(method = RequestMethod.GET, path = "/kml/icon/**")
    public void getKmlIcon(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        kmlIconService.handleRequest(adaptRequest(request), response);
    }

    /**
     * {@link IconService} requires {@link HttpServletRequest#getPathInfo()} to be non-null, but
     * it's always null in spring-boot
     */
    private HttpServletRequest adaptRequest(HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        final String servletPath = "/kml";
        final int kmlIdx = requestURI.indexOf(servletPath);
        if (kmlIdx > -1) {
            final String pathToKml = requestURI.substring(0, kmlIdx + servletPath.length());
            final String pathInfo = requestURI.substring(pathToKml.length());

            return new HttpServletRequestWrapper(request) {
                public @Override String getServletPath() {
                    return servletPath;
                }

                public @Override String getPathInfo() {
                    return pathInfo;
                }
            };
        }
        return request;
    }
}
