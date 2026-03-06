/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.catalog.StoreInfo;

/** Base DTO for {@link StoreInfo} */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CoverageStoreInfoDto.class),
    @JsonSubTypes.Type(value = DataStoreInfoDto.class),
    @JsonSubTypes.Type(value = HTTPStoreInfoDto.class)
})
public abstract class StoreInfoDto extends CatalogInfoDto {
    private String name;
    private String workspace;
    private String description;
    private String type;
    private boolean enabled;
    private org.geoserver.jackson.databind.catalog.ConnectionParameters connectionParameters;
    private MetadataMapDto metadata;

    /** @since geoserver 2.22.0 */
    private boolean disableOnConnFailure;
}
