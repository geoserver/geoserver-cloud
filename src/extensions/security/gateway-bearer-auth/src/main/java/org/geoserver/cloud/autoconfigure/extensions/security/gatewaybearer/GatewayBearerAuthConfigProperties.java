/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.gatewaybearer;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for optional gateway-side OIDC Bearer authentication. */
@Data
@ConfigurationProperties(prefix = GatewayBearerAuthConfigProperties.PREFIX)
public class GatewayBearerAuthConfigProperties {

    public static final String PREFIX = "geoserver.extension.security.gateway-bearer-auth";
    public static final boolean DEFAULT = false;

    /** Whether Bearer authentication is enabled (default: false). */
    private boolean enabled = DEFAULT;

    /** OIDC issuer URI; must match the token's {@code iss} claim. */
    private String issuerUri;

    /** Required audience identifying this GeoServer Cloud API. */
    private String audience;

    /** Claim used as the GeoServer username. */
    private String usernameClaim = "preferred_username";

    /** Nested claim path containing IdP roles. */
    private String rolesClaim = "resource_access.gs-client.roles";

    /** Explicit mapping from IdP role names to GeoServer role names. Unmapped roles are discarded. */
    private Map<String, String> roleMappings = new LinkedHashMap<>();

    /** Prefix expected by GeoServer Cloud's shared-auth role converter. */
    private String rolePrefix = "ROLE_";
}
