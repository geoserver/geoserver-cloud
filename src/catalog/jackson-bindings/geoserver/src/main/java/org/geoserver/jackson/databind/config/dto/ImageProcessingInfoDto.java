/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.geoserver.config.ImageProcessingInfo;

/** DTO for {@link ImageProcessingInfo} ({@code JAIInfo} before 2.28.0) */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ImageProcessingInfoDto {
    public enum PngEncoderType {
        JDK,
        PNGJ
    }

    private boolean allowInterpolation;
    private boolean recycling;
    private int tilePriority;
    private int tileThreads;
    private double memoryCapacity;
    private double memoryThreshold;
    private PngEncoderType pngEncoderType;
}
