/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;

import java.util.List;

@Data
@Generated
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("MapInfo")
public class Map extends CatalogInfoDto {

    private String name;
    private boolean enabled;
    private List<InfoReference> layers;
}
