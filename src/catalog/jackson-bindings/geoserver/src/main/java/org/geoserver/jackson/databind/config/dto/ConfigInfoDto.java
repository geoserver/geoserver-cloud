/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.jackson.databind.catalog.dto.InfoDto;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GeoServer.class),
    @JsonSubTypes.Type(value = Logging.class),
    @JsonSubTypes.Type(value = Service.class),
    @JsonSubTypes.Type(value = Settings.class)
})
public class ConfigInfoDto extends InfoDto {}
