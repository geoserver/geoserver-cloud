/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.geoserver.cloud.gwc.event.ConfigChangeEvent;
import org.geoserver.cloud.gwc.event.EventListenerContext;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;

class CloudGwcConfigPersisterTest {

    private @TempDir Path dataDirectory;

    // a listener of the same event, registered before the persister, finds the change applied
    @Test
    void remoteConfigAppliedFirst() throws Exception {
        CloudGwcConfigPersister persister = newPersister();
        persister.save(new GWCConfig());
        CloudGwcConfigPersister otherInstance = newPersister();
        otherInstance.save(metaTiling(7));
        MetaTilingReader reader = new MetaTilingReader(persister);

        try (GenericApplicationContext context = EventListenerContext.of(reader, persister)) {
            context.publishEvent(new ConfigChangeEvent(otherInstance));
        }

        assertThat(reader.metaTilingX).isEqualTo(7);
    }

    /** A persister of the GWC configuration in the data directory of the test, as each instance has one. */
    private CloudGwcConfigPersister newPersister() {
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(dataDirectory.toFile());
        return new CloudGwcConfigPersister(new XStreamPersisterFactory(), resourceLoader, event -> {});
    }

    private static GWCConfig metaTiling(int metaTilingX) {
        GWCConfig config = new GWCConfig();
        config.setMetaTilingX(metaTilingX);
        return config;
    }

    /** Reads the meta tiling of the GWC defaults when it receives a configuration event. */
    static final class MetaTilingReader {

        private final GWCConfigPersister persister;

        private int metaTilingX;

        MetaTilingReader(GWCConfigPersister persister) {
            this.persister = persister;
        }

        @EventListener(ConfigChangeEvent.class)
        public void onConfigChange() {
            metaTilingX = persister.getConfig().getMetaTilingX();
        }
    }
}
