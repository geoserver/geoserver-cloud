/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import org.geoserver.inspire.InspireXStreamPersisterInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Keeps {@code XStreamPersister} from writing the INSPIRE spatial dataset identifiers as a generic list.
 *
 * <p>{@code UniqueResourceIdentifiers} extends {@code ArrayList}, and the OGC API core initializer registers
 * {@code List} itself as a brief map complex type. This initializer exempts the INSPIRE type from that registration, so
 * the two are only correct as a pair: both are contributed on class presence alone, so that no service ever sees one
 * without the other.
 *
 * <p>The INSPIRE metadata entries are read and written by every service, so neither the running GeoServer service nor
 * {@code geoserver.extension.inspire.enabled} controls this bean. Turning the extension off must not let a service
 * corrupt the configuration the extension wrote while it was on.
 *
 * @see InspireAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(InspireXStreamPersisterInitializer.class)
public class InspireXStreamPersisterAutoConfiguration {

    @Bean
    InspireXStreamPersisterInitializer inspireXStreamConfigurer() {
        return new InspireXStreamPersisterInitializer();
    }
}
