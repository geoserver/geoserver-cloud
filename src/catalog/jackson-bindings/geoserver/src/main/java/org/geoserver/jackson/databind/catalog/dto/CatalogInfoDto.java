/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@JsonSubTypes({
    @JsonSubTypes.Type(value = Workspace.class),
    @JsonSubTypes.Type(value = Namespace.class),
    @JsonSubTypes.Type(value = Style.class),
    @JsonSubTypes.Type(value = Map.class),
    @JsonSubTypes.Type(value = Store.class),
    @JsonSubTypes.Type(value = Resource.class),
    @JsonSubTypes.Type(value = Published.class)
})
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class CatalogInfoDto extends InfoDto {
    private Date dateCreated;
    private Date dateModified;
}
