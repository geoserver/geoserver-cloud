/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;

/**
 * Jackson {@link com.fasterxml.jackson.databind.Module} to handle GeoServer {@link CatalogInfo}
 * bindings.
 *
 * <p>Depends on {@link GeoToolsGeoJsonModule} and {@link GeoToolsFilterModule}.
 *
 * <p>To register the module for a specific {@link ObjectMapper}, either:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.findAndRegisterModules();
 * </code>
 * </pre>
 *
 * Or:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.registerModule(new GeoServerCatalogModule());
 * objectMapper.registerModule(new GeoToolsGeoJsonModule());
 * objectMapper.registerModule(new GeoToolsFilterModule());
 * </code>
 * </pre>
 */
@Slf4j
public class GeoServerCatalogModule extends SimpleModule {
    private static final long serialVersionUID = -8756800180255446679L;

    private final CatalogInfoDeserializer<CatalogInfo> deserializer =
            new CatalogInfoDeserializer<>();

    public GeoServerCatalogModule() {
        super(GeoServerCatalogModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        log.debug("registering jackson de/serializers for all GeoServer CatalogInfo types");

        this.addSerializer(CatalogInfo.class);
        this.addDeserializer(CatalogInfo.class);
        Arrays.stream(ClassMappings.values())
                .map(
                        c -> {
                            Class<CatalogInfo> ci = c.getInterface();
                            return ci;
                        })
                .filter(CatalogInfo.class::isAssignableFrom)
                .distinct()
                .sorted((c1, c2) -> c1.getSimpleName().compareTo(c2.getSimpleName()))
                .forEach(
                        c -> {
                            this.addSerializer((Class<CatalogInfo>) c);
                            this.addDeserializer((Class<CatalogInfo>) c);
                        });
    }

    private <T extends CatalogInfo> void addSerializer(Class<T> clazz) {
        log.trace("registering serializer for {}", clazz.getSimpleName());
        super.addSerializer(new CatalogInfoSerializer<>(clazz));
    }

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> void addDeserializer(Class<T> clazz) {
        log.trace("registering deserializer for {}", clazz.getSimpleName());
        super.addDeserializer(clazz, (CatalogInfoDeserializer<T>) deserializer);
    }
}
