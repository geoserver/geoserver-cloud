/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/**
 * Represents a query for retrieving catalog objects of a specific type from the an {@link ExtendedCatalogFacade}.
 *
 * <p>This class encapsulates the parameters for querying catalog entities (e.g., layers, styles, workspaces).
 *  It supports filtering, sorting, and pagination through a type-safe,
 * immutable design with fluent setters. Queries are typically used by the {@link Catalog} implementation  to fetch subsets
 * of catalog data based on conditions defined by a {@link Filter}, ordered by {@link SortBy} criteria, and
 * limited by offset and count constraints.
 *
 * <p>The class is immutable when constructed, with setters returning the same or a new instance to maintain
 * chainability. It provides factory methods for common use cases (e.g., querying all objects of a type) and
 * constructors for copying or retyping existing queries.
 *
 * @param <T> The specific type of {@link Info} being queried (e.g., {@link org.geoserver.catalog.LayerInfo}).
 * @since 1.0
 * @see Filter
 * @see SortBy
 * @see ExtendedCatalogFacade#query(Query)
 */
@NoArgsConstructor
@RequiredArgsConstructor
@Accessors(chain = true)
public @Data class Query<T extends Info> {

    private @NonNull Class<T> type;
    private @NonNull Filter filter = Filter.INCLUDE;
    private @NonNull List<SortBy> sortBy = new ArrayList<>();
    private Integer offset;
    private Integer count;

    /**
     * Constructs a new {@code Query} by retyping an existing query to a different {@link Info} subclass.
     *
     * <p>This constructor creates a new query instance with the same filter, sorting, offset, and count as the
     * provided query, but with a new target type. It is useful for adapting a generic query to a specific
     * catalog object type while preserving its parameters.
     *
     * @param type  The new type of {@link Info} to query.
     * @param query The existing query to copy parameters from.
     * @throws NullPointerException if {@code type} or {@code query} is null.
     * @example Retyping a query:
     *          <pre>
     *          Query<Info> genericQuery = Query.valueOf(Info.class, someFilter);
     *          Query<LayerInfo> layerQuery = new Query<>(LayerInfo.class, genericQuery);
     *          </pre>
     */
    public Query(@NonNull Class<T> type, @NonNull Query<?> query) {
        this.type = type;
        this.filter = query.getFilter();
        this.sortBy = new ArrayList<>(query.getSortBy());
        this.offset = query.getOffset();
        this.count = query.getCount();
    }

    /**
     * Constructs a new {@code Query} by copying an existing query of the same type.
     *
     * <p>This copy constructor creates a deep copy of the provided query’s properties, ensuring that changes to
     * the new instance (e.g., via setters) do not affect the original. The {@code sortBy} list is defensively
     * copied to maintain independence.
     *
     * @param query The query to copy.
     * @throws NullPointerException if {@code query} is null.
     * @example Copying a query:
     *          <pre>
     *          Query<LayerInfo> original = Query.valueOf(LayerInfo.class, someFilter);
     *          Query<LayerInfo> copy = new Query<>(original);
     *          </pre>
     */
    public Query(@NonNull Query<T> query) {
        this.type = query.getType();
        this.filter = query.getFilter();
        this.sortBy = new ArrayList<>(query.getSortBy());
        this.offset = query.getOffset();
        this.count = query.getCount();
    }

    /**
     * Determines if this query specifies any sorting criteria.
     *
     * <p>This method checks whether the query includes any {@link SortBy} directives, indicating that results
     * should be ordered rather than returned in their natural order.
     *
     * @return {@code true} if sorting is specified (i.e., {@code sortBy} is non-empty); {@code false} otherwise.
     */
    public boolean isSorting() {
        return !sortBy.isEmpty();
    }

    /**
     * Returns the count (limit) of results as an {@link OptionalInt}.
     *
     * <p>The count specifies the maximum number of catalog objects to return. If no count is set, an empty
     * {@link OptionalInt} is returned, indicating no limit.
     *
     * @return An {@link OptionalInt} containing the count if set, or empty if not.
     * @example Checking count:
     *          <pre>
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, Filter.INCLUDE, null, 10);
     *          OptionalInt count = query.count(); // returns OptionalInt.of(10)
     *          </pre>
     */
    public OptionalInt count() {
        return count == null ? OptionalInt.empty() : OptionalInt.of(count.intValue());
    }

    /**
     * Returns the offset (starting index) of results as an {@link OptionalInt}.
     *
     * <p>The offset specifies the number of catalog objects to skip before returning results. If no offset is
     * set, an empty {@link OptionalInt} is returned, indicating the query starts from the beginning.
     *
     * @return An {@link OptionalInt} containing the offset if set, or empty if not.
     * @example Checking offset:
     *          <pre>
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, Filter.INCLUDE, 5, null);
     *          OptionalInt offset = query.offset(); // returns OptionalInt.of(5)
     *          </pre>
     */
    public OptionalInt offset() {
        return offset == null ? OptionalInt.empty() : OptionalInt.of(offset.intValue());
    }

    /**
     * Creates a query that retrieves all objects of a specified type without additional constraints.
     *
     * <p>This factory method constructs a query with the default filter {@link Filter#INCLUDE}, no sorting,
     * and no pagination limits, effectively selecting all available objects of the given type.
     *
     * @param <C>  The type of {@link Info} to query.
     * @param type The class of catalog objects to retrieve.
     * @return A new {@code Query} instance for all objects of the specified type.
     * @throws NullPointerException if {@code type} is null.
     * @example Querying all layers:
     *          <pre>
     *          Query<LayerInfo> allLayers = Query.all(LayerInfo.class);
     *          </pre>
     */
    public static <C extends Info> Query<C> all(Class<? extends Info> type) {
        return valueOf(type, Filter.INCLUDE, null, null);
    }

    /**
     * Creates a query with a specified type and filter, without sorting or pagination.
     *
     * <p>This factory method provides a simple way to query catalog objects with a custom filter, using
     * default values (null) for offset, count, and sorting.
     *
     * @param <T>   The type of {@link CatalogInfo} to query.
     * @param type  The class of catalog objects to retrieve.
     * @param filter The filter to apply to the query (e.g., to match specific properties).
     * @return A new {@code Query} instance with the specified type and filter.
     * @throws NullPointerException if {@code type} or {@code filter} is null.
     * @example Querying layers by name:
     *          <pre>
     *          Filter nameFilter = ...; // e.g., filter by name "roads"
     *          Query<LayerInfo> layerQuery = Query.valueOf(LayerInfo.class, nameFilter);
     *          </pre>
     */
    public static <T extends CatalogInfo> Query<T> valueOf(Class<T> type, Filter filter) {
        return valueOf(type, filter, null, null);
    }

    /**
     * Creates a fully customized query with type, filter, pagination, and sorting options.
     *
     * <p>This factory method constructs a query with all configurable parameters. Null values for filter
     * default to {@link Filter#INCLUDE}, and null sort orders result in an empty sort list. The method
     * ensures non-null {@code SortBy} elements in the list.
     *
     * @param <T>       The type of {@link Info} to query.
     * @param type      The class of catalog objects to retrieve.
     * @param filter    The filter to apply, or null for {@link Filter#INCLUDE}.
     * @param offset    The number of objects to skip, or null for no offset.
     * @param count     The maximum number of objects to return, or null for no limit.
     * @param sortOrder Variable number of {@link SortBy} directives for ordering results (nulls ignored).
     * @return A new {@code Query} instance with the specified parameters.
     * @throws NullPointerException if {@code type} is null.
     * @example Querying sorted and paginated layers:
     *          <pre>
     *          SortBy sortByName = ...; // e.g., sort by "name" ascending
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, Filter.INCLUDE, 10, 5, sortByName);
     *          </pre>
     */
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
     * Returns this query if the provided filter matches the current one, or a new query with the updated filter.
     *
     * <p>This method allows modifying the filter while preserving immutability by returning a new instance
     * unless the filter is unchanged. It leverages the fluent setter pattern for chainability.
     *
     * @param filter The new filter to apply.
     * @return This {@code Query} instance if the filter is unchanged, or a new instance with the new filter.
     * @throws NullPointerException if {@code filter} is null.
     * @example Updating a filter:
     *          <pre>
     *          Query<LayerInfo> query = Query.all(LayerInfo.class);
     *          Filter newFilter = ...; // e.g., filter by workspace
     *          Query<LayerInfo> filteredQuery = query.withFilter(newFilter);
     *          </pre>
     */
    public Query<T> withFilter(Filter filter) {
        return filter.equals(this.filter) ? this : new Query<>(this).setFilter(filter);
    }

    @SuppressWarnings("unchecked")
    public <T extends CatalogInfo> Query<T> as() {
        return (Query<T>) this;
    }
}
