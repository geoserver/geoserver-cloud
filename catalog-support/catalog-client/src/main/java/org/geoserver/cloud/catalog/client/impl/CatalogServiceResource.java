/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;

/** */
@AllArgsConstructor
public class CatalogServiceResource implements Resource {

    private @NonNull ReactiveResourceStoreClient client;
    private @NonNull Path path;
    private @NonNull LockProvider lockProvider;

    public @Override String path() {
        return path.toString();
    }

    public @Override String name() {
        return path.getFileName().toString();
    }

    public @Override Lock lock() {
        return lockProvider.acquire(path());
    }

    private byte[] toByteArray(ByteBuffer buff) {
        byte[] array = new byte[buff.limit()];
        buff.get(array);
        return array;
    }

    public @Override byte[] getContents() throws IOException {
        Optional<ByteBuffer> contents = client.get(path.toString()).blockOptional();
        return contents.map(this::toByteArray).orElseThrow(() -> new FileNotFoundException());
    }

    public @Override void setContents(byte[] contents) throws IOException {
        client.put(path.toString(), ByteBuffer.wrap(contents)).block();
    }

    public @Override void addListener(ResourceListener listener) {
        throw new UnsupportedOperationException("implement");
    }

    public @Override void removeListener(ResourceListener listener) {
        throw new UnsupportedOperationException("implement");
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
                client.put(path.toString(), buff);
            }
        };
    }

    public @Override File file() {
        throw new UnsupportedOperationException("implement");
    }

    public @Override File dir() {
        throw new UnsupportedOperationException("implement");
    }

    public @Override long lastmodified() {
        return client.lastModified(path.toString()).block();
    }

    public @Override Resource parent() {
        Path parent = path.getParent();
        if (null == parent) {
            parent = Paths.get(org.geoserver.platform.resource.Paths.BASE); // root
        }
        return new CatalogServiceResource(client, parent, lockProvider);
    }

    public @Override Resource get(String childPath) {
        if (childPath == null) {
            throw new NullPointerException("Resource path required");
        }
        if ("".equals(childPath)) {
            return this;
        }
        Path childAbsolutePath = path.resolve(childPath);
        return new CatalogServiceResource(client, childAbsolutePath, lockProvider);
    }

    public @Override List<Resource> list() {
        return client.list(path.toString()).toStream().collect(Collectors.toList());
    }

    public @Override Type getType() {
        return client.getType(path.toString()).block();
    }

    public @Override boolean delete() {
        return client.delete(path.toString()).block();
    }

    public @Override boolean renameTo(Resource dest) {
        String target = dest.path();
        Boolean renamed = client.move(path.toString(), target).block();
        if (renamed) {
            this.path = Paths.get(target);
        }
        return renamed;
    }

    private static class RemoteFile extends File {
        private static final long serialVersionUID = 1L;
        private CatalogServiceResource resource;

        public RemoteFile(CatalogServiceResource resource) {
            super(resource.path());
            this.resource = resource;
        }
    }
}
