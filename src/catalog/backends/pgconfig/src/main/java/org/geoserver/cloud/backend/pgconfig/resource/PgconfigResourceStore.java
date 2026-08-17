/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.LoggingTemplate;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

/** @since 1.4 */
@Slf4j
@Transactional(transactionManager = "pgconfigTransactionManager", propagation = SUPPORTS)
public class PgconfigResourceStore implements ResourceStore {

    static final long ROOT_ID = 0L;
    static final long UNDEFINED_ID = -1L;

    private final LoggingTemplate template;
    private final FileSystemResourceStoreCache cache;

    private @NonNull @Getter @Setter LockProvider lockProvider;

    private final PgconfigResourceRowMapper queryMapper;

    private final Predicate<String> fileSystemOnlyPathMatcher;

    private final Predicate<String> dbBackedFilePatterns;

    public PgconfigResourceStore(
            @NonNull FileSystemResourceStoreCache cache,
            @NonNull JdbcTemplate template,
            @NonNull PgconfigLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher,
            @NonNull Predicate<String> dbBackedFilePatterns) {
        this.template = new LoggingTemplate(template).setLog(log);
        this.lockProvider = lockProvider;
        this.queryMapper = new PgconfigResourceRowMapper(this);
        this.cache = cache;
        final String root = "";
        Predicate<String> notRoot = path -> !root.equals(path);
        this.dbBackedFilePatterns = dbBackedFilePatterns;
        this.fileSystemOnlyPathMatcher = notRoot.and(fileSystemOnlyPathMatcher.and(dbBackedFilePatterns.negate()));
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
        Predicate<String> matcher = path -> false;
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
            Resource fsResource = cache.getLocalOnlyStore().get(validPath);
            return new FileSystemResourceAdaptor(fsResource, this);
        }
        return findByPath(validPath).orElseGet(() -> queryMapper.undefined(validPath));
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
            boolean removedLocally = cache.getLocalOnlyStore().remove(validPath);
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
        return cache.getLocalOnlyStore().move(path, target);
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
        try {
            return Optional.of(template.queryForObject(
                    """
                    SELECT id, parentid, "type", path, mtime FROM resourcestore WHERE path = ?
                    """,
                    queryMapper,
                    path));
        } catch (EmptyResultDataAccessException _) {
            return Optional.empty();
        }
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
        return findByPath(path).orElseGet(() -> queryMapper.undefined(path));
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

        // Refresh resource with current database state
        PgconfigResource updated = findByPathOrUndefined(path);
        resource.id = updated.getId();
        resource.lastmodified = updated.lastmodified();
        resource.parentId = updated.getParentId();
        return updated;
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
     * <p>This method is crucial for maintaining consistency of long-lived resource references. It queries the database
     * for the current state of a resource and updates the provided resource instance with the latest information.
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
     * Move implementation shared by the transactional entry points. In-class callers already run within the transaction
     * begun by {@link #move(String, String)} and use this method directly: self-invoking the {@code @Transactional}
     * overload through {@code this} would bypass the transactional proxy.
     */
    private boolean moveDbResource(final PgconfigResource source, final PgconfigResource target) {
        if (source.isUndefined()) {
            return true;
        }
        if (!source.exists()) {
            return false;
        }
        if (source.path().equals(target.path())) {
            return true;
        }

        if (target.exists()) {
            target.delete();
        }
        final String parentPath = target.parentPath();
        if (null != parentPath && parentPath.contains(source.path())) {
            log.warn("Cannot rename a resource to a descendant of itself ({} to {})", source.path(), target.path());
            return false;
        }

        // Ensure target parent directory exists
        PgconfigResource parent = target.parent().mkdirs();

        // Move the source by updating its path and parent in-place (preserves content and id)
        final String sql =
                """
                UPDATE resourcestore
                SET parentid = ?, path = ?
                WHERE id = ?
                """;
        template.update(sql, parent.getId(), target.path(), source.getId());

        // Update all children paths recursively
        final List<PgconfigResource> allChildren = findAllChildren(source);
        final String oldParentPath = source.path();
        final String newParentPath = target.path();
        for (PgconfigResource child : allChildren) {
            String oldPath = child.path();
            String relativePath = oldPath.substring(oldParentPath.length());
            String newPath = newParentPath + relativePath;
            template.update("UPDATE resourcestore SET path = ? WHERE id = ?", newPath, child.getId());
        }

        // Update source resource to reflect it's been moved (type set to UNDEFINED)
        source.type = Type.UNDEFINED;
        source.id = UNDEFINED_ID;
        source.parentId = UNDEFINED_ID;

        // Update target resource with the moved state; look the row up directly, since get() on a hybrid
        // directory path (a mosaic store dir under data/) routes to the local-only adaptor
        PgconfigResource moved = findByPathOrUndefined(target.path());
        target.reset(moved);

        cache.moved(source, target);
        return true;
    }

    /**
     * Materializes into the local cache any database-backed files living under the given local-only directory path
     * (e.g. mosaic config files under {@code data/<ws>/<store>}), never overwriting locally newer files. No-op when the
     * directory has no database-backed children.
     */
    void dumpDbBackedChildren(String path) {
        findByPath(path)
                .filter(PgconfigResource::isDirectory)
                .ifPresent(dbDir -> cache.materialize(findAllChildren(dbDir)));
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
        String sql =
                """
                SELECT id, parentid, "type", path, mtime FROM resourcestore WHERE path LIKE ?
                """;

        String likeQuery = resource.path() + "/%";
        try (Stream<PgconfigResource> s = template.queryForStream(sql, queryMapper, likeQuery)) {
            return s.toList();
        }
    }

    private ResourceNotificationDispatcher dispatcher = new SimpleResourceNotificationDispatcher();

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return dispatcher;
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
        return template.queryForObject(
                """
                SELECT content FROM resourcestore WHERE id = ?
                """,
                byte[].class,
                id);
    }

    public boolean delete(PgconfigResource resource) {
        String sql = """
                DELETE FROM resourcestore WHERE id = ?
                """;
        boolean deleted = 0 < template.update(sql, resource.getId());
        if (deleted) {
            resource.type = Type.UNDEFINED;
            cache.deleted(resource.path());
        }
        return deleted;
    }

    /** @return direct children of resource if resource is a directory, empty list otherwise */
    public List<Resource> list(PgconfigResource resource) {
        if (!resource.exists() || !resource.isDirectory()) {
            return List.of();
        }

        String sql =
                """
                SELECT id, parentid, "type", path, mtime FROM resourcestore WHERE parentid = ?
                """;

        List<Resource> list;
        try (Stream<PgconfigResource> s = template.queryForStream(sql, queryMapper, resource.getId())) {
            // for pre 1.8.1 backwards compatibility, ignore resources that are only to be
            // stored in
            // the filesystem (e.g. tmp/, temp/, etc)
            Stream<PgconfigResource> resources = s.filter(r -> !fileSystemOnlyPathMatcher.test(r.path()));
            list = resources.map(Resource.class::cast).toList();
        }
        cache.updateAll(list);
        return list;
    }

    /** @return */
    public File asFile(PgconfigResource resource) {
        if (!resource.exists()) {
            resource.type = Type.RESOURCE;
            PgconfigResourceStore.this.save(resource);
        }
        return cache.getFile(resource);
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
            cache.materialize(findAllChildren(resource));
        }
        return cache.getDirectory(resource);
    }

    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public PgconfigResource mkdirs(PgconfigResource resource) {
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
                PgconfigResourceStore.this.save(resource, contents);
                cache.dump(resource, new ByteArrayInputStream(contents));
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
