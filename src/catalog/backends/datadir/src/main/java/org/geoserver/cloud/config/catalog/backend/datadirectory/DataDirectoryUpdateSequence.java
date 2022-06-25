/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

/**
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DataDirectoryUpdateSequence implements UpdateSequence {

    private static final String UPDATE_SEQUENCE_FILE_NAME = "updateSequence.properties";

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String CLUSTER_LOCK_NAME = "UPDATE_SEQUENCE";

    private @Autowired @Qualifier("resourceStoreImpl") ResourceStore resourceStore;
    private @Autowired @Qualifier("geoServer") GeoServer geoServer;
    private @Autowired GeoServerDataDirectory dd;
    private @Autowired XStreamPersisterFactory xpf;

    private XStreamPersister xp;

    public @Override long currValue() {
        try {
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

            Properties props = load(resource);
            final long currentValue = getValue(props);
            return currentValue;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public @Override long nextValue() {
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
        XStreamPersister xp = persister();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            xp.save(info, out);
            resource.setContents(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private XStreamPersister persister() {
        if (null == xp) {
            xp = xpf.createXMLPersister();
            Catalog catalog = geoServer.getCatalog();
            xp.setCatalog(catalog);
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
        String comments = """
                """;
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
        final long initialValue =
                Optional.ofNullable(geoServer.getGlobal())
                        .map(GeoServerInfo::getUpdateSequence)
                        .orElse(0L);
        save(resource, initialValue);
    }

    protected Resource resource() throws IOException {
        return resourceStore.get(UPDATE_SEQUENCE_FILE_NAME);
    }

    protected org.geoserver.platform.resource.Resource.Lock lock() {
        LockProvider lockProvider = resourceStore.getLockProvider();
        org.geoserver.platform.resource.Resource.Lock lock =
                lockProvider.acquire(CLUSTER_LOCK_NAME);
        return lock;
    }
}
