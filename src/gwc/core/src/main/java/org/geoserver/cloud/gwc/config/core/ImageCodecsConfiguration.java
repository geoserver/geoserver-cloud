/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the GeoWebCache image codec beans, which upstream declares in {@literal
 * geowebcache-wmsservice-context.xml} but are needed regardless of whether the GWC WMS service is
 * enabled: the {@code coalescedRequestSplitter} the {@code gwcFacade} is built with decodes the
 * cached member tiles and encodes the assembled multi-layer tile.
 *
 * <p>The codecs come as a set: {@code ImageDecoderContainer} and {@code ImageEncoderContainer}
 * collect the per-format codec beans from the application context and fail to initialize if none is
 * found.
 *
 * <p>{@code WebMapServiceConfiguration} imports the rest of {@literal
 * geowebcache-wmsservice-context.xml} and excludes these beans, keeping a single definition site.
 *
 * @since 2.28.5
 */
@Configuration(proxyBeanMethods = false)
@ImportFilteredResource(ImageCodecsConfiguration.CODEC_BEANS_INCLUDES)
public class ImageCodecsConfiguration {

    /**
     * Bean name pattern matching the per-format {@code ImageEncoder} and {@code ImageDecoder} beans
     * plus the two containers collecting them, and nothing else in {@literal
     * geowebcache-wmsservice-context.xml}.
     */
    public static final String CODEC_BEAN_NAMES = "(.*Encoder|.*Decoder|encoderContainer|decoderContainer)";

    static final String CODEC_BEANS_INCLUDES =
            "jar:gs-gwc-[0-9]+.*!/geowebcache-wmsservice-context.xml#name=^" + CODEC_BEAN_NAMES + "$";
}
