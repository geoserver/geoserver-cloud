/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Base DTO for {@link org.geoserver.catalog.CatalogInfo}
 */
@JsonSubTypes({
    @JsonSubTypes.Type(value = WorkspaceInfoDto.class),
    @JsonSubTypes.Type(value = NamespaceInfoDto.class),
    @JsonSubTypes.Type(value = StyleInfoDto.class),
    @JsonSubTypes.Type(value = MapInfoDto.class),
    @JsonSubTypes.Type(value = StoreInfoDto.class),
    @JsonSubTypes.Type(value = ResourceInfoDto.class),
    @JsonSubTypes.Type(value = PublishedInfoDto.class)
})
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class CatalogInfoDto extends InfoDto {
    private Date dateCreated;
    private Date dateModified;
    /**
     * @since 2.28.0
     */
    private String modifiedBy;
}
