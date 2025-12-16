/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

/**
 * Overriddes {@link GeoServerSecurityFilterChainProxy#destroy()} to be resilient.
 * <p>
 * This class is in package {@code org.geoserver.security} to get access to the package private fields from the superclass
 */
public class CloudGeoServerSecurityFilterChainProxy extends GeoServerSecurityFilterChainProxy {

    public CloudGeoServerSecurityFilterChainProxy(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void destroy() {
        if (super.proxy != null) {
            proxy.destroy();
        }
        // do some cleanup
        if (super.securityManager != null) {
            securityManager.removeListener(this);
        }
    }
}
