/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import lombok.Generated;
import lombok.experimental.Accessors;

/** DTO for {@link org.geotools.util.Version} */
@Accessors(chain = true)
public @Data @Generated class VersionDto {
    private String value;
}
