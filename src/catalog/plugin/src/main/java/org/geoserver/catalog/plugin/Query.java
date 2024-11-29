/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/** */
@NoArgsConstructor
@RequiredArgsConstructor
@Accessors(chain = true)
public @Data class Query<T extends Info> {

    private @NonNull Class<T> type;
    private @NonNull Filter filter = Filter.INCLUDE;
    private @NonNull List<SortBy> sortBy = new ArrayList<>();
    private Integer offset;
    private Integer count;

    /** retype constructor */
    public Query(@NonNull Class<T> type, @NonNull Query<?> query) {
        this.type = type;
        this.filter = query.getFilter();
        this.sortBy = new ArrayList<>(query.getSortBy());
        this.offset = query.getOffset();
        this.count = query.getCount();
    }

    /** Copy constructor */
    public Query(@NonNull Query<T> query) {
        this.type = query.getType();
        this.filter = query.getFilter();
        this.sortBy = new ArrayList<>(query.getSortBy());
        this.offset = query.getOffset();
        this.count = query.getCount();
    }

    public boolean isSorting() {
        return !sortBy.isEmpty();
    }

    public OptionalInt count() {
        return count == null ? OptionalInt.empty() : OptionalInt.of(count.intValue());
    }

    public OptionalInt offset() {
        return offset == null ? OptionalInt.empty() : OptionalInt.of(offset.intValue());
    }

    public static <C extends Info> Query<C> all(Class<? extends Info> type) {
        return valueOf(type, Filter.INCLUDE, null, null);
    }

    public static <T extends CatalogInfo> Query<T> valueOf(Class<T> type, Filter filter) {
        return valueOf(type, filter, null, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Info> Query<T> valueOf(
            Class<? extends Info> type, Filter filter, Integer offset, Integer count, SortBy... sortOrder) {

        List<SortBy> sortBy = sortOrder == null
                ? Collections.emptyList()
                : Stream.of(sortOrder).filter(Objects::nonNull).toList();

        filter = filter == null ? Filter.INCLUDE : filter;
        return new Query<>((Class<T>) type)
                .setFilter(filter)
                .setOffset(offset)
                .setCount(count)
                .setSortBy(sortBy);
    }

    /**
     * @return {@code this} if {@code filter} equals {@link #getFilter()}, a copy of this query with
     *     the provided filter otherwise
     */
    public Query<T> withFilter(Filter filter) {
        return filter.equals(this.filter) ? this : new Query<>(this).setFilter(filter);
    }
}
