/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.service;

import static org.geoserver.catalog.impl.ClassMappings.LAYER;
import static org.geoserver.catalog.impl.ClassMappings.LAYERGROUP;
import static org.geoserver.catalog.impl.ClassMappings.MAP;
import static org.geoserver.catalog.impl.ClassMappings.NAMESPACE;
import static org.geoserver.catalog.impl.ClassMappings.RESOURCE;
import static org.geoserver.catalog.impl.ClassMappings.STORE;
import static org.geoserver.catalog.impl.ClassMappings.STYLE;
import static org.geoserver.catalog.impl.ClassMappings.WORKSPACE;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

import java.util.EnumMap;
import java.util.function.Supplier;

// revisit, replaced by BlockingCatalog
// @Component
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class CatalogInfoRepositoryProvider {

    private @Getter @Setter WorkspaceRepository workspaces;
    private @Getter @Setter NamespaceRepository namespaces;
    private @Getter @Setter StoreRepository stores;
    private @Getter @Setter ResourceRepository resources;
    private @Getter @Setter LayerRepository layers;
    private @Getter @Setter LayerGroupRepository layerGroups;
    private @Getter @Setter StyleRepository styles;
    private @Getter @Setter MapRepository maps;

    private final EnumMap<ClassMappings, Supplier<CatalogInfoRepository<?>>> repos =
            registerRepositories();

    public <C extends CatalogInfo, R extends CatalogInfoRepository<C>> @NonNull R of(
            @NonNull Class<? extends CatalogInfo> type) {

        @SuppressWarnings("unchecked")
        R repository = (R) repos.get(toKey(type)).get();
        return repository;
    }

    private @NonNull ClassMappings toKey(@NonNull Class<? extends CatalogInfo> type) {
        ClassMappings mappings = ClassMappings.fromInterface(type);
        if (mappings == null) mappings = ClassMappings.fromImpl(type);
        return mappings;
    }

    private EnumMap<ClassMappings, Supplier<CatalogInfoRepository<?>>> registerRepositories() {

        EnumMap<ClassMappings, Supplier<CatalogInfoRepository<?>>> repos;
        repos = new EnumMap<>(ClassMappings.class);

        registerRepository(repos, WORKSPACE, this::workspaces);
        registerRepository(repos, NAMESPACE, this::namespaces);
        registerRepository(repos, STORE, this::stores);
        registerRepository(repos, RESOURCE, this::resources);
        registerRepository(repos, LAYER, this::layers);
        registerRepository(repos, LAYERGROUP, this::layerGroups);
        registerRepository(repos, STYLE, this::styles);
        registerRepository(repos, MAP, this::maps);
        return repos;
    }

    private static void registerRepository(
            EnumMap<ClassMappings, Supplier<CatalogInfoRepository<?>>> repos,
            ClassMappings typeDef,
            Supplier<CatalogInfoRepository<?>> supplier) {
        repos.put(typeDef, supplier);
        final Class<? extends Info> baseType = typeDef.getInterface();
        Class<? extends Info>[] concreteInterfaces = typeDef.concreteInterfaces();
        for (Class<? extends Info> i : concreteInterfaces) {
            if (!(baseType.equals(i))) {
                repos.put(ClassMappings.fromInterface(i), supplier);
            }
        }
    }
}
