/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import lombok.Generated;

public @Data @Generated class Legend {
    private String id;
    private int width;
    private int height;
    private String format;
    private String onlineResource;
}
