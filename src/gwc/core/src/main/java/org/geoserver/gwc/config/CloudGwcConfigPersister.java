/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.config;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.gwc.event.ConfigChangeEvent;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.util.DimensionWarning;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.springframework.context.event.EventListener;

/**
 * extends {@link GWCConfigPersister} to send {@link ConfigChangeEvent}s upon {@link
 * #save(org.geoserver.gwc.config.GWCConfig)}
 */
@Slf4j
public class CloudGwcConfigPersister extends GWCConfigPersister {

    private Consumer<ConfigChangeEvent> eventPublisher;
    private XStreamPersisterFactory xspf;
    private GWCConfig configuration;

    public CloudGwcConfigPersister(
            XStreamPersisterFactory xspf,
            GeoServerResourceLoader resourceLoader,
            Consumer<ConfigChangeEvent> eventPublisher) {
        super(xspf, resourceLoader);
        this.xspf = xspf;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Override to get a hold to the config and be able of re-loading it upon {@link
     * ConfigChangeEvent}, since super.config is private
     */
    @Override
    public GWCConfig getConfig() {
        if (null == configuration) {
            configuration = super.getConfig();
        }
        return configuration;
    }

    /**
     * Override to publish a {@link ConfigChangeEvent}
     *
     * @see #reloadOnRemoteConfigChangeEvent(ConfigChangeEvent)
     */
    @Override
    public void save(final GWCConfig config) throws IOException {
        super.save(config);
        this.configuration = config;
        eventPublisher.accept(new ConfigChangeEvent(this));
    }

    @EventListener(ConfigChangeEvent.class)
    public void reloadOnRemoteConfigChangeEvent(ConfigChangeEvent event) throws IOException {
        final boolean isRemote = event.getSource() != this;
        if (isRemote) {
            log.info("Reloading gwc configuration upon remote config change event");
            GWCConfig config = reload();
            this.configuration = config;

            // Update ConfigurableBlobStore
            ConfigurableBlobStore blobstore = GeoServerExtensions.bean(ConfigurableBlobStore.class);
            if (blobstore != null) {
                blobstore.setChanged(config, false);
            }
        }
    }

    // super.loadConfig() is private
    private synchronized GWCConfig reload() throws IOException {
        Resource configFile = findConfigFile();
        if (configFile == null || configFile.getType() == Type.UNDEFINED) {
            throw new IllegalStateException("gwc config resource does not exist: %s".formatted(GWC_CONFIG_FILE));
        }

        XStreamPersister xmlPersister = this.xspf.createXMLPersister();
        configure(xmlPersister.getXStream());
        try (InputStream in = configFile.in()) {
            return xmlPersister.load(in, GWCConfig.class);
        }
    }

    // super.configureXstream() is private
    private void configure(XStream xs) {
        xs.alias("GeoServerGWCConfig", GWCConfig.class);
        xs.alias("defaultCachingGridSetIds", HashSet.class);
        xs.alias("defaultCoverageCacheFormats", HashSet.class);
        xs.alias("defaultVectorCacheFormats", HashSet.class);
        xs.alias("defaultOtherCacheFormats", HashSet.class);
        xs.alias("InnerCacheConfiguration", CacheConfiguration.class);
        xs.alias("warning", DimensionWarning.WarningType.class);
        xs.allowTypes(new Class[] {GWCConfig.class, CacheConfiguration.class, DimensionWarning.WarningType.class});
        xs.addDefaultImplementation(LinkedHashSet.class, Set.class);
    }
}
