/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.ServicePersister;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.DefaultUpdateSequence;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.util.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;

@EnableAutoConfiguration
@SpringBootConfiguration
public class TestConfigurationAutoConfiguration implements InitializingBean, ApplicationListener<ContextClosedEvent> {

    private File tmpDir;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.tmpDir = java.nio.file.Files.createTempDirectory("gs").toFile();
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            IOUtils.delete(tmpDir, true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Bean
    GeoServerResourceLoader geoServerResourceLoader() {
        return new GeoServerResourceLoader(tmpDir);
    }

    @Bean
    UpdateSequence testUpdateSequence(GeoServer gs) {
        return new DefaultUpdateSequence(gs);
    }

    @Bean
    XStreamPersisterFactory xStreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    @Bean(name = {"catalog", "rawCatalog"})
    Catalog catalog() {
        final boolean isolated = false;
        return new CatalogPlugin(isolated);
    }

    @Bean
    GeoServer geoServer(@Qualifier("catalog") Catalog catalog) {
        GeoServerImpl gs = new org.geoserver.config.plugin.GeoServerImpl();
        gs.setCatalog(catalog);
        return gs;
    }

    @Bean
    GeoServerExtensions geoserverExtensions() {
        return new GeoServerExtensions();
    }

    @Bean
    GeoServerLoader geoserverLoader(
            @Qualifier("geoServer") GeoServer geoServer,
            @Qualifier("geoServerResourceLoader") GeoServerResourceLoader geoServerResourceLoader) {
        DefaultGeoServerLoader loader = new DefaultGeoServerLoader(geoServerResourceLoader);
        loader.postProcessBeforeInitialization(geoServer, "geoserver");

        removeListener(geoServer, ServicePersister.class);
        removeListener(geoServer, GeoServerConfigPersister.class);

        return loader;
    }

    private void removeListener(GeoServer geoServer, Class<? extends ConfigurationListener> type) {
        Optional<ConfigurationListener> listener =
                geoServer.getListeners().stream().filter(type::isInstance).findFirst();

        if (listener.isPresent()) {
            geoServer.removeListener(listener.orElseThrow());
        }
    }
}
