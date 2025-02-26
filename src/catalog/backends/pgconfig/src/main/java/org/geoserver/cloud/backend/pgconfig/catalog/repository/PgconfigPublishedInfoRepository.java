/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.isInstanceOf;

import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.PublishedInfo;
import org.geotools.api.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @since 2.27.0.0
 */
public abstract class PgconfigPublishedInfoRepository<P extends PublishedInfo>
        extends PgconfigCatalogInfoRepository<P> {

    protected final PgconfigStyleRepository styleRepo;

    protected PgconfigPublishedInfoRepository(
            @NonNull Class<P> type, @NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {
        super(type, template);
        this.styleRepo = styleLoader;
    }

    @Override
    protected final String getQueryTable() {
        return "publishedinfos";
    }

    @Override
    protected <S extends Info> Filter applyTypeFilter(Filter filter, Class<S> type) {
        if (!PublishedInfo.class.equals(type)) {
            filter = and(isInstanceOf(type), filter);
        }
        return filter;
    }
}
