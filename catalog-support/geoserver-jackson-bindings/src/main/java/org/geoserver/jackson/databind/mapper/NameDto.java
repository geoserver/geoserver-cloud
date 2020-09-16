/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.mapper;

import lombok.Data;

/** DTO for {@link org.opengis.feature.type.Name} */
public @Data class NameDto {
    private String namespaceURI;
    private String localPart;
}
