/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto;

import lombok.Data;
import lombok.Generated;

/** DTO for {@link org.opengis.feature.type.Name} */
public @Data @Generated class NameDto {
    private String namespaceURI;
    private String localPart;
}
