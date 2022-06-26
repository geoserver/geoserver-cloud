/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;

import org.geoserver.config.LoggingInfo;

/** DTO for {@link LoggingInfo} */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("LoggingInfo")
public @Data @Generated class Logging extends ConfigInfoDto {
    private String level;
    private String location;
    private boolean stdOutLogging;
}
