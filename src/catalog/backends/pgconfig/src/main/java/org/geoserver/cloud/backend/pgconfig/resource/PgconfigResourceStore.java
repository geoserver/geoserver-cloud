/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.cache.CacheAccess;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.LoggingTemplate;
import org.geoserver.cloud.event.resource.ResourceChanged;
import org.geoserver.cloud.event.resource.ResourceChanged.Change;
import org.geoserver.cloud.event.resource.ResourceChanged.Kind;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.AntPathMatcher;

/** @since 1.4 */
@Slf4j
@Transactional(transactionManager = "pgconfigTransactionManager", propagation = SUPPORTS)
public class PgconfigResourceStore implements ResourceStore {

    static final long ROOT_ID = 0L;
    static final long UNDEFINED_ID = -1L;

    private static final Runnable NO_LOCAL_COPY = () -> {};

    private static final PgconfigResourceRowMapper ROW_MAPPER = new PgconfigResourceRowMapper();

    /**
     * The query of the rows without their content, as read by {@link #ROW_MAPPER}. {@code octet_length} reads the size
     * from the header of the stored content, without loading the content.
     */
    private static final String SELECT_ROWS =
            """
            SELECT id, parentid, "type", path, mtime, octet_length(content) AS size FROM resourcestore
            """;

    private final LoggingTemplate template;
    private final DataSource dataSource;
    private final FileSystemResourceStoreCache localCopies;
    private final @Nullable ResourceStoreCache index;

    /** Present whenever the index is: the folder load reads the rows and the sizes of the index. */
    private final @Nullable StylesFolderLoader stylesFolderLoader;

    private @NonNull @Getter @Setter LockProvider lockProvider;

    private final Predicate<String> fileSystemOnlyPathMatcher;

    private final Predicate<String> dbBackedFilePatterns;

    private final TransactionTemplate transactionTemplate;

    private final ResourceChangePublisher changePublisher;

    /** Creates a store reading the database on each lookup. */
    public PgconfigResourceStore(
            @NonNull FileSystemResourceStoreCache localCopies,
            @NonNull JdbcTemplate template,
            @NonNull PgconfigLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher,
            @NonNull Predicate<String> dbBackedFilePatterns,
            @NonNull PlatformTransactionManager transactionManager,
            @NonNull ResourceChangePublisher changePublisher) {
        this(
                localCopies,
                template,
                lockProvider,
                fileSystemOnlyPathMatcher,
                dbBackedFilePatterns,
                transactionManager,
                changePublisher,
                null);
    }

    /**
     * Creates a store reading its rows from an index, and keeping the content read.
     *
     * @param index the rows of the store and their content, loaded by {@link #indexLoader(JdbcTemplate)}; null to read
     *     the database on each lookup
     */
    @SuppressWarnings("java:S107")
    public PgconfigResourceStore(
            @NonNull FileSystemResourceStoreCache localCopies,
            @NonNull JdbcTemplate template,
            @NonNull PgconfigLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher,
            @NonNull Predicate<String> dbBackedFilePatterns,
            @NonNull PlatformTransactionManager transactionManager,
            @NonNull ResourceChangePublisher changePublisher,
            @Nullable ResourceStoreCache index) {
        this(
                localCopies,
                template,
                lockProvider,
                fileSystemOnlyPathMatcher,
                dbBackedFilePatterns,
                transactionManager,
                changePublisher,
                index,
                StylesFolderLoader.MAX_FOLDER_BYTES);
    }

    /**
     * Creates a store with a bound of the content written by a load of a styles folder.
     *
     * @param maxStylesFolderBytes above this size in total, the files of a styles folder are written one by one
     */
    @VisibleForTesting
    @SuppressWarnings("java:S107")
    PgconfigResourceStore(
            @NonNull FileSystemResourceStoreCache localCopies,
            @NonNull JdbcTemplate template,
            @NonNull PgconfigLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher,
            @NonNull Predicate<String> dbBackedFilePatterns,
            @NonNull PlatformTransactionManager transactionManager,
            @NonNull ResourceChangePublisher changePublisher,
            @Nullable ResourceStoreCache index,
            long maxStylesFolderBytes) {
        this.template = new LoggingTemplate(template).setLog(log);
        this.dataSource = Objects.requireNonNull(template.getDataSource(), "the template has no data source");
        this.lockProvider = lockProvider;
        this.localCopies = localCopies;
        this.index = index;
        this.stylesFolderLoader =
                index == null ? null : new StylesFolderLoader(template, index, localCopies, maxStylesFolderBytes);
        final String root = "";
        Predicate<String> notRoot = path -> !root.equals(path);
        this.dbBackedFilePatterns = dbBackedFilePatterns;
        this.fileSystemOnlyPathMatcher = notRoot.and(fileSystemOnlyPathMatcher.and(dbBackedFilePatterns.negate()));
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.changePublisher = changePublisher;
        if (index != null) {
            index.onChangesFound(this::announceFoundChanges);
        }
    }

