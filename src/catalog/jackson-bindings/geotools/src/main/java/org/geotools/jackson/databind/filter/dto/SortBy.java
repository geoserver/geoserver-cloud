/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link org.opengis.filter.sort.SortBy} */
@NoArgsConstructor
@AllArgsConstructor
public @Data class SortBy {

    public static enum SortOrder {
        ASCENDING,
        DESCENDING
    }

    private Expression.PropertyName propertyName;
    private SortOrder sortOrder;
}
