/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.geoserver.config.JAIInfo;

/** DTO for {@link JAIInfo} */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class JaiDto {
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
