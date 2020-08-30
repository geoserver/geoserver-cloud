package org.geoserver.cloud.autoconfigure.catalog.plugin;

import org.geoserver.catalog.plugin.DefaultCatalogFacade;
import org.geoserver.cloud.config.catalog.RawCatalogConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Overrides the "rawCatalog" initialization to make sure its default catalog facade is a pluggable
 * {@link DefaultCatalogFacade}
 */
@Configuration
@Import(RawCatalogConfiguration.class)
public class PluggableCatalogBackendAutoconfiguration {}
