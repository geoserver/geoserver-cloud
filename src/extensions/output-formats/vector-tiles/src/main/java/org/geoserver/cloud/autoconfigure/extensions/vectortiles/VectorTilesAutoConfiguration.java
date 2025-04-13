/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.vectortiles;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Vector Tiles WMS output formats extension.
 *
 * <p>
 * This auto-configuration enables the Vector Tiles extension in GeoServer Cloud,
 * allowing various vector tile formats to be used as WMS output formats. It is activated
 * when the following conditions are met:
 * <ul>
 *   <li>The GeoServer WMS module is available</li>
 *   <li>The required VectorTileMapOutputFormat classes are on the classpath</li>
 *   <li>The geoserver.extension.vector-tiles.enabled property is true (the default)</li>
 * </ul>
 *
 * <p>
 * The configuration supports three vector tile formats, each of which can be individually
 * enabled or disabled:
 * <ul>
 *   <li>MapBox - Controlled by geoserver.extension.vector-tiles.mapbox</li>
 *   <li>GeoJSON - Controlled by geoserver.extension.vector-tiles.geojson</li>
 *   <li>TopoJSON - Controlled by geoserver.extension.vector-tiles.topojson</li>
 * </ul>
 *
 * @since 2.27.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnVectorTiles
@EnableConfigurationProperties(VectorTilesConfigProperties.class)
@ImportFilteredResource("jar:gs-vectortiles-.*!/applicationContext.xml#name=(VectorTilesExtension)")
@Import({MapBoxConfiguration.class, GeoJsonConfiguration.class, TopoJsonConfiguration.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.vectortiles")
public class VectorTilesAutoConfiguration {

    /**
     * Constructor that configures the Vector Tiles extension.
     *
     * <p>
     * Sets the enabled state of the Vector Tiles extension based on whether any
     * of the vector tile formats are enabled.
     *
     * @param extensionInfo The ModuleStatusImpl bean for the Vector Tiles extension
     * @param config The configuration properties for the Vector Tiles extension
     */
    @SuppressWarnings("java:S6830")
    public VectorTilesAutoConfiguration(
            @Qualifier("VectorTilesExtension") ModuleStatusImpl extensionInfo, VectorTilesConfigProperties config) {

        extensionInfo.setEnabled(config.anyEnabled());
    }

    /**
     * Logs that the Vector Tiles extension is enabled.
     */
    @PostConstruct
    void log() {
        log.info("Vector Tiles extension enabled");
    }
}
