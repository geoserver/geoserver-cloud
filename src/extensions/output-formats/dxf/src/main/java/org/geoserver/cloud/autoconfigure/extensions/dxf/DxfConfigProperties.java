/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.dxf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the DXF extension.
 *
 * <p>
 * These properties control whether the DXF extension is enabled.
 *
 * @since 2.27.0
 */
@Data
@ConfigurationProperties(prefix = "geoserver.extension.dxf")
public class DxfConfigProperties {

    /**
     * Whether DXF output format support is enabled.
     * Default is true.
     */
    private boolean enabled = true;

    /**
     * Whether DXF WPS extension is enabled.  geoserver.extension.dxf.enabled must also be true.
     *
     * Default is true.
     */
    private boolean wps = true;
}
