/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wfs;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the WFS administration pages and the WFS service beans they read.
 * <p>
 * {@code wfsXStreamPersisterInitializer} is filtered out of the {@code gs-wfs}
 * import: every service reads and writes the cascaded stored query
 * configuration it describes, so it is contributed unconditionally by
 * {@code WfsXStreamPersisterAutoConfiguration} instead of by the services
 * that happen to run a WFS.
 */
@Configuration(proxyBeanMethods = true)
@ImportFilteredResource({
    "jar:gs-web-wfs-.*!/applicationContext.xml",
    "jar:gs-wfs-.*!/applicationContext.xml#name=^(?!wfsXStreamPersisterInitializer).*$",
    "jar:gs-dxf-core-.*!/applicationContext.xml#name=.*"
    // FlatGeobuf moved to output-formats/flatgeobuf extension
})
public class WfsConfiguration {}
