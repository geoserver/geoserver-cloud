/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import java.util.Set;
import lombok.Data;
import org.geoserver.config.JAIInfo;

/** DTO for {@link JAIInfo} */
public @Data class JaiDto {
    public static enum PngEncoderType {
        JDK,
        NATIVE,
        PNGJ
    };

    public static @Data class JAIEXTInfo {
        private Set<String> JAIOperations;
        private Set<String> JAIEXTOperations;
    }

    private boolean allowInterpolation;
    private boolean recycling;
    private int tilePriority;
    private int tileThreads;
    private double memoryCapacity;
    private double memoryThreshold;
    private PngEncoderType pngEncoderType;
    private boolean jpegAcceleration;
    private boolean allowNativeMosaic;
    private boolean allowNativeWarp;
    private JAIEXTInfo JAIEXTInfo;
}
