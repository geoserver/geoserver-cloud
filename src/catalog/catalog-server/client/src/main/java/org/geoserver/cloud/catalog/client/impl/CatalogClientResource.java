/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import com.google.common.io.ByteStreams;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient.ResourceDescriptor;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** */
@AllArgsConstructor
class CatalogClientResource implements Resource {

    private @NonNull ReactiveResourceStoreClient.ResourceDescriptor descriptor;
    private @NonNull CatalogClientResourceStore store;

    @Override
    public String path() {
        return descriptor.getPath();
    }

    @Override
    public void addListener(ResourceListener listener) {
        store.getResourceNotificationDispatcher().addListener(path(), listener);
    }

    @Override
    public void removeListener(ResourceListener listener) {
        store.getResourceNotificationDispatcher().removeListener(path(), listener);
    }

    @Override
    public String name() {
        return Paths.get(path()).getFileName().toString();
    }

    @Override
    public Lock lock() {
        return store.getLockProvider().acquire(path());
    }

    private byte[] toByteArray(org.springframework.core.io.Resource buff) {
        try (InputStream in = buff.getInputStream()) {
            byte[] array = ByteStreams.toByteArray(in);
            return array;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] getContents() throws IOException {
        org.springframework.core.io.Resource contents = store.getFileContent(path());
        return toByteArray(contents);
    }

    @Override
    public void setContents(byte[] contents) throws IOException {
        store.put(path(), ByteBuffer.wrap(contents));
    }

    @Override
    public InputStream in() {
        try {
            return new ByteArrayInputStream(getContents());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public OutputStream out() {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                ByteBuffer buff = ByteBuffer.wrap(super.buf, 0, super.count);
                store.put(path(), buff);
            }
        };
    }

    @Override
    public File file() {
        return store.file(this);
    }

    @Override
    public File dir() {
        return store.dir(this);
    }

    @Override
    public long lastmodified() {
        return descriptor.getLastModified();
    }

    @Override
    public Resource parent() {
        Path parentPath = Paths.get(path()).getParent();
        if (null == parentPath) {
            parentPath = Paths.get(org.geoserver.platform.resource.Paths.BASE); // root
        }
        return store.get(parentPath.toString());
    }

    @Override
    public Resource get(final String childName) {
        Objects.requireNonNull(childName, "Resource path required");
        if ("".equals(childName)) {
            return this;
        }
        Path path = Paths.get(path());
        Path child = path.resolve(childName);
        return store.get(child.toString());
    }

    @Override
    public List<Resource> list() {
        return store.list(path()).collect(Collectors.toList());
    }

    @Override
    public Type getType() {
        return descriptor.getType();
    }

    @Override
    public boolean delete() {
        boolean removed = store.remove(path());
        if (removed) {
            this.descriptor.setType(Type.UNDEFINED);
        }
        return removed;
    }

    @Override
    public boolean renameTo(Resource dest) {
        boolean moved = store.move(path(), dest.path());
        if (moved) {
            this.descriptor = store.get(dest.path()).getDescriptor();
            return true;
        }
        return moved;
    }

    @NonNull
    ResourceDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[path: %s, type: %s, lastModified: %s, lockProvider: %s]",
                getClass().getSimpleName(),
                descriptor.getPath(),
                descriptor.getType(),
                descriptor.getLastModified(),
                store.getLockProvider().getClass().getSimpleName());
    }

    boolean isDirectory() {
        return getType() == Type.DIRECTORY;
    }

    boolean isFile() {
        return getType() == Type.RESOURCE;
    }

    boolean exists() {
        return getType() != Type.UNDEFINED;
    }
}
