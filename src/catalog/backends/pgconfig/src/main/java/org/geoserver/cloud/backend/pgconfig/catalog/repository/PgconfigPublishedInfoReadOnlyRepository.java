/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import lombok.NonNull;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.plugin.Patch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class PgconfigPublishedInfoReadOnlyRepository extends PgconfigPublishedInfoRepository<PublishedInfo> {

    public PgconfigPublishedInfoReadOnlyRepository(
            @NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {

        super(PublishedInfo.class, template, styleLoader);
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.PUBLISHEDINFO_BUILD_COLUMNS;
    }

    @Override
    protected final RowMapper<PublishedInfo> newRowMapper() {
        return CatalogInfoRowMapper.<PublishedInfo>newInstance().setStyleLoader(styleRepo::findById);
    }

    @Override
    public void add(@NonNull PublishedInfo value) {
        throwUnsupported();
    }

    @Override
    public void remove(@NonNull PublishedInfo value) {
        throwUnsupported();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PublishedInfo update(@NonNull PublishedInfo value, @NonNull Patch patch) {
        throw throwUnsupported();
    }

    private UnsupportedOperationException throwUnsupported() {
        throw new UnsupportedOperationException(
                "%s is read-only".formatted(getClass().getSimpleName()));
    }
}
