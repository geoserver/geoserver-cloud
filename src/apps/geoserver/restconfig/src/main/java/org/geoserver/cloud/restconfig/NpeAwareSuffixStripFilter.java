/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.restconfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.geoserver.rest.SuffixStripFilter;
import org.springframework.context.ApplicationContext;

/**
 * Specialized version of {@link SuffixStripFilter} that ensures the request is properly adapted
 * before processing, preventing potential NullPointerExceptions in the base filter logic when
 * running in the GeoServer Cloud environment.
 */
class NpeAwareSuffixStripFilter extends SuffixStripFilter {

    public NpeAwareSuffixStripFilter(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        request = RestRequestPathInfoFilter.adaptRequest(request);
        super.doFilterInternal(request, response, filterChain);
    }
}
