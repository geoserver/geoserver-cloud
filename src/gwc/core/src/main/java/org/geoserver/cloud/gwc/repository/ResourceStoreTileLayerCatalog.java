/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import com.thoughtworks.xstream.XStream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.config.util.SecureXStream;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.geoserver.gwc.layer.TileLayerCatalogListener.Type;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.DimensionWarning;
import org.geowebcache.config.ContextualConfigurationProvider.Context;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.storage.blobstore.file.FilePathUtils;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.repository")
@RequiredArgsConstructor
public class ResourceStoreTileLayerCatalog implements TileLayerCatalog {

    private final @NonNull ResourceStore resourceStore;

    /**
     * Used by {@link XMLConfiguration#getConfiguredXStreamWithContext}, at {@link #initialize()},
     * to lookup implementations of {@link org.geowebcache.config.XMLConfigurationProvider}, such as
     * {@code S3BlobStoreConfigProvider}, etc. This could be improved.
     */
    private final Optional<WebApplicationContext> applicationContext;

    private final AtomicBoolean initialized = new AtomicBoolean();
    private final List<TileLayerCatalogListener> listeners = new CopyOnWriteArrayList<>();

    private Supplier<XStream> xstreamProvider;
    private XStream serializer;
    private String baseDirectory;

    @Override
    public void reset() {
        if (initialized.compareAndSet(true, false)) {
            xstreamProvider = null;
            serializer = null;
        }
    }

