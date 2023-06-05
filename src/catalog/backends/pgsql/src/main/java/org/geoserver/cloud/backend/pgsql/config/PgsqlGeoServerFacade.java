/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.config;

import lombok.NonNull;

import org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @since 1.4
 */
public class PgsqlGeoServerFacade extends RepositoryGeoServerFacadeImpl {

    public PgsqlGeoServerFacade(@NonNull JdbcTemplate template) {
        super(new PgsqlConfigRepository(template));
    }

    public PgsqlGeoServerFacade(@NonNull PgsqlConfigRepository repo) {
        super(repo);
    }
}
