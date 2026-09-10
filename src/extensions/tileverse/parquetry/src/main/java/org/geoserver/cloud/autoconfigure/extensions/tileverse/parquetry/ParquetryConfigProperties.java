/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Parquetry plugin extension, bound to
 * {@code geoserver.extension.tileverse.parquetry}.
 *
 * <p>{@code enabled} is the umbrella switch: when off, every feature is off regardless of its own toggle. When on, each
 * feature follows its own toggle. All default to on:
 *
 * <ul>
 *   <li>{@code parquet.enabled}: the "Parquet" datastore
 *   <li>{@code output-formats.geoparquet.enabled}: the {@code geoparquet} WFS GetFeature output format
 *   <li>{@code output-formats.arrow-ipc.enabled}: the {@code arrow-ipc} WFS GetFeature output format
 * </ul>
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     tileverse:
 *       parquetry:
 *         enabled: true
 *         parquet:
 *           enabled: true
 *         output-formats:
 *           geoparquet:
 *             enabled: true
 *           arrow-ipc:
 *             enabled: true
 * }</pre>
 *
 * @since 3.1.0
 */
@ConfigurationProperties(prefix = ParquetryConfigProperties.PREFIX)
public @Data class ParquetryConfigProperties {

    /** Configuration prefix for the Parquetry plugin properties */
    static final String PREFIX = "geoserver.extension.tileverse.parquetry";

    /** Umbrella switch for the whole plugin. */
    private boolean enabled = true;

    /** The "Parquet" datastore and its store-edit panel. */
    private Feature parquet = new Feature();

    /** The WFS GetFeature output formats. */
    private OutputFormats outputFormats = new OutputFormats();

    /** Whether the plugin contributes anything: the umbrella switch is on and at least one feature is on. */
    public boolean anyEnabled() {
        return enabled && (parquet.isEnabled() || outputFormats.anyEnabled());
    }

    /** The WFS GetFeature output formats the plugin provides. */
    public static @Data class OutputFormats {

        /** The {@code geoparquet} output format. */
        private Feature geoparquet = new Feature();

        /** The {@code arrow-ipc} output format. */
        private Feature arrowIpc = new Feature();

        boolean anyEnabled() {
            return geoparquet.isEnabled() || arrowIpc.isEnabled();
        }
    }

    /** One switchable feature of the plugin. */
    public static @Data class Feature {

        /** Whether the feature is on. */
        private boolean enabled = true;
    }
}
