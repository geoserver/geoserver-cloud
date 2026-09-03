/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wps;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the WPS service beans and the WCS and WFS beans WPS processes build on.
 * <p>
 * {@code wfsXStreamPersisterInitializer} is filtered out of the {@code gs-wfs}
 * import: every service reads and writes the cascaded stored query
 * configuration it describes, so it is contributed unconditionally by
 * {@code WfsXStreamPersisterAutoConfiguration} instead of by the services
 * that happen to run a WFS.
 */
@Configuration
@ImportFilteredResource({
    "jar:gs-wps-.*!/applicationContext.xml",
    "jar:gs-wcs-.*!/applicationContext.xml",
    "jar:gs-wcs1_0-.*!/applicationContext.xml",
    "jar:gs-wcs1_1-.*!/applicationContext.xml",
    "jar:gs-wcs2_0-.*!/applicationContext.xml",
    "jar:gs-wfs-.*!/applicationContext.xml#name=^(?!wfsInsertElementHandler|wfsUpdateElementHandler|wfsDeleteElementHandler|wfsReplaceElementHandler|wfsXStreamPersisterInitializer).*$",
    "jar:gs-dxf-core-.*!/applicationContext.xml#name=.*",
    "jar:gs-dxf-wps-.*!/applicationContext.xml#name=.*"
})
public class WpsApplicationConfiguration {

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }
}
