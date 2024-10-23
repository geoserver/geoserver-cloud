/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.config;

import lombok.NonNull;
import org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @since 1.4
 */
public class PgconfigGeoServerFacade extends RepositoryGeoServerFacadeImpl {

    public PgconfigGeoServerFacade(@NonNull JdbcTemplate template) {
        super(new PgconfigConfigRepository(template));
    }

    public PgconfigGeoServerFacade(@NonNull PgconfigConfigRepository repo) {
        super(repo);
    }
}
