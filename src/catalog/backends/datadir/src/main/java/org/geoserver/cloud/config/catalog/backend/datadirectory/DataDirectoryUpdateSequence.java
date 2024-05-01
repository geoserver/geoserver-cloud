/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

/**
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DataDirectoryUpdateSequence implements UpdateSequence, GeoServerInitializer {

    private static final String UPDATE_SEQUENCE_FILE_NAME = "updateSequence.properties";

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String CLUSTER_LOCK_NAME = "UPDATE_SEQUENCE";

    /** Provides the cluster aware {@link ResourceStore#getLockProvider LockProvider} */
    private final @NonNull ResourceStore resourceStore;

    private final @NonNull GeoServerDataDirectory dd;
    private final @NonNull XStreamPersisterFactory xpf;

    private GeoServer geoServer;
    private XStreamPersister xp;

    @Override
    public void initialize(GeoServer geoServer) throws IOException {
        this.geoServer = geoServer;
        Resource resource = getOrCreateResource();
        log.debug("Update sequence resource is {}", resource.path());
    }

    @Override
    public long currValue() {
        try {
            Resource resource = getOrCreateResource();

            Properties props = load(resource);
            return getValue(props);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Resource getOrCreateResource() throws IOException {
        Resource resource = resource();
        if (!Resources.exists(resource)) {
            org.geoserver.platform.resource.Resource.Lock clusterLock = lock();
            try {
                resource = resource();
                if (!Resources.exists(resource)) {
                    initialize(resource);
                }
            } finally {
                clusterLock.release();
            }
        }
        return resource;
    }

    @Override
    public long nextValue() {
        org.geoserver.platform.resource.Resource.Lock clusterLock = lock();
        try {
            final long newValue = computeAndSaveNewValue();
            persistGeoServerInfo(newValue);
            return newValue;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            clusterLock.release();
        }
    }

    protected long computeAndSaveNewValue() throws IOException {
        Resource resource = resource();
        if (!Resources.exists(resource)) {
            initialize(resource);
        }
        Properties props = load(resource);
        final long currentValue = getValue(props);
        final long newValue = currentValue + 1;
        save(resource, newValue);
        return newValue;
    }

    private void persistGeoServerInfo(long newValue) {
        log.debug("Saving update sequence {}", newValue);
        GeoServerInfo info = ModificationProxy.unwrap(geoServer.getGlobal());
        info.setUpdateSequence(newValue);
        Resource resource = dd.config(info);
        XStreamPersister persister = persister();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            persister.save(info, out);
            resource.setContents(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private XStreamPersister persister() {
        if (null == xp) {
            xp = xpf.createXMLPersister();
            boolean initialized = null != geoServer;
            if (initialized) {
                Catalog catalog = geoServer.getCatalog();
                xp.setCatalog(catalog);
            }
        }
        return xp;
    }

    private Long getValue(Properties props) {
        return Long.valueOf(props.getProperty("value", "0"));
    }

    /** Precondition: be called while holding the {@link #lock()} */
    private void save(Resource resource, long value) throws IOException {
        Properties props = new Properties();
        props.put("value", String.valueOf(value));
        save(resource, props);
    }

    /** Precondition: be called while holding the {@link #lock()} */
    private void save(Resource resource, Properties props) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String comments = "";
        props.store(new OutputStreamWriter(out, CHARSET), comments);
        byte[] contents = out.toByteArray();
        resource.setContents(contents);
    }

    /** Precondition: be called while holding the {@link #lock()} */
    protected Properties load(Resource resource) throws IOException {
        byte[] contents = resource.getContents();
        Properties props = new Properties();
        InputStreamReader reader =
                new InputStreamReader(new ByteArrayInputStream(contents), CHARSET);
        props.load(reader);
        return props;
    }

    /** Precondition: be called while holding the {@link #lock()} */
    private void initialize(Resource resource) throws IOException {
        Optional<GeoServerInfo> global = loadGlobalInfo();
        final long initialValue = global.map(GeoServerInfo::getUpdateSequence).orElse(0L);
        save(resource, initialValue);
    }

    private Optional<GeoServerInfo> loadGlobalInfo() throws IOException {
        GeoServerInfo geoServerInfo = null;
        if (null == geoServer) {
            Resource configResource = dd.config(new GeoServerInfoImpl());
            if (Resources.exists(configResource)) {
                byte[] contents = configResource.getContents();
                ByteArrayInputStream in = new ByteArrayInputStream(contents);
                geoServerInfo = persister().load(in, GeoServerInfo.class);
            }
        } else {
            geoServerInfo = geoServer.getGlobal();
        }
        return Optional.ofNullable(geoServerInfo);
    }

    protected Resource resource() {
        return resourceStore.get(UPDATE_SEQUENCE_FILE_NAME);
    }

    protected org.geoserver.platform.resource.Resource.Lock lock() {
        LockProvider lockProvider = resourceStore.getLockProvider();
        return lockProvider.acquire(CLUSTER_LOCK_NAME);
    }
}
