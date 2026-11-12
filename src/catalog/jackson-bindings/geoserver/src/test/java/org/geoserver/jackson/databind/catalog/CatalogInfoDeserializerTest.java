/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.SubtypeResolver;
import tools.jackson.databind.jsontype.impl.StdSubtypeResolver;

class CatalogInfoDeserializerTest {

    private Catalog catalog;
    private CatalogTestData data;
    private ProxyUtils proxyResolver;

    @BeforeAll
    static void oneTimeSetup() {
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    @BeforeEach
    void before() {
        catalog = new CatalogPlugin();
        GeoServer geoserver = new GeoServerImpl();
        geoserver.setCatalog(catalog);
        data = CatalogTestData.initialized(() -> catalog, () -> geoserver).initialize();
        proxyResolver = new ProxyUtils(() -> catalog, Optional.of(geoserver));
    }

    @Test
    void typeResolvedOncePerMapper() throws Exception {
        CountingSubtypeResolver resolver = new CountingSubtypeResolver();
        ObjectMapper mapper = newCountingMapper(resolver);
        String json = mapper.writeValueAsString(data.layerFeatureTypeA);

        mapper.readValue(json, LayerInfo.class);
        mapper.readValue(json, LayerInfo.class);
        int countAfterSecond = resolver.calls.get();
        for (int i = 2; i < 100; i++) {
            mapper.readValue(json, LayerInfo.class);
        }

        assertThat(resolver.calls.get()).isEqualTo(countAfterSecond);
    }

    @Test
    void mappersShareOneModule() throws Exception {
        GeoServerCatalogModule shared = new GeoServerCatalogModule();
        ObjectMapper first = newMapperWith(shared);
        ObjectMapper second = newMapperWith(shared);
        LayerInfo layer = ModificationProxy.unwrap(data.layerFeatureTypeA);
        FeatureTypeInfo featureType = ModificationProxy.unwrap(data.featureTypeA);

        LayerInfo decodedLayer = first.readValue(first.writeValueAsString(layer), LayerInfo.class);
        FeatureTypeInfo decodedType = second.readValue(second.writeValueAsString(featureType), FeatureTypeInfo.class);

        assertThat(proxyResolver.resolve(decodedLayer)).isEqualTo(layer);
        assertThat(proxyResolver.resolve(decodedType)).isEqualTo(featureType);
    }

    private ObjectMapper newMapperWith(GeoServerCatalogModule module) {
        return JsonMapper.builder().findAndAddModules().addModule(module).build();
    }

    private ObjectMapper newCountingMapper(CountingSubtypeResolver resolver) {
        return JsonMapper.builder()
                .findAndAddModules()
                .subtypeResolver(resolver)
                .build();
    }

    /** Counts type id resolutions. The mapper uses a snapshot of its resolver. The snapshot shares the counter. */
    private static class CountingSubtypeResolver extends StdSubtypeResolver {
        private static final long serialVersionUID = 1L;

        final transient AtomicInteger calls;

        CountingSubtypeResolver() {
            this(new AtomicInteger(), new HashSet<>());
        }

        private CountingSubtypeResolver(AtomicInteger calls, Set<NamedType> registered) {
            super(registered);
            this.calls = calls;
        }

        @Override
        public SubtypeResolver snapshot() {
            Set<NamedType> registered = new HashSet<>(_registeredSubtypes);
            return new CountingSubtypeResolver(calls, registered);
        }

        @Override
        public Collection<NamedType> collectAndResolveSubtypesByTypeId(
                MapperConfig<?> config, AnnotatedClass baseType) {
            calls.incrementAndGet();
            return super.collectAndResolveSubtypesByTypeId(config, baseType);
        }

        @Override
        public Collection<NamedType> collectAndResolveSubtypesByTypeId(
                MapperConfig<?> config, AnnotatedMember property, JavaType baseType) {
            calls.incrementAndGet();
            return super.collectAndResolveSubtypesByTypeId(config, property, baseType);
        }
    }
}
