/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @since 1.4
 */
@EqualsAndHashCode(exclude = {"store"})
class PgconfigResource implements Resource {
    @Getter long id;
    @Getter long parentId;
    Resource.Type type;
    String path;
    long lastmodified;
    private PgconfigResourceStore store;

    PgconfigResource(
            @NonNull PgconfigResourceStore store,
            long id,
            long parentId,
            @NonNull Resource.Type type,
            @NonNull String path,
            long lastmodified) {
        this.store = store;
        this.id = id;
        this.parentId = parentId;
        this.type = type;
        this.path = path;
        this.lastmodified = lastmodified;
    }

    /** Undefined type factory method */
    static PgconfigResource undefined(@NonNull PgconfigResourceStore store, @NonNull String path) {
        return new PgconfigResource(
                store,
                PgconfigResourceStore.UNDEFINED_ID,
                PgconfigResourceStore.UNDEFINED_ID,
                Type.UNDEFINED,
                path,
                0L);
    }

    void copy(PgconfigResource other) {
        this.id = other.id;
        this.parentId = other.parentId;
        this.type = other.type;
        this.path = other.path;
        this.lastmodified = other.lastmodified;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String name() {
        return Paths.name(path);
    }

    @Override
    public InputStream in() {
        byte[] contents = store.contents(this);
        return new ByteArrayInputStream(contents);
    }

    @Override
    public OutputStream out() {
        return store.out(this);
    }

    @Override
    public Lock lock() {
        return store.getLockProvider().acquire(path());
    }

    @Override
    public File file() {
        return store.asFile(this);
    }

    @Override
    public File dir() {
        return store.asDir(this);
    }

    @Override
    public long lastmodified() {
        return lastmodified;
    }

    @Override
    public PgconfigResource parent() {
        return store.getParent(this);
    }

    @Override
    public Resource get(@NonNull String childPath) {
        if ("".equals(childPath)) {
            return this;
        }
        String resourcePath = Paths.path(path(), childPath);
        return store.get(resourcePath);
    }

    @Override
    public List<Resource> list() {
        return store.list(this);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean delete() {
        return store.delete(this);
    }

    @Override
    public boolean renameTo(@NonNull Resource dest) {
        return store.move(this, ((PgconfigResource) dest));
    }

    @Override
    public void addListener(ResourceListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeListener(ResourceListener listener) {
        // TODO Auto-generated method stub
    }

    public String parentPath() {
        return Paths.parent(path());
    }

    @Override
    public String toString() {
        return path;
    }

    public PgconfigResource mkdirs() {
        return store.mkdirs(this);
    }

    public boolean exists() {
        return id != PgconfigResourceStore.UNDEFINED_ID;
    }

    public boolean isFile() {
        return getType() == Type.RESOURCE;
    }

    public boolean isDirectory() {
        return getType() == Type.DIRECTORY;
    }

    public boolean isUndefined() {
        return getType() == Type.UNDEFINED;
    }
}
