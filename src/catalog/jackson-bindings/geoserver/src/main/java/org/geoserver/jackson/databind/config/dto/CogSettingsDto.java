/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;

/** */
@JsonTypeName("CogSettings")
@Data
public class CogSettingsDto {
    public enum RangeReaderType {
        HTTP,
        S3,
        GS,
        Azure;
    }

    private RangeReaderType rangeReaderSettings = RangeReaderType.HTTP;
    private boolean useCachingStream;
}
