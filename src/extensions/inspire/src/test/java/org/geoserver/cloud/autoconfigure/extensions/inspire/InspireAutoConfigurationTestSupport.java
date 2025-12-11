/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import java.io.File;
import lombok.experimental.UtilityClass;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@UtilityClass
public class InspireAutoConfigurationTestSupport {

    public static ApplicationContextRunner createContextRunner(File tempDir) {
        ResourceStore resourceStore = new FileSystemResourceStore(tempDir);
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(resourceLoader);

        return new ApplicationContextRunner()
                .withBean(ResourceStore.class, () -> resourceStore)
                .withBean(GeoServerResourceLoader.class, () -> resourceLoader)
                .withBean("dataDirectory", GeoServerDataDirectory.class, () -> datadir)
                .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
                .withConfiguration(AutoConfigurations.of(InspireAutoConfiguration.class));
    }
}
