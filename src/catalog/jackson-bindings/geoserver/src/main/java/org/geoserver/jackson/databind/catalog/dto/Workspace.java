/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;

import java.io.Serializable;
import java.util.Map;

@Data
@Generated
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("WorkspaceInfo")
public class Workspace extends CatalogInfoDto {
    private String name;
    private boolean isolated;
    private Map<String, Serializable> metadata;
}
