/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.core;

import org.geoserver.ogcapi.OGCAPIXStreamPersisterInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Teaches {@code XStreamPersister} how to read and write the {@code LinkInfo} lists stored under the
 * {@code ogcApiLinks} key of the metadata maps of layers, layer groups, resources and settings.
 *
 * <p>Without the initializer, {@code XStreamPersister} writes those metadata values as the string returned by their
 * {@code toString()} and reads them back as a {@code String}. Every service reads and writes the catalog, so this is a
 * property of the configuration subsystem rather than of any one GeoServer service: neither the running service nor
 * {@code geoserver.extension.ogcapi.features.enabled} controls it. Turning the extension off must not let a service
 * corrupt the configuration the extension wrote while it was on.
 *
 * <p>The initializer also registers {@code List} itself as a brief map complex type. That registration is only correct
 * alongside the INSPIRE initializer, which exempts {@code UniqueResourceIdentifiers} from it; both are contributed on
 * class presence alone so that no service ever sees one without the other.
 *
 * <p>{@code OgcApiCoreConfiguration} contributes the same initializer through the component scan in
 * {@code gs-ogcapi-core}'s {@code applicationContext.xml}, which the bean name filters of
 * {@code ImportFilteredResource} cannot reach. Hence {@link ConditionalOnMissingBean} and the ordering after the
 * services that import that configuration, so the services running an OGC API keep a single initializer.
 */
@AutoConfiguration(
        afterName = "org.geoserver.cloud.autoconfigure.extensions.ogcapi.features.OgcApiFeaturesAutoConfiguration")
@ConditionalOnClass(OGCAPIXStreamPersisterInitializer.class)
public class OgcApiCoreXStreamPersisterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OGCAPIXStreamPersisterInitializer.class)
    OGCAPIXStreamPersisterInitializer ogcApiXStreamPersisterInitializer() {
        return new OGCAPIXStreamPersisterInitializer();
    }
}
