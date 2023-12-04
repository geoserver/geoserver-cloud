/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog;

import lombok.NonNull;

import org.geoserver.catalog.plugin.RepositoryCatalogFacadeImpl;
import org.geoserver.cloud.backend.pgsql.catalog.repository.PgsqlLayerGroupRepository;
import org.geoserver.cloud.backend.pgsql.catalog.repository.PgsqlLayerRepository;
import org.geoserver.cloud.backend.pgsql.catalog.repository.PgsqlNamespaceRepository;
import org.geoserver.cloud.backend.pgsql.catalog.repository.PgsqlResourceRepository;
import org.geoserver.cloud.backend.pgsql.catalog.repository.PgsqlStoreRepository;
import org.geoserver.cloud.backend.pgsql.catalog.repository.PgsqlStyleRepository;
import org.geoserver.cloud.backend.pgsql.catalog.repository.PgsqlWorkspaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @since 1.4
 */
public class PgsqlCatalogFacade extends RepositoryCatalogFacadeImpl {

    public PgsqlCatalogFacade(@NonNull JdbcTemplate template) {
        super.setNamespaceRepository(new PgsqlNamespaceRepository(template));
        super.setWorkspaceRepository(new PgsqlWorkspaceRepository(template));
        super.setStoreRepository(new PgsqlStoreRepository(template));
        super.setResourceRepository(new PgsqlResourceRepository(template));
        super.setLayerRepository(new PgsqlLayerRepository(template));
        super.setLayerGroupRepository(new PgsqlLayerGroupRepository(template));
        super.setStyleRepository(new PgsqlStyleRepository(template));
    }
}
