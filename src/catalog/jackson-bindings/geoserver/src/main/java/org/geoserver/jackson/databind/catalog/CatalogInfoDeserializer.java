/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.mapstruct.factory.Mappers;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

/**
 * Decodes any {@link CatalogInfo} through the polymorphic {@link CatalogInfoDto} hierarchy.
 *
 * <p>Asking the parser for a {@code CatalogInfoDto} makes Jackson build the type deserializer of the polymorphic root
 * again on each read, which is costly on hot paths. The instance created by {@link #createContextual} looks the root
 * deserializer up once and reuses it. Nothing is cached in a static field: a module instance may be registered in
 * several mappers, each with its own root deserializer.
 */
public class CatalogInfoDeserializer<I extends CatalogInfo> extends ValueDeserializer<I> {

    private static final CatalogInfoMapper mapper = Mappers.getMapper(CatalogInfoMapper.class);

    /** The root deserializer of the mapper being contextualized, or {@code null} if not contextualized. */
    private final ValueDeserializer<Object> rootDeserializer;

    public CatalogInfoDeserializer() {
        this(null);
    }

    private CatalogInfoDeserializer(ValueDeserializer<Object> rootDeserializer) {
        this.rootDeserializer = rootDeserializer;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType dtoType = ctxt.constructType(CatalogInfoDto.class);
        ValueDeserializer<Object> root = ctxt.findRootValueDeserializer(dtoType);
        return new CatalogInfoDeserializer<>(root);
    }

    @Override
    public I deserialize(JsonParser parser, DeserializationContext ctxt) {
        CatalogInfoDto dto = readDto(parser, ctxt);
        return mapper.map(dto);
    }

    private CatalogInfoDto readDto(JsonParser parser, DeserializationContext ctxt) {
        if (rootDeserializer == null) {
            return parser.readValueAs(CatalogInfoDto.class);
        }
        return (CatalogInfoDto) rootDeserializer.deserialize(parser, ctxt);
    }
}
