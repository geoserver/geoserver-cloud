/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

import org.geotools.jdbc.VirtualTable;

/** DTO type for {@link VirtualTable} */
public @Data class VirtualTableDto {

    private String name;
    private String sql;
    private boolean escapeSql;
}
