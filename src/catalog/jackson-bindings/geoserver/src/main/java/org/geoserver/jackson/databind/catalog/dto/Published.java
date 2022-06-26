/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonSubTypes({
    @JsonSubTypes.Type(value = Layer.class),
    @JsonSubTypes.Type(value = LayerGroup.class)
})
@Data
@Generated
@EqualsAndHashCode(callSuper = true)
public abstract class Published extends CatalogInfoDto {

    public enum PublishedType {
        VECTOR,
        RASTER,
        REMOTE,
        WMS,
        GROUP,
        WMTS
    }

    private String name;
    private String title;
    private String Abstract;
    private boolean enabled;
    private boolean advertised;
    private List<AuthorityURL> authorityURLs;
    private List<LayerIdentifier> identifiers;
    private Attribution attribution;
    private Map<String, Serializable> metadata;
}