    /**
     * Tells the listeners of this instance about the rows changed behind the store, found by a load of the index.
     * Neither a write of the store nor an event announced these changes: without the notification, the parsed style of
     * a changed style file would stay stale. The local copy of a deleted file is removed.
     */
    private void announceFoundChanges(IndexChanges changes) {
        changes.created().forEach(row -> notifyFound(row, Kind.CREATED));
        changes.modified().forEach(row -> notifyFound(row, Kind.MODIFIED));
        for (ResourceRow row : changes.deleted()) {
            localCopies.deleted(row.path());
            notifyFound(row, Kind.DELETED);
        }
    }

    private void notifyFound(ResourceRow row, Kind kind) {
        ResourceChanged change = ResourceChanged.createLocal(row.path(), kind, row.type(), row.mtime(), List.of());
        changePublisher.notifyLocally(change);
    }

    /** Returns the query of the rows of the store without their content, the loader of a {@link ResourceStoreCache}. */
    public static Supplier<List<ResourceRow>> indexLoader(@NonNull JdbcTemplate template) {
        LoggingTemplate loggingTemplate = new LoggingTemplate(template).setLog(log);
        return () -> queryAllRows(loggingTemplate);
    }

    private static List<ResourceRow> queryAllRows(LoggingTemplate template) {
        try (Stream<ResourceRow> rows = template.queryForStream(SELECT_ROWS, ROW_MAPPER)) {
            return rows.toList();
        }
    }

    public Predicate<String> dbBackedFilePatterns() {
        return dbBackedFilePatterns;
    }

    /**
     * Returns a filter that matches the directories defined in the {@link #defaultIgnoredDirs()} filter, plus the
     * following resources:
     *
     * <ul>
     *   <li>{@literal security/role/default/roles.xml.lock}: {@code org.geoserver.security.xml.XMLRoleStore}'s lock
     *       file, uses a shutdown hook through an {@code org.geoserver.security.file.LockFile} which doesn't follow
     *       standard locking mechanisms and causes exceptions since the jdbc datasource is alredy closed
     *   <li>{@literal security/usergroup/default/users.xml.lock}:
     *       {@code org.geoserver.security.xml.XMLUserGroupStore}'s lock file, uses a shutdown hook through an
     *       {@code org.geoserver.security.file.LockFile} which doesn't follow standard locking mechanisms and causes
     *       exceptions since the jdbc datasource is alredy closed
     * </ul>
     *
     * @return
     */
    public static Predicate<String> defaultIgnoredResources() {
        final Set<String> ignoredResources =
                Set.of("security/role/default/roles.xml.lock", "security/usergroup/default/users.xml.lock");
        return defaultIgnoredDirs().or(ignoredResources::contains);
    }

    public static Predicate<String> defaultIgnoredDirs() {
        return PgconfigResourceStore.simplePathMatcher("temp", "tmp", "legendsamples", "data", "logs");
    }

    public static Predicate<String> simplePathMatcher(String... paths) {
        Predicate<String> matcher = _ -> false;
        for (String path : paths) {
            path = normalize(path);
            matcher = matcher.or(path::equals);
            @SuppressWarnings("java:S1075")
            final String dirpath = path + "/";
            matcher = matcher.or(r -> r.startsWith(dirpath));
        }
        return matcher;
    }

    /** Predicate matching resource paths against Ant-style patterns (e.g. {@code data/**&#47;*.properties}). */
    public static Predicate<String> antPathMatcher(@NonNull List<String> patterns) {
        final List<String> defensiveCopy = List.copyOf(patterns);
        final AntPathMatcher matcher = new AntPathMatcher();
        return path -> defensiveCopy.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }

    @Override
    public Resource get(@NonNull String path) {
        final String validPath = normalize(path);
        if (fileSystemOnlyPathMatcher.test(validPath)) {
            Resource fsResource = localCopies.getLocalOnlyStore().get(validPath);
            return new FileSystemResourceAdaptor(fsResource, this);
        }
        return findByPathOrUndefined(validPath);
    }

