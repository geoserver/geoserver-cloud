/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

import com.google.common.base.Preconditions;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
@Slf4j
@Transactional(transactionManager = "pgconfigTransactionManager", propagation = SUPPORTS)
public class PgconfigResourceStore implements ResourceStore {
    static final long ROOT_ID = 0L;
    static final long UNDEFINED_ID = -1L;

    private final JdbcTemplate template;
    private final FileSystemResourceStoreCache cache;

    private @NonNull @Getter @Setter LockProvider lockProvider;

    private final PgconfigResourceRowMapper queryMapper;

    private final Predicate<String> fileSystemOnlyPathMatcher;

    public PgconfigResourceStore(
            @NonNull Path cacheDirectory,
            @NonNull JdbcTemplate template,
            @NonNull PgconfigLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher) {
        this(
                FileSystemResourceStoreCache.of(cacheDirectory),
                template,
                lockProvider,
                fileSystemOnlyPathMatcher);
    }

    public PgconfigResourceStore(
            @NonNull FileSystemResourceStoreCache cache,
            @NonNull JdbcTemplate template,
            @NonNull PgconfigLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher) {
        this.template = template;
        this.lockProvider = lockProvider;
        this.queryMapper = new PgconfigResourceRowMapper(this);
        this.cache = cache;
        final String root = "";
        Predicate<String> notRoot = path -> !root.equals(path);
        this.fileSystemOnlyPathMatcher = notRoot.and(fileSystemOnlyPathMatcher);
    }

    public static Predicate<String> defaultIgnoredDirs() {
        return PgconfigResourceStore.simplePathMatcher(
                "temp", "tmp", "legendsamples", "data", "logs");
    }

    public static Predicate<String> simplePathMatcher(String... paths) {
        Predicate<String> matcher = path -> false;
        for (String path : paths) {
            path = normalize(path);
            matcher = matcher.or(path::equals);
            final String dirpath = path + "/";
            matcher = matcher.or(r -> r.startsWith(dirpath));
        }
        return matcher;
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
        @Delegate @NonNull private final Resource delegate;
        private final @NonNull PgconfigResourceStore store;

        @Override
        public Resource parent() {
            String parentPath = Paths.parent(this.path());
            return store.get(parentPath);
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
            return cache.getLocalOnlyStore().remove(validPath);
        }
        return findByPath(validPath).map(PgconfigResource::delete).orElse(false);
    }

