/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

/** DTO for {@link org.geoserver.cog.CogSettings} */
@JsonTypeName("CogSettings")
@Data
public class CogSettingsDto {

    /** DTO for {@link org.geoserver.cog.CogSettings.RangeReaderType} */
    @JsonTypeName("RangeReaderType")
    public enum RangeReaderTypeDto {
        HTTP,
        S3,
        GS,
        Azure
    }

    private RangeReaderTypeDto rangeReaderSettings = RangeReaderTypeDto.HTTP;
    private boolean useCachingStream;
}
