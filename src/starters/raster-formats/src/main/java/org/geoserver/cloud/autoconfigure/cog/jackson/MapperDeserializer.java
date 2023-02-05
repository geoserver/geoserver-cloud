/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
class MapperDeserializer<DTO, T> extends JsonDeserializer<T> {

    private final @NonNull Class<DTO> from;
    private final Function<DTO, T> mapper;

    public @Override T deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        DTO dto;
        T value;
        try {
            dto = parser.readValueAs(from);
        } catch (RuntimeException e) {
            log.error("Error reading object of type {}", from.getCanonicalName(), e);
            throw e;
        }
        try {
            value = mapper.apply(dto);
        } catch (RuntimeException e) {
            log.error("Error mapping read object of type {}", dto.getClass().getCanonicalName(), e);
            throw e;
        }
        return value;
    }
}
