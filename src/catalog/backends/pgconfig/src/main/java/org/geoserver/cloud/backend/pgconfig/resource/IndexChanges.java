/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import org.jspecify.annotations.Nullable;

/**
 * The rows changed between two loads of a {@link ResourceStoreCache}: the rows created, modified and deleted. A row is
 * modified if its path holds another id, type or mtime.
 *
 * <p>The store applies its own writes and the remote events to the index: the loads differ by the changes made outside
 * the store, by another program or through events lost while the event bus was away.
 */
record IndexChanges(List<ResourceRow> created, List<ResourceRow> modified, List<ResourceRow> deleted) {

    /** No changes: a read answered by the loaded index, or the first load. */
    static final IndexChanges NONE = new IndexChanges(List.of(), List.of(), List.of());

    IndexChanges {
        created = List.copyOf(created);
        modified = List.copyOf(modified);
        deleted = List.copyOf(deleted);
    }

    boolean isEmpty() {
        return created.isEmpty() && modified.isEmpty() && deleted.isEmpty();
    }

    /** Compares two loads in one pass: the maps are sorted by path, and their iterators walk them side by side. */
    static IndexChanges between(NavigableMap<String, ResourceRow> before, NavigableMap<String, ResourceRow> after) {
        List<ResourceRow> created = new ArrayList<>();
        List<ResourceRow> modified = new ArrayList<>();
        List<ResourceRow> deleted = new ArrayList<>();
        Iterator<ResourceRow> previousRows = before.values().iterator();
        Iterator<ResourceRow> currentRows = after.values().iterator();
        ResourceRow previous = next(previousRows);
        ResourceRow current = next(currentRows);
        while (previous != null || current != null) {
            int order = comparePaths(previous, current);
            if (order < 0) {
                deleted.add(previous);
                previous = next(previousRows);
            } else if (order > 0) {
                created.add(current);
                current = next(currentRows);
            } else {
                if (differ(previous, current)) {
                    modified.add(current);
                }
                previous = next(previousRows);
                current = next(currentRows);
            }
        }
        return new IndexChanges(created, modified, deleted);
    }

    private static @Nullable ResourceRow next(Iterator<ResourceRow> rows) {
        return rows.hasNext() ? rows.next() : null;
    }

    /** Orders two rows by path. A missing row comes after the others: the rows left in the other map remain. */
    private static int comparePaths(@Nullable ResourceRow previous, @Nullable ResourceRow current) {
        if (previous == null) {
            return 1;
        }
        if (current == null) {
            return -1;
        }
        return previous.path().compareTo(current.path());
    }

    private static boolean differ(ResourceRow previous, ResourceRow current) {
        return previous.id() != current.id()
                || previous.type() != current.type()
                || previous.mtime() != current.mtime();
    }
}
