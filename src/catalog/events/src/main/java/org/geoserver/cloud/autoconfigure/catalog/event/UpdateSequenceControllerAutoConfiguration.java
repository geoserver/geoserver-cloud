/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.event;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

/**
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class UpdateSequenceControllerAutoConfiguration {

    @Bean
    UpdateSequenceController updateSequenceController( //
            UpdateSequence updateSequence, //
            ApplicationEventPublisher eventPublisher, //
            GeoServer geoServer //
            ) {
        return new UpdateSequenceController(updateSequence, eventPublisher, geoServer);
    }
}
