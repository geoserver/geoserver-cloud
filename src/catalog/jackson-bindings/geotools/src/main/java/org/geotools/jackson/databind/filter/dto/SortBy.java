/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link org.geotools.api.filter.sort.SortBy} */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortBy {

    public enum SortOrder {
        ASCENDING,
        DESCENDING
    }

    private Expression.PropertyName propertyName;
    private SortOrder sortOrder;
}
