/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link org.geotools.api.filter.sort.SortBy} */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("SortBy")
public class SortByDto {

    /**
     * DTO for {@link org.geotools.api.filter.sort.SortOrder}
     */
    @JsonTypeName("SortOrder")
    public enum SortOrderDto {
        ASCENDING,
        DESCENDING
    }

    private ExpressionDto.PropertyNameDto propertyName;
    private SortOrderDto sortOrder;
}
