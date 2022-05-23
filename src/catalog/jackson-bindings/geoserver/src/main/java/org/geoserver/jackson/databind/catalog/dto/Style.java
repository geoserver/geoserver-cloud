/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class Style extends CatalogInfoDto {

    private String name;
    private InfoReference workspace;
    private String format;
    private VersionDto formatVersion;
    private String filename;
    private Legend legend;
    private Map<String, Serializable> metadata;
}
