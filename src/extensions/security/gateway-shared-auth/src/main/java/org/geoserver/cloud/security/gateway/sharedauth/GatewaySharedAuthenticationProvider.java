/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import com.thoughtworks.xstream.XStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;

/**
 * Authentication provider for the Gateway Shared Authentication filter in GeoServer Cloud.
 *
 * <p>This provider creates instances of {@link GatewaySharedAuthenticationFilter} based on the
 * configured {@link Mode}. It's responsible for properly registering the filter with GeoServer's
 * security subsystem and configuring XStream serialization for the filter's configuration classes.
 *
 * <p><strong>IMPORTANT:</strong> The package name must not be changed as it's used in XStream
 * serialization of security configuration.
 *
 * @see GatewaySharedAuthenticationFilter
 * @since 1.9
 */
@RequiredArgsConstructor
public class GatewaySharedAuthenticationProvider extends AbstractFilterProvider {

    /**
     * Used to indicate whether the auth provider shall act in server or client mode
     *
     * @see GatewaySharedAuthenticationFilter#server()
     * @see GatewaySharedAuthenticationFilter#client()
     */
    public enum Mode {
        SERVER,
        CLIENT,
        DISABLED
    }

    private final @NonNull Mode mode;

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        XStream xStream = xp.getXStream();
        xStream.allowTypes(new Class[] {GatewaySharedAuthenticationFilter.Config.class});
        xStream.alias("gatewaySharedAuthentication", GatewaySharedAuthenticationProvider.class);
        xStream.alias("gatewaySharedAuthenticationFilter", GatewaySharedAuthenticationFilter.Config.class);
    }

    @Override
    public Class<GatewaySharedAuthenticationFilter> getFilterClass() {
        return GatewaySharedAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return switch (mode) {
            case SERVER -> GatewaySharedAuthenticationFilter.server();
            case CLIENT -> GatewaySharedAuthenticationFilter.client();
            case DISABLED -> GatewaySharedAuthenticationFilter.disabled();
            default -> throw new IllegalArgumentException("Unexpected value: " + mode);
        };
    }
}
