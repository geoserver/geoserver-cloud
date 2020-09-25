/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.plugin.Query;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/** DTO for {@link Query} */
@NoArgsConstructor
@RequiredArgsConstructor
public @Data class QueryDto {
    private @NonNull Class<?> type;
    private @NonNull Filter filter = Filter.INCLUDE;
    private @NonNull List<SortBy> sortBy = new ArrayList<>();
    private Integer offset;
    private Integer count;
}