    @RequiredArgsConstructor
    static class FileSystemResourceAdaptor implements Resource {

        private interface Overrides {
            Resource parent();

            Resource get(String resourcePath);

            List<Resource> list();

            File dir();
        }

        @Delegate(excludes = Overrides.class)
        @NonNull
        private final Resource delegate;

        private final @NonNull PgconfigResourceStore store;

        @Override
        public Resource parent() {
            String parentPath = Paths.parent(this.path());
            return store.get(parentPath);
        }

        @Override
        public Resource get(String resourcePath) {
            if ("".equals(resourcePath)) {
                return this;
            }
            return store.get(Paths.path(path(), resourcePath));
        }

        @Override
        public List<Resource> list() {
            store.dumpDbBackedChildren(path());
            return delegate.list();
        }

        @Override
        public File dir() {
            store.dumpDbBackedChildren(path());
            return delegate.dir();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FileSystemResourceAdaptor fra
                    && Objects.equals(path(), fra.path())
                    && Objects.equals(getType(), fra.getType());
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    @Override
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public boolean remove(@NonNull String path) {

        String validPath = normalize(path);
        if (fileSystemOnlyPathMatcher.test(validPath)) {
            boolean removedLocally = localCopies.getLocalOnlyStore().remove(validPath);
            boolean removedDbRows = removeDbBackedSubtree(validPath);
            return removedLocally || removedDbRows;
        }
        return findByPath(validPath).map(PgconfigResource::delete).orElse(false);
    }

    /**
     * Deletes the database rows living under a local-only path. A local-only directory may be hybrid: a mosaic store
     * directory under {@code data/} whose whitelisted config files live as database rows. Rows left behind would
     * re-materialize the deleted store's configuration into every pod's cache when a store is later created at the same
     * path. Deleting the directory row cascades to its children.
     */
    private boolean removeDbBackedSubtree(String validPath) {
        return findByPath(validPath).map(this::delete).orElse(false);
    }

    @Override
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public boolean move(@NonNull String path, @NonNull String target) {
        Resource from = get(path);
        Resource to = get(target);
        if (from instanceof PgconfigResource pgFrom && to instanceof PgconfigResource pgTo) {
            return moveDbResource(pgFrom, pgTo);
        }
        if (from instanceof PgconfigResource) {
            throw new UnsupportedOperationException(
                    "source resource targets database but target resource matches the ignored resources predicate. Source: %s, target: %s"
                            .formatted(path, target));
        }
        if (to instanceof PgconfigResource) {
            throw new UnsupportedOperationException(
                    "target resource targets database but source resource matches the ignored resources predicate. Source: %s, target: %s"
                            .formatted(path, target));
        }
        return moveLocalOnly(path, target);
    }

    /**
     * Moves a local-only path, relocating any database rows living under it. A local-only directory may be hybrid: a
     * mosaic store directory under {@code data/} whose whitelisted config files live as database rows. Moving only the
     * local files would leave the configuration rows orphaned at the old path and absent at the new one. The database
     * move relocates the local files too, through {@link FileSystemResourceStoreCache#moved}.
     */
    private boolean moveLocalOnly(String path, String target) {
        Optional<PgconfigResource> dbBacked = findByPath(normalize(path));
        if (dbBacked.isPresent()) {
            PgconfigResource pgTarget = findByPathOrUndefined(normalize(target));
            return moveDbResource(dbBacked.get(), pgTarget);
        }
        return localCopies.getLocalOnlyStore().move(path, target);
    }

    private static String normalize(String path) {
        path = Paths.valid(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    Optional<PgconfigResource> findByPath(@NonNull String path) {
        path = Paths.valid(path);
        Preconditions.checkArgument(!path.startsWith("/"), "Absolute paths not supported: %s", path);
        Optional<ResourceRow> row;
        if (useIndex()) {
            row = index.find(path);
        } else {
            row = queryByPath(path);
        }
        return row.map(this::toResource);
    }

    /**
     * Whether reads go through the index: there is one, and no transaction runs on the data source. A read within a
     * transaction must see the rows written by it, and the mtimes set by the triggers.
     */
    private boolean useIndex() {
        return index != null && !CacheAccess.inTransaction(dataSource);
    }

    private Optional<ResourceRow> queryByPath(String path) {
        String sql = SELECT_ROWS + " WHERE path = ?";
        try {
            return Optional.of(template.queryForObject(sql, ROW_MAPPER, path));
        } catch (EmptyResultDataAccessException _) {
            return Optional.empty();
        }
    }

    // A write re-reads the rows changed by the triggers in one query
    private Map<String, ResourceRow> queryByPaths(String... paths) {
        String sql = SELECT_ROWS + " WHERE path = ANY(?)";
        try (Stream<ResourceRow> rows = template.queryForStream(sql, ROW_MAPPER, (Object) paths)) {
            return rows.collect(Collectors.toMap(ResourceRow::path, Function.identity()));
        }
    }

    /**
     * Reads the rows at the paths again and applies them to the index, after a change made through another instance. A
     * row found replaces the indexed one. A path without a row is removed from the index, with its subtree. The content
     * kept for these rows is evicted. Without an index, there is nothing to refresh.
     *
     * <p>If the refresh fails, it clears the index, loaded again on next use, and rethrows the exception.
     */
    void refresh(@NonNull Collection<String> paths) {
        ResourceStoreCache refreshed = index;
        if (refreshed == null) {
            return;
        }
        try {
            Map<String, ResourceRow> rows = queryByPaths(paths.toArray(String[]::new));
            paths.forEach(path -> refreshPath(refreshed, path, rows.get(path)));
        } catch (RuntimeException e) {
            refreshed.clear();
            throw e;
        }
    }

    // Removing a path evicts the content of its row and of its subtree
    private static void refreshPath(ResourceStoreCache refreshed, String path, @Nullable ResourceRow row) {
        if (row == null) {
            refreshed.remove(path);
        } else {
            refreshed.evictContent(row.id());
            refreshed.put(row);
        }
    }

    private PgconfigResource toResource(ResourceRow row) {
        return new PgconfigResource(this, row.id(), row.parentId(), row.type(), row.path(), row.mtime());
    }

    private List<PgconfigResource> toResources(List<ResourceRow> rows) {
        return rows.stream().map(this::toResource).toList();
    }

    private PgconfigResource undefined(String path) {
        return PgconfigResource.undefined(this, path);
    }

    /**
     * Looks up the database row for a path directly, without routing through the local-only/db-backed predicate.
     *
     * <p>A whitelisted db-backed file may live under a directory that otherwise matches the local-only routing
     * predicate (e.g. {@code data/ws/store} for {@code data/ws/store/indexer.properties}). The database still keeps its
     * own directory hierarchy for such a file's ancestors; parent resolution and directory bookkeeping for it must
     * therefore bypass that routing rather than go through {@link #get(String)}.
     */
    private PgconfigResource findByPathOrUndefined(String path) {
        return findByPath(path).orElseGet(() -> undefined(path));
    }

    private PgconfigResource resourceAt(Map<String, ResourceRow> rows, String path) {
        ResourceRow row = rows.get(path);
        return row == null ? undefined(path) : toResource(row);
    }

    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public PgconfigResource save(@NonNull PgconfigResource resource) {
        return save(resource, null);
    }

    /**
     * Creates the resource if it doesn't exist, updates it if it does.
     *
     * <p>Uses PostgreSQL {@code UPSERT (INSERT ... ON CONFLICT ... DO UPDATE)} for atomic operation.
     *
     * @param resource the resource to save
     * @param contents the content bytes for RESOURCE types, null for DIRECTORY or to keep existing content
     * @return the updated resource with current database state
     * @throws IllegalArgumentException if resource type is UNDEFINED
     */
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public PgconfigResource save(@NonNull PgconfigResource resource, byte[] contents) {
        return inTransaction(() -> saveRow(resource, contents, NO_LOCAL_COPY));
    }

    /**
     * Runs a write in the transaction of the caller, or in a new one. The {@code @Transactional} annotations do not
     * apply here: the resources and the store itself call the store without its transactional proxy.
     */
    private <T> T inTransaction(Supplier<T> write) {
        return transactionTemplate.execute(_ -> write.get());
    }

    /**
     * Saves the row of a resource. Once the save commits, it updates the local copy and announces the change: a rolled
     * back write leaves no local copy behind.
     */
    private PgconfigResource saveRow(PgconfigResource resource, byte[] contents, Runnable localCopyUpdate) {
        // Check type field directly without triggering updateState() which would reset it
        if (resource.type == Type.UNDEFINED) {
            throw new IllegalArgumentException(
                    "Attempting to save a resource of undefined type: %s".formatted(resource));
        } else if (resource.type == Type.DIRECTORY && contents != null) {
            throw new IllegalArgumentException(
                    "Attempting to save a directory resource with contents: %s".formatted(resource));
        } else if (resource.type == Type.RESOURCE && contents == null) {
            // Prepare content - use empty byte array for RESOURCE types if null
            contents = new byte[0];
        }

        final boolean existed = findByPath(resource.path()).isPresent();

        // Ensure parent directory exists
        PgconfigResource parent = ensureParentExists(resource);

        // Use UPSERT for atomic insert-or-update
        // COALESCE(EXCLUDED.content, resourcestore.content) means:
        //   - If new content provided (not null) -> use it
        //   - If new content is null -> keep existing content
        // This allows metadata-only updates (type, parent) without losing file content
        String sql =
                """
                INSERT INTO resourcestore (parentid, "type", path, content)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (parentid, path)
                DO UPDATE SET
                    "type" = EXCLUDED."type",
                    content = COALESCE(EXCLUDED.content, resourcestore.content),
                    mtime = timezone('UTC'::text, now());
                """;

        final long parentId = parent.getId();
        // Access type field directly to avoid triggering updateState()
        final String type = resource.type.toString();
        final String path = resource.path();

        template.update(sql, parentId, type, path, contents);

        // Refresh resource with current database state. An insert sets the mtime of the parent too, by trigger
        Map<String, ResourceRow> written = queryByPaths(path, parent.path());
        PgconfigResource updated = resourceAt(written, path);
        resource.id = updated.getId();
        resource.lastmodified = updated.lastmodified();
        resource.parentId = updated.getParentId();
        long id = updated.getId();
        Collection<ResourceRow> writtenRows = written.values();
        Runnable localChange = updatingIndex(
                storeCache -> {
                    storeCache.evictContent(id);
                    writtenRows.forEach(storeCache::put);
                },
                localCopyUpdate);
        changePublisher.afterCommit(savedEvent(updated, existed), localChange);
        return updated;
    }

    /**
     * Applies a write to the index once it commits, before the change of the local copies. Without an index, only the
     * local copies change.
     */
    private Runnable updatingIndex(Consumer<ResourceStoreCache> indexUpdate, Runnable localCopyUpdate) {
        ResourceStoreCache committedTo = index;
        if (committedTo == null) {
            return localCopyUpdate;
        }
        return () -> {
            indexUpdate.accept(committedTo);
            localCopyUpdate.run();
        };
    }

    // The UPSERT does not tell an insert from an update: the caller looks the row up before writing
    private static ResourceChanged savedEvent(PgconfigResource saved, boolean existed) {
        Kind kind = existed ? Kind.MODIFIED : Kind.CREATED;
        return ResourceChanged.createLocal(saved.path(), kind, saved.type, saved.lastmodified, List.of());
    }

    private PgconfigResource ensureParentExists(PgconfigResource resource) {
        PgconfigResource parent = resource.parent();
        if (parent == null) {
            // Only root resource has null parent
            throw new IllegalArgumentException(
                    "Cannot save resource with null parent (only root has null parent): %s".formatted(resource.path()));
        }
        parent = parent.mkdirs();
        return parent;
    }

    /**
     * Updates the state of a resource from the database.
     *
     * <p>This method is crucial for maintaining consistency of long-lived resource references. It reads the current
     * state of a resource, from the index when it is in use, and updates the provided resource instance with it.
     *
     * <p>It's particularly important for components like AbstractAccessRuleDAO and RESTAccessRuleDAO that hold resource
     * references as instance variables. These references can become stale when the underlying database record is
     * modified by another process or service instance.
     *
     * <p>If the resource no longer exists in the database, both its type is set to UNDEFINED and its id is set to
     * UNDEFINED_ID to ensure consistent state.
     *
     * @param resource the resource to update
     * @see PgconfigResource#updateState()
     */
    public void updateState(PgconfigResource resource) {
        Optional<PgconfigResource> indb = findByPath(resource.path());
        indb.ifPresentOrElse(
                // Resource found in database - copy all properties
                resource::reset,
                // Resource not found in database - mark as undefined
                () -> {
                    resource.type = Type.UNDEFINED;
                    resource.id = UNDEFINED_ID;
                    resource.parentId = UNDEFINED_ID;
                    // lastmodified intentionally not updated to avoid inconsistency with
                    // ResourceNotificationDispatcher
                });
    }

    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public boolean move(@NonNull final PgconfigResource source, @NonNull final PgconfigResource target) {
        return moveDbResource(source, target);
    }

    /**
     * Move implementation shared by the entry points. It joins the transaction of the caller, or runs in a new one:
     * in-class callers and resources reach it without the transactional proxy.
     */
    private boolean moveDbResource(final PgconfigResource source, final PgconfigResource target) {
        return inTransaction(() -> moveRows(source, target));
    }

    /**
     * Moves the row of the source and the rows of its descendants. Once the move commits, it announces the deletion of
     * the source and the creation of the target, in that order.
     *
     * <p>The source and the target are looked up in the database, inside the transaction. The resources hold the state
     * read from the index at their lookup, and a row written by another instance in the meantime is not in it. Several
     * instances starting at once write the same file, and the second one renames its copy onto a row it has not seen.
     */
    private boolean moveRows(final PgconfigResource source, final PgconfigResource target) {
        if (source.isUndefined()) {
            return true;
        }
        if (source.path().equals(target.path())) {
            return true;
        }
        Optional<ResourceRow> sourceRow = queryByPath(source.path());
        if (sourceRow.isEmpty()) {
            return false;
        }
        source.reset(toResource(sourceRow.get()));

        queryByPath(target.path()).map(this::toResource).ifPresent(this::deleteRow);
        final String parentPath = target.parentPath();
        if (null != parentPath && parentPath.contains(source.path())) {
            log.warn("Cannot rename a resource to a descendant of itself ({} to {})", source.path(), target.path());
            return false;
        }

        // Ensure target parent directory exists
        PgconfigResource parent = target.parent().mkdirs();

        // Listed before the source row moves: a revalidation of the moved source finds no row, and lists no children
        final List<PgconfigResource> allChildren = findAllChildren(source);
        final ResourceChanged movedAway = deletedEvent(source, allChildren);

        // Move the source by updating its path and parent in-place (preserves content and id)
        final String sql =
                """
                UPDATE resourcestore
                SET parentid = ?, path = ?
                WHERE id = ?
                """;
        template.update(sql, parent.getId(), target.path(), source.getId());

        // Update all children paths recursively
        final String oldParentPath = source.path();
        final String newParentPath = target.path();
        final List<String> movedChildPaths = new ArrayList<>();
        for (PgconfigResource child : allChildren) {
            String oldPath = child.path();
            String relativePath = oldPath.substring(oldParentPath.length());
            String newPath = newParentPath + relativePath;
            template.update("UPDATE resourcestore SET path = ? WHERE id = ?", newPath, child.getId());
            movedChildPaths.add(newPath);
        }

        // Update source resource to reflect it's been moved (type set to UNDEFINED)
        source.type = Type.UNDEFINED;
        source.id = UNDEFINED_ID;
        source.parentId = UNDEFINED_ID;

        // Update target resource with the moved state; look the row up directly, since get() on a hybrid
        // directory path (a mosaic store dir under data/) routes to the local-only adaptor. The triggers set the
        // mtime of each moved row, and of both parents
        Map<String, ResourceRow> moved = queryMovedRows(newParentPath, target.parentPath(), source.parentPath());
        target.reset(resourceAt(moved, newParentPath));

        Map<Boolean, List<ResourceRow>> inSubtree = moved.values().stream()
                .collect(Collectors.partitioningBy(row -> isInSubtree(row.path(), newParentPath)));
        List<ResourceRow> movedRows = inSubtree.get(true);
        List<ResourceRow> parentRows = inSubtree.get(false);
        Runnable localChange = updatingIndex(
                storeCache -> {
                    storeCache.moved(oldParentPath, newParentPath, movedRows);
                    parentRows.forEach(storeCache::put);
                },
                () -> localCopies.moved(source, target));
        changePublisher.afterCommit(movedAway, localChange);
        changePublisher.afterCommit(movedEvent(target, movedChildPaths));
        return true;
    }

    /** Reads the row at the target of a move, the rows of its subtree, and the rows of both parents. */
    private Map<String, ResourceRow> queryMovedRows(
            String targetPath, String targetParentPath, String sourceParentPath) {
        String sql = SELECT_ROWS + " WHERE path = ANY(?) OR starts_with(path, ?)";
        String[] paths = {targetPath, targetParentPath, sourceParentPath};
        String subtreePrefix = targetPath + "/";
        try (Stream<ResourceRow> rows = template.queryForStream(sql, ROW_MAPPER, paths, subtreePrefix)) {
            return rows.collect(Collectors.toMap(ResourceRow::path, Function.identity()));
        }
    }

    private static boolean isInSubtree(String path, String subtreePath) {
        return path.equals(subtreePath) || path.startsWith(subtreePath + "/");
    }

    private static ResourceChanged movedEvent(PgconfigResource target, List<String> movedChildPaths) {
        List<Change> changes = movedChildPaths.stream()
                .map(path -> new Change(path, Kind.CREATED))
                .toList();
        return ResourceChanged.createLocal(target.path(), Kind.CREATED, target.type, target.lastmodified, changes);
    }

    /**
     * Materializes into the local cache any database-backed files living under the given local-only directory path
     * (e.g. mosaic config files under {@code data/<ws>/<store>}), never overwriting locally newer files. No-op when the
     * directory has no database-backed children.
     */
    void dumpDbBackedChildren(String path) {
        findByPath(path)
                .filter(PgconfigResource::isDirectory)
                .ifPresent(dbDir -> localCopies.materialize(findAllChildren(dbDir)));
    }

    /**
     * Paths of the database-backed file rows under the given directory path, whether or not their local cache copies
     * exist. Empty when the path has no database row.
     */
    public List<String> dbBackedFilePaths(@NonNull String path) {
        return findByPath(normalize(path)).map(this::findAllChildren).orElseGet(List::of).stream()
                .filter(PgconfigResource::isFile)
                .map(PgconfigResource::path)
                .filter(dbBackedFilePatterns)
                .toList();
    }

    List<PgconfigResource> findAllChildren(PgconfigResource resource) {
        if (!resource.exists() || !resource.isDirectory()) {
            return List.of();
        }
        List<ResourceRow> rows;
        if (useIndex()) {
            rows = index.descendants(resource.path());
        } else {
            rows = queryDescendants(resource.path());
        }
        return toResources(rows);
    }

    /**
     * Reads the rows below a path. The descendants of the root are the rows of the store but the root, as in the index.
     * The prefix is compared as plain text: in a {@code LIKE} pattern, the {@code _} of a name such as {@code ws_1}
     * would also match the {@code 2} of {@code ws21}.
     */
    private List<ResourceRow> queryDescendants(String path) {
        String sql = SELECT_ROWS + " WHERE starts_with(path, ?) AND id <> ?";
        String prefix = path.isEmpty() ? "" : path + "/";
        try (Stream<ResourceRow> rows = template.queryForStream(sql, ROW_MAPPER, prefix, ROOT_ID)) {
            return rows.toList();
        }
    }

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return changePublisher.dispatcher();
    }

    /** @return */
    public byte[] contents(PgconfigResource resource) {
        if (!resource.exists() || resource.isUndefined()) {
            throw new IllegalStateException("File not found %s".formatted(resource.path()));
        }
        if (resource.isDirectory()) {
            throw new IllegalStateException("%s is a directory".formatted(resource.path()));
        }

        long id = resource.getId();
        if (useIndex()) {
            return index.content(id, () -> queryContent(id));
        }
        return queryContent(id);
    }

    private byte[] queryContent(long id) {
        String sql = """
                SELECT content FROM resourcestore WHERE id = ?
                """;
        return template.queryForObject(sql, byte[].class, id);
    }

    public boolean delete(PgconfigResource resource) {
        return inTransaction(() -> deleteRow(resource));
    }

    /**
     * Deletes the row of a resource, and the rows of its descendants by cascade. Once the deletion commits, it removes
     * the local copies and announces the deletion.
     */
    private boolean deleteRow(PgconfigResource resource) {
        // Listed before the cascade removes them
        List<PgconfigResource> descendants = findAllChildren(resource);
        String sql = """
                DELETE FROM resourcestore WHERE id = ?
                """;
        boolean deleted = 0 < template.update(sql, resource.getId());
        if (deleted) {
            ResourceChanged event = deletedEvent(resource, descendants);
            String path = resource.path();
            resource.type = Type.UNDEFINED;
            // The deletion sets the mtime of the parent, by trigger
            Optional<ResourceRow> parent =
                    Optional.ofNullable(resource.parentPath()).flatMap(this::queryByPath);
            Runnable localChange = updatingIndex(
                    storeCache -> {
                        storeCache.remove(path);
                        parent.ifPresent(storeCache::put);
                    },
                    () -> localCopies.deleted(path));
            changePublisher.afterCommit(event, localChange);
        }
        return deleted;
    }

    // The mtime of a deleted resource is the last one read from its row
    private static ResourceChanged deletedEvent(PgconfigResource deleted, List<PgconfigResource> descendants) {
        List<Change> changes = descendants.stream()
                .map(descendant -> new Change(descendant.path(), Kind.DELETED))
                .toList();
        return ResourceChanged.createLocal(deleted.path(), Kind.DELETED, deleted.type, deleted.lastmodified, changes);
    }

    /** @return direct children of resource if resource is a directory, empty list otherwise */
    public List<Resource> list(PgconfigResource resource) {
        if (!resource.exists() || !resource.isDirectory()) {
            return List.of();
        }

        List<ResourceRow> rows;
        if (useIndex()) {
            rows = index.children(toDirectoryRow(resource));
        } else {
            rows = queryChildren(resource.getId());
        }
        List<Resource> list = withoutFileSystemOnly(rows);
        localCopies.updateAll(list);
        return list;
    }

    // Reads the fields: the getters would revalidate the resource. A directory has no content
    private static ResourceRow toDirectoryRow(PgconfigResource directory) {
        return new ResourceRow(
                directory.id, directory.parentId, directory.type, directory.path, directory.lastmodified, 0);
    }

    private List<ResourceRow> queryChildren(long parentId) {
        String sql = SELECT_ROWS + " WHERE parentid = ?";
        try (Stream<ResourceRow> rows = template.queryForStream(sql, ROW_MAPPER, parentId)) {
            return rows.toList();
        }
    }

    // for pre 1.8.1 backwards compatibility, ignore resources that are only to be stored in the filesystem (e.g.
    // tmp/, temp/, etc)
    private List<Resource> withoutFileSystemOnly(List<ResourceRow> rows) {
        return rows.stream()
                .filter(row -> !fileSystemOnlyPathMatcher.test(row.path()))
                .map(this::toResource)
                .map(Resource.class::cast)
                .toList();
    }

    /** @return */
    public File asFile(PgconfigResource resource) {
        if (!resource.exists()) {
            resource.type = Type.RESOURCE;
            PgconfigResourceStore.this.save(resource);
        }
        loadStylesFolderOf(resource);
        return localCopies.getFile(resource);
    }

    /**
     * Writes the local copies of a styles folder with one query, if the file sits in one and its copy is missing or
     * outdated, see {@code StylesFolderLoader}. The folder load reads the rows of the index: within a transaction, the
     * store reads the database, and the file is written alone.
     */
    private void loadStylesFolderOf(PgconfigResource resource) {
        if (stylesFolderLoader != null && useIndex()) {
            index.find(resource.path()).ifPresent(stylesFolderLoader::loadFolderOf);
        }
    }

    /**
     * @param resource
     * @return
     */
    public File asDir(PgconfigResource resource) {
        if (!resource.exists()) {
            resource.type = Type.DIRECTORY;
            PgconfigResourceStore.this.save(resource);
        }
        // skip the root: PgconfigGeoServerResourceLoader calls get("").dir() at startup, and
        // materializing its children would dump the entire resourcestore table to disk
        if (!resource.path().isEmpty()) {
            localCopies.materialize(findAllChildren(resource));
        }
        return localCopies.getDirectory(resource);
    }

    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public PgconfigResource mkdirs(PgconfigResource resource) {
        return inTransaction(() -> createDirectories(resource));
    }

    // The saves of the created directories announce them, before the save of the resource inside them
    private PgconfigResource createDirectories(PgconfigResource resource) {
        if (resource.exists() && resource.isDirectory()) {
            return resource;
        }
        if (resource.isFile()) {
            throw new IllegalStateException("mkdirs() can only be called on DIRECTORY or UNDEFINED resources");
        }
        PgconfigResource parent = getParent(resource);
        if (null == parent) {
            return resource;
        }
        if (!parent.exists()) {
            parent = parent.mkdirs();
        }
        resource.parentId = parent.getId();
        resource.type = Type.DIRECTORY;
        PgconfigResourceStore.this.save(resource);
        PgconfigResource saved = findByPathOrUndefined(resource.path());
        resource.reset(saved);
        return resource;
    }

    public OutputStream out(PgconfigResource resource) {
        if (resource.isDirectory()) {
            throw new IllegalStateException("%s is a directory".formatted(resource.path()));
        }

        resource.type = Type.RESOURCE;

        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                byte[] contents = this.toByteArray();
                Runnable localCopyUpdate = () -> localCopies.dump(resource, new ByteArrayInputStream(contents));
                inTransaction(() -> saveRow(resource, contents, localCopyUpdate));
            }
        };
    }

    public PgconfigResource getParent(PgconfigResource resource) {
        if (ROOT_ID == resource.getId()) {
            return null;
        }
        String parentPath = resource.parentPath();
        return findByPathOrUndefined(parentPath);
    }
}
