/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.catalog.client.reactivefeign.BlockingResourceStoreClient;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient.ResourceDescriptor;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.MemoryLockProvider;
import org.geoserver.platform.resource.NullLockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Lock;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.util.Assert;

/** */
@Slf4j
public class CatalogClientResourceStore implements ResourceStore {
    private static final NullLockProvider NULL_LOCK_PROVIDER = new NullLockProvider();

    /** LockProvider used to secure resources for exclusive access */
    private @Getter @Setter @NonNull LockProvider lockProvider = NULL_LOCK_PROVIDER;

    private final BlockingResourceStoreClient remoteStore;
    private FileSystemResourceStore localStore;

    /**
     * No-op notification dispatcher, resources are live-through-the-wire. May end up needing a
     * notification mechanism, but until proved differently, there seems to be no need for it.
     */
    private ResourceNotificationDispatcher resourceNotificationDispatcher =
            NullResourceNotificationDispatcher.INSTANCE;

    public CatalogClientResourceStore(@NonNull BlockingResourceStoreClient client) {
        this.remoteStore = client;
        Path localCache = getOrCreateDefaultLocalCacheDirectory();
        setLocalCacheDirectory(localCache.toFile());
    }

    protected Path getOrCreateDefaultLocalCacheDirectory() {
        Path tmpdir = Path.of(System.getProperty("java.io.tmpdir"));
        Path localCache = tmpdir.resolve("cngs/catalog-service/resource_store");
        if (!Files.isDirectory(localCache)) {
            try {
                Files.createDirectories(localCache);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Unable to create catalog-service local resource cache directory "
                                + localCache.toString(),
                        e);
            }
        }
        return localCache;
    }

    public CatalogClientResourceStore(
            @NonNull BlockingResourceStoreClient client, @NonNull File localCache) {
        this.remoteStore = client;
        setLocalCacheDirectory(localCache);
    }

    public void setLocalCacheDirectory(File localCache) {
        this.localStore = createLocalStore(localCache);
    }

    private FileSystemResourceStore createLocalStore(@NonNull File localCache) {
        FileSystemResourceStore local =
                new FileSystemResourceStore(localCache) {
                    public @Override ResourceNotificationDispatcher
                            getResourceNotificationDispatcher() {
                        return NullResourceNotificationDispatcher.INSTANCE;
                    }
                };
        local.setLockProvider(new MemoryLockProvider(Runtime.getRuntime().availableProcessors()));
        return local;
    }

    private Map<String, ResourceDescriptor> dumbCache = new ConcurrentHashMap<>();

    public @Override CatalogClientResource get(String path) {
        try {
            // ResourceDescriptor descriptor = remoteStore.describe(path);
            ResourceDescriptor descriptor = dumbCache.computeIfAbsent(path, remoteStore::describe);
            return toResource(descriptor);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public @Override boolean remove(String path) {
        boolean deleted = remoteStore.delete(path);
        if (deleted) {
            localStore.get(path).delete();
        }
        return deleted;
    }

    public @Override boolean move(String path, String target) {
        ResourceDescriptor moved = remoteStore.move(path, target).orElse(null);
        localStore.move(path, target);
        return moved != null && target.equals(moved.getPath());
    }

    public @Override ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return resourceNotificationDispatcher;
    }

    private static class NullResourceNotificationDispatcher
            implements ResourceNotificationDispatcher {

        static final NullResourceNotificationDispatcher INSTANCE =
                new NullResourceNotificationDispatcher();

        public @Override void addListener(String resource, ResourceListener listener) {}

        public @Override boolean removeListener(String resource, ResourceListener listener) {
            return true;
        }

        public @Override void changed(ResourceNotification notification) {}
    }

    org.springframework.core.io.Resource getFileContent(String path) throws IOException {
        return remoteStore.getFileContent(path);
    }

    void put(String path, ByteBuffer contents) {
        remoteStore.put(path, contents);
    }

    Stream<CatalogClientResource> list(String path) {
        return remoteStore.list(path).map(this::toResource);
    }

    CatalogClientResource toResource(ResourceDescriptor descriptor) {
        return new CatalogClientResource(descriptor, this);
    }

    /**
     * @param catalogServiceResource
     * @return
     */
    File file(@NonNull CatalogClientResource resource) {
        final Resource local = localStore.get(resource.path());
        final CatalogClientResource remote = get(resource.path());

        if (remote.isDirectory()) {
            throw new IllegalStateException(remote.path() + " is a directory");
        }

        boolean localIsFile = Type.RESOURCE.equals(local.getType());
        boolean localAndRemoteUpToDate = resource.lastmodified() == local.lastmodified();

        if (localIsFile && localAndRemoteUpToDate) {
            return local.file();
        }
        Lock lock = resource.lock();
        try {
            return updateLocalFile(local, remote);
        } finally {
            lock.release();
        }
    }

    File dir(@NonNull CatalogClientResource resource) {
        final Resource local = localStore.get(resource.path());
        final CatalogClientResource remote = get(resource.path());
        if (!remote.exists()) {
            ResourceDescriptor descriptor = resource.getDescriptor();
            descriptor.setType(Type.DIRECTORY);
            ResourceDescriptor created = remoteStore.create(resource.path(), descriptor);
            resource = toResource(created);
        }
        if (remote.isFile()) {
            throw new IllegalStateException(remote.path() + " is a file, not a directory");
        }
        Lock lock = local.lock();
        try {
            boolean localIsDirectory = Type.DIRECTORY.equals(local.getType());
            if (localIsDirectory && !local.delete()) {
                throw new IllegalStateException(
                        "Unable to delte local copy of directory " + resource.path());
            }
            File localDirectory = local.dir();
            boolean localAndRemoteUpToDate = resource.lastmodified() == local.lastmodified();
            if (!localAndRemoteUpToDate) {
                localDirectory.setLastModified(remote.lastmodified());
            }
            return localDirectory;
        } finally {
            lock.release();
        }
    }

    private File updateLocalFile(Resource local, CatalogClientResource remote) {
        boolean localExists = local.getType() != Type.UNDEFINED;
        if (localExists && !local.delete()) {
            throw new IllegalStateException(
                    "Unable to delete local copy of resource at " + local.path());
        }
        File file = local.file(); // forces creation if doesn't exist
        if (remote.exists()) {
            final long lastModified = remote.lastmodified();
            Assert.state(
                    lastModified > 0L,
                    "remote resource claims to exist but last-modified is not set");
            log.debug(
                    "Updating remote resource {} to local cache: {}",
                    remote.path(),
                    file.getAbsolutePath());
            try (InputStream in = remote.in();
                    OutputStream out = local.out()) {
                ByteStreams.copy(in, out);
                log.debug("Local cache updated: {}", file.getAbsolutePath());
            } catch (IOException | RuntimeException e) {
                log.error(
                        "Error trying to copy remote resource {} to local cache {}",
                        remote.path(),
                        file.getAbsolutePath(),
                        e);
                local.delete();
                throw e instanceof IOException
                        ? new UncheckedIOException((IOException) e)
                        : (RuntimeException) e;
            }
            file.setLastModified(remote.lastmodified());
            long localLastModified = file.lastModified();
            Assert.state(
                    lastModified == localLastModified,
                    String.format(
                            "last-modified time mismatch, expected %d, was %d",
                            lastModified, localLastModified));
        }

        return file;
    }
}
