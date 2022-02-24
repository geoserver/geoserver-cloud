/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CoverageStore.class, name = "CoverageStoreInfo"),
    @JsonSubTypes.Type(value = DataStore.class, name = "DataStoreInfo"),
    @JsonSubTypes.Type(value = HTTPStore.class)
})
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Store extends CatalogInfoDto {
    private String name;
    private InfoReference workspace;
    private String description;
    private String type;
    private boolean enabled;
    private Map<String, Serializable> connectionParameters;
    private Map<String, Serializable> metadata;
}
