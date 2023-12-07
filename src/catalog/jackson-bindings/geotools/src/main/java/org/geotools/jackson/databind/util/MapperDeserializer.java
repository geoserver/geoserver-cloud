/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Function;

/**
 * Generic {@link JsonDeserializer} that applies a function from an for-the-wire POJO type to the
 * the original object type after {@link JsonParser#readValueAs(Class)} reading it.
 *
 * @param <D> DTO type
 * @param <T> object model type
 */
@Slf4j
@RequiredArgsConstructor
public class MapperDeserializer<D, T> extends JsonDeserializer<T> {

    private final @NonNull Class<D> from;
    private final Function<D, T> mapper;

    @Override
    public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {

        D dto;
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
