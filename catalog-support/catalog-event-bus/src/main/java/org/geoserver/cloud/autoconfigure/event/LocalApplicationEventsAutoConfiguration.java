/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.event;

import org.geoserver.cloud.event.LocalApplicationEventsConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration auto-configuration} for {@link
 * LocalApplicationEventsConfiguration}
 */
@Configuration
@ConditionalOnProperty(
    value = "geoserver.catalog.events.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Import({LocalApplicationEventsConfiguration.class})
public class LocalApplicationEventsAutoConfiguration {}
