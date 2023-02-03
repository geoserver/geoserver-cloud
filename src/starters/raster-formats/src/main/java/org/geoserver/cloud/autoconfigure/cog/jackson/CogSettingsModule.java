/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.cog.CogSettings;

import java.util.function.Function;

/**
 * Jackson-databind module to enable encoding {@link CogSettings} as part of {@link
 * CoverageStoreInfo}'s metadata map for {@literal gs-jackson-databind}
 *
 * <p>The module is exposed through the {@literal com.fasterxml.jackson.databind.Module} SPI.
 *
 * @since 1.0
 */
public class CogSettingsModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    private final CogSettingsMapper mapper = new CogSettingsMapper();

    public CogSettingsModule() {
        super("CogSettingsModule");

        register(
                org.geoserver.cog.CogSettings.class,
                mapper::toJackson,
                org.geoserver.cloud.autoconfigure.cog.jackson.CogSettings.class,
                mapper::toModel);

        register(
                org.geoserver.cog.CogSettingsStore.class,
                mapper::toJackson,
                org.geoserver.cloud.autoconfigure.cog.jackson.CogSettingsStore.class,
                mapper::toModel);
    }

    private <T, DTO> void register(
            Class<T> type,
            Function<T, DTO> serializerMapper,
            Class<DTO> dtoType,
            Function<DTO, T> deserializerMapper) {

        MapperSerializer<T, DTO> serializer = new MapperSerializer<>(type, serializerMapper);
        MapperDeserializer<DTO, T> deserializer =
                new MapperDeserializer<>(dtoType, deserializerMapper);
        super.addSerializer(type, serializer);
        super.addDeserializer(type, deserializer);
    }
}
