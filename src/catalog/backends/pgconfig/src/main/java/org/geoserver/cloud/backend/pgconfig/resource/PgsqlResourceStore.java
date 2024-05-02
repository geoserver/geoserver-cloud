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
public class PgsqlResourceStore implements ResourceStore {
    static final long ROOT_ID = 0L;
    static final long UNDEFINED_ID = -1L;

    private final JdbcTemplate template;
    private final FileSystemResourceStoreCache cache;

    private @NonNull @Getter @Setter LockProvider lockProvider;

    private final PgsqlResourceRowMapper queryMapper;

    private final Predicate<String> fileSystemOnlyPathMatcher;

    public PgsqlResourceStore(
            @NonNull Path cacheDirectory,
            @NonNull JdbcTemplate template,
            @NonNull PgsqlLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher) {
        this(
                FileSystemResourceStoreCache.of(cacheDirectory),
                template,
                lockProvider,
                fileSystemOnlyPathMatcher);
    }

    public PgsqlResourceStore(
            @NonNull FileSystemResourceStoreCache cache,
            @NonNull JdbcTemplate template,
            @NonNull PgsqlLockProvider lockProvider,
            @NonNull Predicate<String> fileSystemOnlyPathMatcher) {
        this.template = template;
        this.lockProvider = lockProvider;
        this.queryMapper = new PgsqlResourceRowMapper(this);
        this.cache = cache;
        final String root = "";
        Predicate<String> notRoot = path -> !root.equals(path);
        this.fileSystemOnlyPathMatcher = notRoot.and(fileSystemOnlyPathMatcher);
    }

    public static Predicate<String> defaultIgnoredDirs() {
        return PgsqlResourceStore.simplePathMatcher("temp", "tmp", "legendsamples", "data", "logs");
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
        private final @NonNull PgsqlResourceStore store;

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
        return findByPath(validPath).map(PgsqlResource::delete).orElse(false);
    }

    @Override
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public boolean move(@NonNull String path, @NonNull String target) {
        Resource from = get(path);
        Resource to = get(target);
        if (from instanceof PgsqlResource pgFrom && to instanceof PgsqlResource pgTo) {
            return PgsqlResourceStore.this.move(pgFrom, pgTo);
        }
        if (from instanceof PgsqlResource) {
            throw new UnsupportedOperationException(
                    "source resource targets database but target resource matches the ignored resources predicate. Source: %s, target: %s"
                            .formatted(path, target));
        }
        if (to instanceof PgsqlResource) {
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

    private Optional<PgsqlResource> findByPath(@NonNull String path) {
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
     * @throws IllegalArgumentException if {@link PgsqlResource#isUndefined()}
     */
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public void save(@NonNull PgsqlResource resource) {
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
            PgsqlResource parent = resource.parent().mkdirs();
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
        PgsqlResource updated = (PgsqlResource) get(resource.path);
        resource.id = updated.getId();
        resource.lastmodified = updated.lastmodified();
        resource.parentId = updated.getParentId();
    }

    /**
     * Saves the contents of the given resource
     *
     * @return the new resource lastupdated timestamp
     * @throws IllegalArgumentException if <code>
     *      {@link PgsqlResource#isDirectory() resource.isDirectory()} || !{@link
     *     PgsqlResource#exists() resource.exists()}</code>
     */
    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public long save(@NonNull PgsqlResource resource, byte[] contents) {
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
    public boolean move(@NonNull final PgsqlResource source, @NonNull final PgsqlResource target) {
        if (source.isUndefined()) return true;
        if (!source.exists()) {
            return false;
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
        final List<PgsqlResource> allChildren = findAllChildren(source);
        PgsqlResource parent = target.parent().mkdirs();
        PgsqlResource save =
                new PgsqlResource(
                        this,
                        source.getId(),
                        parent.getId(),
                        source.getType(),
                        target.path(),
                        source.lastmodified());
        PgsqlResourceStore.this.save(save);
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

    List<PgsqlResource> findAllChildren(PgsqlResource resource) {
        if (!resource.exists() || !resource.isDirectory()) return List.of();
        String sql =
                """
				SELECT id, parentid, "type", path, mtime FROM resourcestore WHERE path LIKE ?
				""";

        String likeQuery = resource.path() + "/%";
        try (Stream<PgsqlResource> s = template.queryForStream(sql, queryMapper, likeQuery)) {
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
    public byte[] contents(PgsqlResource resource) {
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

    public boolean delete(PgsqlResource resource) {
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
    public List<Resource> list(PgsqlResource resource) {
        if (!resource.exists() || !resource.isDirectory()) return List.of();

        String sql =
                """
				SELECT id, parentid, "type", path, mtime FROM resourcestore WHERE parentid = ?
				""";

        List<Resource> list;
        try (Stream<PgsqlResource> s =
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
    public File asFile(PgsqlResource resource) {
        if (!resource.exists()) {
            resource.type = Type.RESOURCE;
            PgsqlResourceStore.this.save(resource);
        }
        return cache.getFile(resource);
    }

    /**
     * @param resource
     * @return
     */
    public File asDir(PgsqlResource resource) {
        if (!resource.exists()) {
            resource.type = Type.DIRECTORY;
            PgsqlResourceStore.this.save(resource);
        }
        return cache.getDirectory(resource);
    }

    @Transactional(transactionManager = "pgconfigTransactionManager", propagation = REQUIRED)
    public PgsqlResource mkdirs(PgsqlResource resource) {
        if (resource.exists() && resource.isDirectory()) {
            return resource;
        }
        if (resource.isFile())
            throw new IllegalStateException(
                    "mkdirs() can only be called on DIRECTORY or UNDEFINED resources");

        PgsqlResource parent = getParent(resource);
        if (null == parent) return resource;
        if (!parent.exists()) {
            parent = (PgsqlResource) parent.mkdirs();
        }
        resource.parentId = parent.getId();
        resource.type = Type.DIRECTORY;
        PgsqlResourceStore.this.save(resource);
        PgsqlResource saved = (PgsqlResource) get(resource.path());
        resource.copy(saved);
        return resource;
    }

    public OutputStream out(PgsqlResource res) {
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
                    PgsqlResourceStore.this.save(res);
                    PgsqlResource saved = findByPath(path).orElseThrow();
                    res.copy(saved);
                }
                byte[] contents = this.toByteArray();
                long mtime = PgsqlResourceStore.this.save(res, contents);
                res.lastmodified = mtime;
                cache.dump(res, new ByteArrayInputStream(contents));
            }
        };
    }

    public PgsqlResource getParent(PgsqlResource resource) {
        if (ROOT_ID == resource.getId()) return null;
        String parentPath = resource.parentPath();
        try {
            return (PgsqlResource) get(parentPath);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
