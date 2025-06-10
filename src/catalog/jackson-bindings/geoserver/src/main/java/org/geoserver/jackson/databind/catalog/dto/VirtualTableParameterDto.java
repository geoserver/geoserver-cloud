/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.geotools.jdbc.VirtualTableParameter;

/** DTO type for {@link VirtualTableParameter} */
@Data
public class VirtualTableParameterDto {

    private String name;
    private String defaultValue;

    @JsonProperty("regexpValidator")
    private String validator; // Store as string for UI display
}
