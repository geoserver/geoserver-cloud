/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.resource;

import com.google.common.base.Preconditions;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
@Slf4j
public class PgsqlResourceStore implements ResourceStore {

    private final JdbcTemplate template;
    private final FileSystemResourceStoreCache cache;

    private @NonNull @Getter @Setter LockProvider lockProvider;

    private final PgsqlResourceRowMapper queryMapper;

    public PgsqlResourceStore(
            @NonNull Path cacheDirectory,
            @NonNull JdbcTemplate template,
            @NonNull PgsqlLockProvider lockProvider) {
        this(FileSystemResourceStoreCache.of(cacheDirectory), template, lockProvider);
    }

    public PgsqlResourceStore(
            @NonNull FileSystemResourceStoreCache cache,
            @NonNull JdbcTemplate template,
            @NonNull PgsqlLockProvider lockProvider) {
        this.template = template;
        this.lockProvider = lockProvider;
        this.queryMapper = new PgsqlResourceRowMapper(this);
        this.cache = cache;
    }

    @Override
    public Resource get(@NonNull String path) {
        if (Paths.isAbsolute(path)) {
            return Files.asResource(new File(path));
        }
        return findByPath(path).orElseGet(() -> queryMapper.undefined(path));
    }

    public Optional<PgsqlResource> findByPath(@NonNull String path) {
        path = Paths.valid(path);
        Preconditions.checkArgument(
                !path.startsWith("/"), "Absolute paths not supported: %s", path);
        try {
            return Optional.of(
                    template.queryForObject(
                            """
					SELECT id, parentid, "type", path, mtime FROM resources WHERE path = ?
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
    public void save(@NonNull PgsqlResource resource) {
        if (resource.isUndefined())
            throw new IllegalArgumentException(
                    "Attempting to save a resource of undefined type: %s".formatted(resource));

        if (resource.exists()) {
            String sql =
                    """
					UPDATE resourcestore SET parentid = ?, "type" = ?, name = ?
					WHERE id = ?;
					""";
            long id = resource.getId();
            long parentId = resource.getParentId();
            String type = resource.getType().toString();
            String name = resource.name();
            template.update(sql, parentId, type, name, id);
        } else {
            PgsqlResource parent = resource.parent().mkdirs();
            String sql =
                    """
					INSERT INTO resourcestore (parentid, "type", name, content)
					VALUES (?, ?, ?, ?);
					""";
            long parentId = parent.getId();
            String type = resource.getType().toString();
            String name = resource.name();
            byte[] contents = resource.getType() == Type.DIRECTORY ? null : new byte[0];
            template.update(sql, parentId, type, name, contents);
        }
    }

    /**
     * Saves the contents of the given resource
     *
     * @return the new resource lastupdated timestamp
     * @throws IllegalArgumentException if <code>
     *      {@link PgsqlResource#isDirectory() resource.isDirectory()} || !{@link
     *     PgsqlResource#exists() resource.exists()}</code>
     */
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

    @Override
    public boolean remove(@NonNull String path) {
        return findByPath(path).map(PgsqlResource::delete).orElse(false);
    }

    @Override
    public boolean move(@NonNull String path, @NonNull String target) {
        ensureNotAbsolute(path);
        ensureNotAbsolute(target);
        return move((PgsqlResource) get(path), (PgsqlResource) get(target));
    }

    private void ensureNotAbsolute(String path) {
        Preconditions.checkArgument(
                !Paths.isAbsolute(path), "Absolute paths not supported: %s", path);
    }

    public boolean move(@NonNull PgsqlResource source, @NonNull PgsqlResource target) {
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
        PgsqlResource parent = target.parent().mkdirs();
        PgsqlResource save =
                new PgsqlResource(
                        this,
                        source.getId(),
                        parent.getId(),
                        source.getType(),
                        target.path(),
                        source.lastmodified());
        save(save);
        target.copy(save);
        source.type = Type.UNDEFINED;
        cache.moved(source, target);
        return true;
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
				SELECT id, parentid, "type", path, mtime FROM resources WHERE parentid = ?
				""";

        List<Resource> list;
        try (Stream<PgsqlResource> s =
                template.queryForStream(sql, queryMapper, resource.getId())) {
            list = s.map(Resource.class::cast).toList();
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
            save(resource);
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
            save(resource);
        }
        return cache.getDirectory(resource);
    }

    public void mkdirs(PgsqlResource resource) {
        if (resource.exists() && resource.isDirectory()) {
            return;
        }
        if (resource.isFile())
            throw new IllegalStateException(
                    "mkdirs() can only be called on DIRECTORY or UNDEFINED resources");

        PgsqlResource parent = resource.parent();
        if (null == parent) return;
        if (!parent.exists()) {
            parent = parent.mkdirs();
        }
        resource.parentId = parent.getId();
        resource.type = Type.DIRECTORY;
        save(resource);
        PgsqlResource saved = (PgsqlResource) get(resource.path());
        resource.copy(saved);
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
                    save(res);
                    PgsqlResource saved = findByPath(path).orElseThrow();
                    res.copy(saved);
                }
                byte[] contents = this.toByteArray();
                long mtime = save(res, contents);
                res.lastmodified = mtime;
                cache.dump(res, new ByteArrayInputStream(contents));
            }
        };
    }
}
