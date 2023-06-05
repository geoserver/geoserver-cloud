/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.DefaultUpdateSequence;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnMissingBean(UpdateSequence.class)
public class DefaultUpdateSequenceAutoConfiguration {
    @Bean
    UpdateSequence defaultUpdateSequence(GeoServer gs) {
        return new DefaultUpdateSequence(gs);
    }
}
