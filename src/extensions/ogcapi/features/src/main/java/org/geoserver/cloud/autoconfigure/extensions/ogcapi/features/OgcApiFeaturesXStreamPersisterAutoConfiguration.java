/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.ogcapi.v1.features.FeatureServiceXStreamPersisterInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Teaches {@code XStreamPersister} how to read and write the OGC API Features conformance objects stored under the
 * {@code ogcapiFeatures}, {@code cql2} and {@code ecql} keys of {@code WFSInfo}'s metadata map.
 *
 * <p>Without the initializer, {@code XStreamPersister} writes those metadata values as the string returned by their
 * {@code toString()} and reads them back as a {@code String}, which later fails with a {@code ClassCastException} in
 * the WFS service. Every service reads and writes {@code WFSInfo}, so this is a property of the configuration subsystem
 * rather than of the OGC API Features service: neither the running GeoServer service nor
 * {@code geoserver.extension.ogcapi.features.enabled} controls it. Turning the extension off must not let a service
 * corrupt the configuration the extension wrote while it was on.
 *
 * @see OgcApiFeaturesAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(FeatureConformance.class)
public class OgcApiFeaturesXStreamPersisterAutoConfiguration {

    @Bean
    FeatureServiceXStreamPersisterInitializer featureServiceXStreamInitializer() {
        return new FeatureServiceXStreamPersisterInitializer();
    }
}
