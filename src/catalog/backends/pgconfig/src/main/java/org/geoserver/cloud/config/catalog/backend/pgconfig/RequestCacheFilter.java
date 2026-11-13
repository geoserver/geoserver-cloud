/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;

/**
 * Opens the cache of a request when the request starts, and closes it when the request ends. The catalog and
 * configuration lookups of the request then decode each object once, see {@link RequestCacheScope}.
 *
 * <p>If the filter also runs for forwards or includes, the nested call finds the scope open: it opens nothing and
 * closes nothing. The filter closes the cache it opened, even if the cache bound to the thread changed during the
 * request.
 */
public class RequestCacheFilter implements Filter {

    private final int maxEntries;

    /** @param maxEntries the maximum number of objects kept by the cache of a request */
    public RequestCacheFilter(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Runnable closeScope = RequestCacheScope.open(maxEntries);
        try {
            chain.doFilter(request, response);
        } finally {
            closeScope.run();
        }
    }
}
