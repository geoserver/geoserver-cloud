/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Web Map Service extensions.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   styling:
 *     css.enabled: true
 *     mapbox.enabled: true
 *   wms:
 *     outputFormats:
 *       vectorTiles:
 *         mapbox.enabled: true
 *         geojson.enabled: true
 *         topojson.enabled: true
 * }</pre>
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "geoserver")
public @Data class WmsExtensionsConfigProperties {

    private Styling styling = new Styling();
    private Wms wms = new Wms();

    public static @Data @NoArgsConstructor @AllArgsConstructor class EnabledProperty {
        private boolean enabled;
    }

    public static @Data class Styling {
        private EnabledProperty css = new EnabledProperty();
        private EnabledProperty mapbox = new EnabledProperty();
    }

    public static @Data class Wms {
        private WmsOutputFormatsConfigProperties outputFormats =
                new WmsOutputFormatsConfigProperties();

        public static @Data class WmsOutputFormatsConfigProperties {
            private VectorTilesConfigProperties vectorTiles = new VectorTilesConfigProperties();

            public static @Data class VectorTilesConfigProperties {
                /** Enable or disable MapBox VectorTiles output format */
                private EnabledProperty mapbox = new EnabledProperty(true);
                /** Enable or disable GeoJSON VectorTiles output format */
                private EnabledProperty geojson = new EnabledProperty(true);
                /** Enable or disable TopoJSON VectorTiles output format */
                private EnabledProperty topojson = new EnabledProperty(true);

                public boolean anyEnabled() {
                    return isEnabled(mapbox) || isEnabled(geojson) || isEnabled(topojson);
                }

                private boolean isEnabled(EnabledProperty p) {
                    return p != null && p.isEnabled();
                }
            }
        }
    }
}
