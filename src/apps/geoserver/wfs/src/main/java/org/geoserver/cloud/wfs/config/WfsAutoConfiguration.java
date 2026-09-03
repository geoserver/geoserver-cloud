/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wfs.config;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.core.GeoServerWebMvcMainAutoConfiguration;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Contributes the WFS service beans from {@code gs-wfs}'s {@code applicationContext.xml}.
 * <p>
 * {@code wfsXStreamPersisterInitializer} is filtered out of the {@code gs-wfs}
 * import: every service reads and writes the cascaded stored query
 * configuration it describes, so it is contributed unconditionally by
 * {@code WfsXStreamPersisterAutoConfiguration} instead of by the services
 * that happen to run a WFS.
 */
@AutoConfiguration(after = GeoServerWebMvcMainAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ImportFilteredResource({"jar:gs-wfs-.*!/applicationContext.xml#name=^(?!wfsXStreamPersisterInitializer).*$"})
public class WfsAutoConfiguration {

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }
}
