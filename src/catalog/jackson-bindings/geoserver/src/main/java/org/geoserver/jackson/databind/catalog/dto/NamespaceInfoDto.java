/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * DTO for {@link org.geoserver.catalog.NamespaceInfo}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("NamespaceInfo")
public class NamespaceInfoDto extends CatalogInfoDto {
    private String name;

    @JsonProperty("uri")
    @SuppressWarnings("java:S116")
    private String URI;

    private boolean isolated;
    private MetadataMapDto metadata;
}
