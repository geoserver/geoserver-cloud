/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.security;

import org.geoserver.catalog.Catalog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoServerSecurityDisabledConfiguration {

    @Bean(name = "secureCatalog")
    public Catalog secureCatalog(@Qualifier("rawCatalog") Catalog rawCatalog) throws Exception {
        return rawCatalog;
    }
}
