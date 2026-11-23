/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Serves the {@code /accessDenied.html} page to which GeoServer's exception translation filters forward a 403.
 *
 * <p>GeoServer resolves the forward through its dispatcher, as for any other path. That resolution fails in the
 * services for some paths of the denied request. The page has its own servlet instead, mapped to its path, and the
 * servlet container hands the forward to that servlet by its path. The answer does not depend on the request wrappers
 * of the services, and the 403 status set by the filter stays untouched.
 *
 * <p>The page lives in {@code META-INF/resources} of this module, one copy for every service, where the servlet
 * container also finds it. That is what lets the filters register it as their error page at startup instead of logging
 * that it cannot be found on every filter chain build.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class AccessDeniedPageAutoConfiguration {

    static final String ACCESS_DENIED_PAGE = "/accessDenied.html";

    @Bean
    ServletRegistrationBean<AccessDeniedPageServlet> accessDeniedPageServlet() {
        return new ServletRegistrationBean<>(new AccessDeniedPageServlet(), ACCESS_DENIED_PAGE);
    }
}
