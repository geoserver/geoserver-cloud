/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import com.google.common.io.ByteStreams;
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
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient.ResourceDescriptor;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;

/** */
@AllArgsConstructor
class CatalogClientResource implements Resource {

    private @NonNull ReactiveResourceStoreClient.ResourceDescriptor descriptor;
    private @NonNull CatalogClientResourceStore store;

    public @Override String path() {
        return descriptor.getPath();
    }

    public @Override void addListener(ResourceListener listener) {
        store.getResourceNotificationDispatcher().addListener(path(), listener);
    }

    public @Override void removeListener(ResourceListener listener) {
        store.getResourceNotificationDispatcher().removeListener(path(), listener);
    }

    public @Override String name() {
        return Paths.get(path()).getFileName().toString();
    }

    public @Override Lock lock() {
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

    public @Override byte[] getContents() throws IOException {
        org.springframework.core.io.Resource contents = store.getFileContent(path());
        return toByteArray(contents);
    }

    public @Override void setContents(byte[] contents) throws IOException {
        store.put(path(), ByteBuffer.wrap(contents));
    }

    public @Override InputStream in() {
        try {
            return new ByteArrayInputStream(getContents());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public @Override OutputStream out() {
        return new ByteArrayOutputStream() {
            public @Override void close() throws IOException {
                ByteBuffer buff = ByteBuffer.wrap(super.buf, 0, super.count);
                store.put(path(), buff);
            }
        };
    }

    public @Override File file() {
        return store.file(this);
    }

    public @Override File dir() {
        return store.dir(this);
    }

    public @Override long lastmodified() {
        return descriptor.getLastModified();
    }

    public @Override Resource parent() {
        Path parentPath = Paths.get(path()).getParent();
        if (null == parentPath) {
            parentPath = Paths.get(org.geoserver.platform.resource.Paths.BASE); // root
        }
        return store.get(parentPath.toString());
    }

    public @Override Resource get(final String childName) {
        Objects.requireNonNull(childName, "Resource path required");
        if ("".equals(childName)) {
            return this;
        }
        Path path = Paths.get(path());
        Path child = path.resolve(childName);
        return store.get(child.toString());
    }

    public @Override List<Resource> list() {
        return store.list(path()).collect(Collectors.toList());
    }

    public @Override Type getType() {
        return descriptor.getType();
    }

    public @Override boolean delete() {
        boolean removed = store.remove(path());
        if (removed) {
            this.descriptor.setType(Type.UNDEFINED);
        }
        return removed;
    }

    public @Override boolean renameTo(Resource dest) {
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

    public @Override String toString() {
        return String.format(
                "%s[path: %s, type: %s, lastModified: %s, lockProvider: %s]",
                getClass().getSimpleName(),
                descriptor.getPath(),
                descriptor.getType(),
                descriptor.getLastModified(),
                store.getLockProvider().getClass().getSimpleName());
    }

    public @Override boolean isDirectory() {
        return getType() == Type.DIRECTORY;
    }

    public @Override boolean isFile() {
        return getType() == Type.RESOURCE;
    }

    public @Override boolean exists() {
        return getType() != Type.UNDEFINED;
    }
}
