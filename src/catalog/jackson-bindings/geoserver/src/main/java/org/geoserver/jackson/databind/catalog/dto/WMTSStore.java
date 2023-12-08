/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName("WMTSStoreInfo")
@EqualsAndHashCode(callSuper = true)
public class WMTSStore extends HTTPStore {
    private String headerName;
    private String headerValue;
}
