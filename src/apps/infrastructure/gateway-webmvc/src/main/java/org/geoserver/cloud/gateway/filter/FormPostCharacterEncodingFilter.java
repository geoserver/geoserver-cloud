/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.cloud.gateway.server.mvc.filter.FormFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Declares UTF-8 as the request character encoding of form posts that do not specify one, before Spring Cloud Gateway's
 * {@link FormFilter} parses them.
 *
 * <p>{@link FormFilter} rebuilds the body of every {@code application/x-www-form-urlencoded} POST from the servlet
 * parameter map and encodes the result as UTF-8. Tomcat parses that map with the request character encoding, falling
 * back to ISO-8859-1 when the {@code Content-Type} header has no {@code charset} parameter, which is how browsers
 * submit HTML forms. Decoding as ISO-8859-1 and re-encoding as UTF-8 double-encodes every non-ASCII character on its
 * way to the backend service: a contact title with non-ASCII letters saved from the web UI reached the catalog garbled,
 * while the same value sent by a Wicket Ajax request, which declares its charset, arrived intact.
 *
 * <p>The gateway keeps Spring Boot's {@code CharacterEncodingFilter} disabled because it would declare a charset on
 * every proxied request, altering {@code Content-Type} headers such as {@code application/zip} that backend services
 * match exactly. This filter limits the declaration to the form posts re-encoded by {@link FormFilter}; the forwarded
 * {@code Content-Type} of those requests gains {@code charset=UTF-8}, which describes the rebuilt body accurately.
 *
 * <p>Registered as a bean in {@link org.geoserver.cloud.autoconfigure.gateway.GatewayApplicationAutoconfiguration
 * GatewayApplicationAutoconfiguration}.
 *
 * @see <a href="https://github.com/geoserver/geoserver-cloud/issues/981">Issue #981</a>
 * @since 3.1.0
 */
public class FormPostCharacterEncodingFilter extends OncePerRequestFilter implements Ordered {

    /** Runs right before {@link FormFilter}, which must see the encoding before it reads the parameter map. */
    public static final int ORDER = FormFilter.FORM_FILTER_ORDER - 1;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isFormPostWithoutCharset(request)) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        chain.doFilter(request, response);
    }

    private static boolean isFormPostWithoutCharset(HttpServletRequest request) {
        return isFormPost(request) && request.getCharacterEncoding() == null;
    }

    /** The same criteria used by {@link FormFilter} to decide which requests it rebuilds. */
    private static boolean isFormPost(HttpServletRequest request) {
        String contentType = request.getContentType();
        boolean formUrlEncoded =
                contentType != null && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        return formUrlEncoded && HttpMethod.POST.matches(request.getMethod());
    }
}
