/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;

@Data
@Generated
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("WorkspaceInfo")
public class Workspace extends CatalogInfoDto {
    private String name;
    private boolean isolated;
    private MetadataMapDto metadata;
}
