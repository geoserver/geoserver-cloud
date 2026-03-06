/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** DTO for {@link org.geoserver.catalog.MapInfo} */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("MapInfo")
public class MapInfoDto extends CatalogInfoDto {

    private String name;
    private boolean enabled;
    private List<String> layers;
}
