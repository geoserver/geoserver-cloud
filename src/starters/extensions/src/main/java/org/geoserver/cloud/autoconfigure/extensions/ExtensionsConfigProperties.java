/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for extensions.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     appschema:
 *       enabled: true
 * }</pre>
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "geoserver")
public @Data class ExtensionsConfigProperties {

    private Extension extension = new Extension();

    public static @Data @NoArgsConstructor @AllArgsConstructor class EnabledProperty {
        private boolean enabled;
    }

    public static @Data class Extension {
        private EnabledProperty appschema = new EnabledProperty();
    }
}
