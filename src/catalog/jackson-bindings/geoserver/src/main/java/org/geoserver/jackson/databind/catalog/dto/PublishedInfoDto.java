/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.geoserver.catalog.PublishedInfo;

/**
 * Base DTO for {@link PublishedInfo}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonSubTypes({@JsonSubTypes.Type(value = LayerInfoDto.class), @JsonSubTypes.Type(value = LayerGroupInfoDto.class)})
public abstract class PublishedInfoDto extends CatalogInfoDto {

    /**
     * DTO for {@link org.geoserver.catalog.PublishedType}
     */
    @JsonTypeName("PublishedType")
    public enum PublishedTypeDto {
        VECTOR,
        RASTER,
        REMOTE,
        WMS,
        GROUP,
        WMTS
    }

    private String name;
    private String title;

    @JsonProperty("abstract")
    @SuppressWarnings("java:S116")
    private String Abstract;

    private boolean enabled;
    private boolean advertised;
    private List<AuthorityURLInfoDto> authorityURLs;
    private List<LayerIdentifierInfoDto> identifiers;
    private AttributionInfoDto attribution;
    private MetadataMapDto metadata;
}