    @Override
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            this.baseDirectory = "gwc-layers";
            this.xstreamProvider =
                    () ->
                            XMLConfiguration.getConfiguredXStreamWithContext(
                                    new SecureXStream(),
                                    applicationContext.orElse(null),
                                    Context.PERSIST);
            this.serializer = newXStream();
        }
    }

    @Override
    public void addListener(TileLayerCatalogListener listener) {
        if (null != listener) listeners.add(listener);
    }

    @Override
    public Set<String> getLayerIds() {
        checkInitialized();
        try (Stream<GeoServerTileLayerInfo> all = findAll()) {
            return all.map(GeoServerTileLayerInfo::getId).collect(Collectors.toSet());
        }
    }

    @Override
    public Set<String> getLayerNames() {
        checkInitialized();
        try (Stream<GeoServerTileLayerInfo> all = findAll()) {
            return all.map(GeoServerTileLayerInfo::getName).collect(Collectors.toSet());
        }
    }

    @Override
    public String getLayerId(@NonNull String layerName) {
        checkInitialized();
        try (Stream<GeoServerTileLayerInfo> all = findAll()) {
            return all.filter(l -> layerName.equals(l.getName()))
                    .map(GeoServerTileLayerInfo::getId)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public String getLayerName(String layerId) {
        checkInitialized();
        try (Stream<GeoServerTileLayerInfo> all = findAll()) {
            return all.filter(l -> layerId.equals(l.getId()))
                    .map(GeoServerTileLayerInfo::getName)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public GeoServerTileLayerInfo getLayerById(@NonNull String id) {
        checkInitialized();
        return findFile(id).map(this::depersist).orElse(null);
    }

    @Override
    public GeoServerTileLayerInfo getLayerByName(String layerName) {
        checkInitialized();
        try (Stream<GeoServerTileLayerInfo> all = findAll()) {
            return all.filter(l -> layerName.equals(l.getName())).findFirst().orElse(null);
        }
    }

    @Override
    public GeoServerTileLayerInfo delete(@NonNull String tileLayerId) {
        checkInitialized();
        GeoServerTileLayerInfo info = null;
        Optional<Resource> resource = findFile(tileLayerId);
        if (!resource.isPresent()) {
            return null;
        }
        final Resource file = resource.get();
        try {
            info = depersist(file);
            if (file.delete()) {
                notify(tileLayerId, Type.DELETE);
                return info;
            }
        } catch (UncheckedIOException e) {
            log.warn("Error deleting resource {}", info, e);
        }
        return null;
    }

    @Override
    public GeoServerTileLayerInfo save(@NonNull GeoServerTileLayerInfo newValue) {
        checkInitialized();
        final String layerId = newValue.getId();
        Objects.requireNonNull(layerId);
        final GeoServerTileLayerInfo prev = getLayerById(layerId);
        persist(newValue);
        Type eventType = prev == null ? Type.CREATE : Type.MODIFY;
        notify(layerId, eventType);
        return prev;
    }

    @Override
    public boolean exists(String layerId) {
        checkInitialized();
        return findFile(layerId).isPresent();
    }

    @Override
    public String getPersistenceLocation() {
        return resourceStore.get(baseDirectory).path();
    }

    /**
     * Precondition check all public methods should make before proceeding to ensure they've been
     * called on an initialized state
     *
     * @throws IllegalStateException if this layer catalog has not been initialized yet
     */
    private void checkInitialized() {
        Preconditions.checkState(
                this.initialized.get(), "DefaultTileLayerCatalog is not initialized");
    }

    private Resource baseDirectory() {
        return resourceStore.get(baseDirectory);
    }

    private void persist(GeoServerTileLayerInfo real) {
        final String tileLayerId = real.getId();
        final Resource file = getFile(tileLayerId);
        persist(real, file.out());
    }

    private void persist(GeoServerTileLayerInfo real, OutputStream out) {
        try {
            try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                serializer.toXML(real, writer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private GeoServerTileLayerInfo depersist(final Resource res) {
        try {
            return depersist(res, this.serializer);
        } catch (RuntimeException e) {
            log.warn("Error depersisting tile layer {}, returning null", res.path(), e);
            return null;
        }
    }

    private GeoServerTileLayerInfo depersist(final Resource res, final XStream unmarshaller) {
        if (log.isDebugEnabled())
            log.debug("Depersisting GeoServerTileLayerInfo from {}", res.path());
        return depersist(res.in(), unmarshaller);
    }

    private GeoServerTileLayerInfo depersist(InputStream in, XStream unmarshaller) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return (GeoServerTileLayerInfo) unmarshaller.fromXML(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Stream<GeoServerTileLayerInfo> findAll() {
        return findAllTileLayerResources().map(this::depersist).filter(Objects::nonNull);
    }

    private Stream<Resource> findAllTileLayerResources() {
        final ResourceStore store = this.resourceStore;
        final Resource layersBase = baseDirectory();
        if (layersBase.getType() != Resource.Type.DIRECTORY) {
            return Stream.empty();
        }

        if (store instanceof FileSystemResourceStore) {
            Path basePath = layersBase.dir().toPath();
            return findAllTileLayerResources(basePath);
        }
        Predicate<Resource> xmlFilter = new Resources.ExtensionFilter("XML")::accept;
        return layersBase.list().stream().filter(xmlFilter);
    }

    private Stream<Resource> findAllTileLayerResources(Path basePath) {
        final PathMatcher matcher = basePath.getFileSystem().getPathMatcher("glob:**.xml");
        DirectoryStream.Filter<Path> filter =
                path -> {
                    boolean matches = matcher.matches(path);
                    return matches && Files.isRegularFile(path);
                };
        DirectoryStream<Path> directoryStream;
        try {
            directoryStream = Files.newDirectoryStream(basePath, filter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final Resource baseDirectory = baseDirectory();
        return Streams.stream(directoryStream)
                .onClose(() -> closeSilently(directoryStream))
                .map(Path::getFileName)
                .map(Path::toString)
                .peek(name -> log.trace("found potential tile layer file {}", name))
                .map(baseDirectory::get);
    }

    private void closeSilently(DirectoryStream<Path> directoryStream) {
        try {
            directoryStream.close();
        } catch (IOException e) {
            log.warn("Error closing directory stream for " + baseDirectory);
        }
    }

    private Optional<Resource> findFile(final String tileLayerId) {
        Resource resource = getFile(tileLayerId);
        return Optional.of(resource).filter(r -> r.getType() == Resource.Type.RESOURCE);
    }

    protected Resource getFile(final String tileLayerId) {
        final String fileName = layerIdToFileName(tileLayerId);
        return baseDirectory().get(fileName);
    }

    private String layerIdToFileName(final String tileLayerId) {
        return FilePathUtils.filteredLayerName(tileLayerId) + ".xml";
    }

    private void notify(String layerId, Type eventType) {
        listeners.forEach(l -> notify(l, layerId, eventType));
    }

    private void notify(TileLayerCatalogListener l, String layerId, Type eventType) {
        try {
            l.onEvent(layerId, eventType);
        } catch (RuntimeException e) {
            String listener = l == null ? null : l.getClass().getCanonicalName();
            log.warn(
                    "Error notifying listener of {} change event for TileLayer {}",
                    listener,
                    eventType,
                    layerId,
                    e);
        }
    }

    private XStream newXStream() {
        XStream serializer = this.xstreamProvider.get();
        serializer.allowTypeHierarchy(GeoServerTileLayerInfo.class);
        serializer.allowTypes(new Class[] {DimensionWarning.WarningType.class});
        // have to use a string here because UnmodifiableSet is private
        serializer.allowTypes(new String[] {"java.util.Collections$UnmodifiableSet"});
        serializer.addDefaultImplementation(LinkedHashSet.class, Set.class);
        serializer.alias("warning", DimensionWarning.WarningType.class);
        return serializer;
    }
}