    @Override
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public boolean move(@NonNull String path, @NonNull String target) {
        Resource from = get(path);
        Resource to = get(target);
        if (from instanceof PgconfigResource pgFrom && to instanceof PgconfigResource pgTo) {
            return PgconfigResourceStore.this.move(pgFrom, pgTo);
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

    private Optional<PgconfigResource> findByPath(@NonNull String path) {
        path = Paths.valid(path);
        Preconditions.checkArgument(
                !path.startsWith("/"), "Absolute paths not supported: %s", path);
        try {
            return Optional.of(
                    template.queryForObject(
                            """
					SELECT id, parentid, "type", path, mtime FROM resourcestore WHERE path = ?
					""",
                            queryMapper,
                            path));
        } catch (EmptyResultDataAccessException empty) {
            return Optional.empty();
        }
    }

    /**
     * Creates the resource if it doesn't exist, updates it if it does
     *
     * @throws IllegalArgumentException if {@link PgconfigResource#isUndefined()}
     */
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public void save(@NonNull PgconfigResource resource) {
        if (resource.isUndefined())
            throw new IllegalArgumentException(
                    "Attempting to save a resource of undefined type: %s".formatted(resource));

        if (resource.exists()) {
            String sql =
                    """
					UPDATE resourcestore SET parentid = ?, "type" = ?, path = ?
					WHERE id = ?;
					""";
            long id = resource.getId();
            long parentId = resource.getParentId();
            String type = resource.getType().toString();
            String path = resource.path();
            template.update(sql, parentId, type, path, id);
        } else {
            PgconfigResource parent = resource.parent().mkdirs();
            String sql =
                    """
					INSERT INTO resourcestore (parentid, "type", path, content)
					VALUES (?, ?, ?, ?);
					""";
            long parentId = parent.getId();
            String type = resource.getType().toString();
            String path = resource.path();
            byte[] contents = resource.getType() == Type.DIRECTORY ? null : new byte[0];
            template.update(sql, parentId, type, path, contents);
        }
        PgconfigResource updated = (PgconfigResource) get(resource.path);
        resource.id = updated.getId();
        resource.lastmodified = updated.lastmodified();
        resource.parentId = updated.getParentId();
    }

    /**
     * Saves the contents of the given resource
     *
     * @return the new resource lastupdated timestamp
     * @throws IllegalArgumentException if <code>
     *      {@link PgconfigResource#isDirectory() resource.isDirectory()} || !{@link
     *     PgconfigResource#exists() resource.exists()}</code>
     */
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public long save(@NonNull PgconfigResource resource, byte[] contents) {
        if (!resource.exists())
            throw new IllegalArgumentException(
                    "Resource does not exist: %s".formatted(resource.path()));

        if (!resource.isFile())
            throw new IllegalArgumentException(
                    "Resource is a directory, can't have contents: %s".formatted(resource.path()));

        if (null == contents) contents = new byte[0];
        template.update(
                """
				UPDATE resourcestore SET content = ? WHERE id = ?
				""",
                contents,
                resource.getId());
        return getLastmodified(resource.getId());
    }

    public long getLastmodified(long resourceId) {
        Timestamp ts =
                template.queryForObject(
                        "SELECT mtime FROM resourcestore WHERE id = ?",
                        Timestamp.class,
                        resourceId);
        return null == ts ? 0L : ts.getTime();
    }

    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public boolean move(
            @NonNull final PgconfigResource source, @NonNull final PgconfigResource target) {
        if (source.isUndefined()) return true;
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
            log.warn(
                    "Cannot rename a resource to a descendant of itself ({} to {})",
                    source.path(),
                    target.path());
            return false;
        }
        final List<PgconfigResource> allChildren = findAllChildren(source);
        PgconfigResource parent = target.parent().mkdirs();
        PgconfigResource save =
                new PgconfigResource(
                        this,
                        source.getId(),
                        parent.getId(),
                        source.getType(),
                        target.path(),
                        source.lastmodified());
        PgconfigResourceStore.this.save(save);
        target.copy(save);
        source.type = Type.UNDEFINED;

        final String oldParentPath = source.path();
        final String newParehtPath = target.path();
        for (var child : allChildren) {
            String oldPath = child.path().substring(oldParentPath.length());
            String newPath = newParehtPath + oldPath;
            String sql = "UPDATE resourcestore SET path = ? WHERE id = ?;";
            long id = child.getId();
            template.update(sql, newPath, id);
        }

        cache.moved(source, target);
        return true;
    }

    List<PgconfigResource> findAllChildren(PgconfigResource resource) {
        if (!resource.exists() || !resource.isDirectory()) return List.of();
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

    /**
     * @return
     */
    public byte[] contents(PgconfigResource resource) {
        if (!resource.exists() || resource.isUndefined())
            throw new IllegalStateException("File not found %s".formatted(resource.path()));
        if (resource.isDirectory())
            throw new IllegalStateException("%s is a directory".formatted(resource.path()));

        long id = resource.getId();
        return template.queryForObject(
                """
				SELECT content FROM resourcestore WHERE id = ?
				""", byte[].class, id);
    }

    public boolean delete(PgconfigResource resource) {
        String sql = """
				DELETE FROM resourcestore WHERE id = ?
				""";
        boolean deleted = 0 < template.update(sql, resource.getId());
        if (deleted) {
            resource.type = Type.UNDEFINED;
        }
        return deleted;
    }

    /**
     * @return direct children of resource iif resource is a directory, empty list otherwise
     */
    public List<Resource> list(PgconfigResource resource) {
        if (!resource.exists() || !resource.isDirectory()) return List.of();

        String sql =
                """
				SELECT id, parentid, "type", path, mtime FROM resourcestore WHERE parentid = ?
				""";

        List<Resource> list;
        try (Stream<PgconfigResource> s =
                template.queryForStream(sql, queryMapper, resource.getId())) {
            // for pre 1.8.1 backwards compatibility, ignore resources that are only to be stored in
            // the filesystem (e.g. tmp/, temp/, etc)
            var resources = s.filter(r -> !fileSystemOnlyPathMatcher.test(r.path()));
            list = resources.map(Resource.class::cast).toList();
        }
        cache.updateAll(list);
        return list;
    }

    /**
     * @return
     */
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
        return cache.getDirectory(resource);
    }

    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public PgconfigResource mkdirs(PgconfigResource resource) {
        if (resource.exists() && resource.isDirectory()) {
            return resource;
        }
        if (resource.isFile())
            throw new IllegalStateException(
                    "mkdirs() can only be called on DIRECTORY or UNDEFINED resources");

        PgconfigResource parent = getParent(resource);
        if (null == parent) return resource;
        if (!parent.exists()) {
            parent = (PgconfigResource) parent.mkdirs();
        }
        resource.parentId = parent.getId();
        resource.type = Type.DIRECTORY;
        PgconfigResourceStore.this.save(resource);
        PgconfigResource saved = (PgconfigResource) get(resource.path());
        resource.copy(saved);
        return resource;
    }

    public OutputStream out(PgconfigResource res) {
        if (res.isDirectory()) {
            throw new IllegalStateException("%s is a directory".formatted(res.path()));
        }
        if (res.isUndefined()) {
            res.type = Type.RESOURCE;
        }
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                if (!res.exists()) {
                    String path = res.path();
                    PgconfigResourceStore.this.save(res);
                    PgconfigResource saved = findByPath(path).orElseThrow();
                    res.copy(saved);
                }
                byte[] contents = this.toByteArray();
                long mtime = PgconfigResourceStore.this.save(res, contents);
                res.lastmodified = mtime;
                cache.dump(res, new ByteArrayInputStream(contents));
            }
        };
    }

    public PgconfigResource getParent(PgconfigResource resource) {
        if (ROOT_ID == resource.getId()) return null;
        String parentPath = resource.parentPath();
        return (PgconfigResource) get(parentPath);
    }
}
