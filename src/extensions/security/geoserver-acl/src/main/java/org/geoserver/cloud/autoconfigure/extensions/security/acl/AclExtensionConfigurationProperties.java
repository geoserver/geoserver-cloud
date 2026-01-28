/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.acl;

import lombok.Data;
import org.geoserver.acl.plugin.config.webapi.ApiClientConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the GeoServer ACL extension.
 * <p>
 * This only enables or disables the extension. The {@link ApiClientConfiguration API Client} configuration properties are used to configure access to the target ACL service.
 *
 * @since 2.27.0.0
 */
@Data
@ConfigurationProperties(prefix = AclExtensionConfigurationProperties.PREFIX)
public class AclExtensionConfigurationProperties {

    /** Configuration prefix for GeoServer ACL properties */
    public static final String PREFIX = "geoserver.extension.security.acl";

    /** Whether the GeoServer ACL extension is enabled (default: false) */
    public static final boolean DEFAULT = false;

    /** Enable/disable GeoServer ACL integration (default: false) */
    private boolean enabled = DEFAULT;
}
