/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import lombok.Generated;

import org.geotools.jdbc.VirtualTable;

/** DTO type for {@link VirtualTable} */
public @Data @Generated class VirtualTableDto {

    private String name;
    private String sql;
    private boolean escapeSql;
}
