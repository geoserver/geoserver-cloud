/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.remote.legendsample.RemoteEventLegendSampleProcessor;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Invalidates the legend samples of the capabilities upon remote changes of styles. The wms and gwc services define the
 * {@code legendSample} bean, the other services have nothing to invalidate.
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnClass(name = "org.geoserver.wms.capabilities.LegendSampleImpl")
@ConditionalOnCatalogEvents
public class RemoteEventLegendSampleAutoConfiguration {

    @Bean
    RemoteEventLegendSampleProcessor remoteEventLegendSampleProcessor(
            @Qualifier("rawCatalog") Catalog rawCatalog, ObjectProvider<LegendSampleImpl> legendSamples) {

        return new RemoteEventLegendSampleProcessor(rawCatalog, legendSamples);
    }
}
