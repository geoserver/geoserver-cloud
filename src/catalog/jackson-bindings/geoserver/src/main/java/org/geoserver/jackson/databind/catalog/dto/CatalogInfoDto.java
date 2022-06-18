/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;

import java.util.Date;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Workspace.class, name = "WorkspaceInfo"),
    @JsonSubTypes.Type(value = Namespace.class, name = "NamespaceInfo"),
    @JsonSubTypes.Type(value = Style.class, name = "StyleInfo"),
    @JsonSubTypes.Type(value = Map.class, name = "MapInfo"),
    @JsonSubTypes.Type(value = Store.class),
    @JsonSubTypes.Type(value = Resource.class),
    @JsonSubTypes.Type(value = Published.class)
})
@Data
@Generated
@EqualsAndHashCode(callSuper = true)
public abstract class CatalogInfoDto extends InfoDto {
    private Date dateCreated;
    private Date dateModified;
}
