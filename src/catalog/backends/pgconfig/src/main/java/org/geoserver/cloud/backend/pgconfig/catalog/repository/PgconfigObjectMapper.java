/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;

import org.geotools.jackson.databind.util.ObjectMapperUtil;

@UtilityClass
public class PgconfigObjectMapper {

    /**
     * Creates a Jackson {@link ObjectMapper} configured for the PostgreSQL JSONB encoding of
     * catalog and config info objects. In particular, null values must be encoded as {@code null}
     * in JSON, for the Postgres triggers calling {@code populate_table_columns_from_jsonb()} to
     * correctly unset columns used for joining when values in the JSON representation change to
     * {@code null}.
     *
     * <p>For example, if a layer group is moved from one workspace to no-workspace, the {@code
     * layergroupinfo.workspace} table column must be automatically set to null by {@code
     * populate_table_columns_from_jsonb()}, which won't happen if the new JSON object does not have
     * the {@code "workspace": null} property, as when the {@link ObjectMapper} has default property
     * inclusion {@link Include#NON_EMPTY}.
     *
     * @return a Jackson {@link ObjectMapper} configured for the PostgreSQL JSONB encoding of
     *     catalog and config info objects.
     * @see ObjectMapperUtil#newObjectMapper()
     * @see {@code populate_table_columns_from_jsonb()} in {@literal
     *     src/main/resources/db/pgconfig/migration/postgresql/V1_0__Catalog_Tables.sql}
     */
    public static ObjectMapper newObjectMapper() {
        ObjectMapper mapper = ObjectMapperUtil.newObjectMapper();
        // encode nulls as nulls, default from ObjectMapperUtil.newObjectMapper() is
        // Include.NON_EMPTY
        mapper.setDefaultPropertyInclusion(Include.ALWAYS);
        return mapper;
    }
}
