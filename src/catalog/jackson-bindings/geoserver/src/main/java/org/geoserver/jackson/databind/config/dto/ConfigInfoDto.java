/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.catalog.Info;
import org.geoserver.jackson.databind.catalog.dto.InfoDto;

/**
 * Base DTO for configuration (i.e. non-catalog) {@link Info} types
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GeoServerInfoDto.class),
    @JsonSubTypes.Type(value = LoggingInfoDto.class),
    @JsonSubTypes.Type(value = ServiceInfoDto.class),
    @JsonSubTypes.Type(value = SettingsInfoDto.class)
})
public class ConfigInfoDto extends InfoDto {}
