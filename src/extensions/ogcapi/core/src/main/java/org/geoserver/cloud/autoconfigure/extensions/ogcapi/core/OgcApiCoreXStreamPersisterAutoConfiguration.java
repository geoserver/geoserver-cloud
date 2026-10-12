/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.core;

import org.geoserver.ogcapi.FilterConformanceXStreamPersisterInitializer;
import org.geoserver.ogcapi.OGCAPIXStreamPersisterInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Teaches {@code XStreamPersister} how to read and write the OGC API objects stored in metadata maps:
 *
 * <ul>
 *   <li>the {@code LinkInfo} lists stored under the {@code ogcApiLinks} key of the metadata maps of layers, layer
 *       groups, resources and settings;
 *   <li>the filter conformance objects stored under the {@code cql2} and {@code ecql} keys of the metadata map of a
 *       service.
 * </ul>
 *
 * <p>Without the initializers, {@code XStreamPersister} writes those metadata values as the string returned by their
 * {@code toString()} and reads them back as a {@code String}. Every service reads and writes the catalog, so this is a
 * property of the configuration subsystem rather than of any one GeoServer service: neither the running service nor
 * {@code geoserver.extension.ogcapi.features.enabled} controls it. Turning the extension off must not let a service
 * corrupt the configuration the extension wrote while it was on.
 *
 * <p>{@code OGCAPIXStreamPersisterInitializer} also registers {@code List} itself as a brief map complex type. That
 * registration is only correct alongside the INSPIRE initializer, which exempts {@code UniqueResourceIdentifiers} from
 * it; both are contributed on class presence alone so that no service ever sees one without the other.
 */
@AutoConfiguration
@ConditionalOnClass(OGCAPIXStreamPersisterInitializer.class)
public class OgcApiCoreXStreamPersisterAutoConfiguration {

    @Bean
    OGCAPIXStreamPersisterInitializer ogcApiXStreamPersisterInitializer() {
        return new OGCAPIXStreamPersisterInitializer();
    }

    @Bean
    FilterConformanceXStreamPersisterInitializer filterConformanceXStreamInitializer() {
        return new FilterConformanceXStreamPersisterInitializer();
    }
}
