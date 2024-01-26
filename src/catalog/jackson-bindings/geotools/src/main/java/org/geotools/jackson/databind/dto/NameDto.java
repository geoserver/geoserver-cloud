/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.dto;

import lombok.Data;

/** DTO for {@link org.geotools.api.feature.type.Name} */
@Data
public class NameDto {
    private String namespaceURI;
    private String localPart;
}
