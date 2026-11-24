/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.restconfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.geoserver.cloud.gwc.config.core.GwcRequestPathInfoFilter;
import org.geoserver.configuration.core.rest.RestRequestPathInfoFilter;
import org.geoserver.rest.SuffixStripFilter;
import org.springframework.context.ApplicationContext;

/**
 * Replaces the per-application {@code NpeAwareSuffixStripFilter} definitions from the former restconfig and gwc
 * applications, which conflicted on bean name when both services run in the same application context. Both path
 * adapters ignore requests that do not target their API.
 */
class UnifiedSuffixStripFilter extends SuffixStripFilter {

    public UnifiedSuffixStripFilter(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest adapted = RestRequestPathInfoFilter.adaptRequest(request);
        adapted = GwcRequestPathInfoFilter.adaptRequest(adapted);
        super.doFilterInternal(adapted, response, filterChain);
    }
}
