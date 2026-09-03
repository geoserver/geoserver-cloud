/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.wfs;

import org.geoserver.wfs.WFSXStreamPersisterInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Teaches {@code XStreamPersister} how to read and write the {@code StoredQueryConfiguration} stored under the
 * {@code storedQueryConfiguration} key of a cascaded WFS feature type's metadata map.
 *
 * <p>Without the initializer, {@code XStreamPersister} writes that metadata value as the string returned by its
 * {@code toString()} and reads it back as a {@code String}, silently losing the stored query the layer cascades to.
 * Every service reads and writes the catalog, so this is a property of the configuration subsystem rather than of the
 * WFS service, and {@code geoserver.service.wfs.enabled} must not control it.
 *
 * <p>This module hosts the bean because {@code WFSXStreamPersisterInitializer} belongs to core GeoServer rather than to
 * an extension, and this is the module every service already has on its classpath with {@code gs-wfs-core} declared
 * optional.
 */
@AutoConfiguration
@ConditionalOnClass(WFSXStreamPersisterInitializer.class)
public class WfsXStreamPersisterAutoConfiguration {

    @Bean
    WFSXStreamPersisterInitializer wfsXStreamPersisterInitializer() {
        return new WFSXStreamPersisterInitializer();
    }
}
