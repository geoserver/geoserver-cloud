/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Objects.requireNonNull;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class to register providers of any kind based on {@link CatalogInfo} subtypes.
 *
 * <p>It is often necessary to perform operations or access different object based on {@link
 * CatalogInfo} subtypes; and common to write a bunch of if-else-if blocks with instanceof checks.
 *
 * <p>This utility allows to register those elements and avoid that kind of code.
 *
 * @param R the type of resource to be accessed on a class basis for {@link CatalogInfo} subtypes
 */
public class CatalogInfoTypeRegistry<R> {

    private final EnumMap<ClassMappings, R> mappings = new EnumMap<>(ClassMappings.class);

    /**
     * Registers the {@code resource} for the given type only, first narrowing {@code type} to its
     * corresponding {@link CatalogInfo} interface type.
     */
    public <T extends CatalogInfo> CatalogInfoTypeRegistry<R> register(Class<T> type, R resource) {
        requireNonNull(type);
        requireNonNull(resource);
        ClassMappings key = determineKey(type);
        mappings.put(key, resource);
        return (CatalogInfoTypeRegistry<R>) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends CatalogInfo> CatalogInfoTypeRegistry<Consumer<T>> consume(
            Class<T> type, Consumer<T> with) {
        return ((CatalogInfoTypeRegistry<Consumer<T>>) this).registerRecursively(type, with);
    }

    /**
     * Registers the {@code resource} for the given type and all its {@link CatalogInfo}
     * sub-interfaces (e.g. for {@link StoreInfo StoreInfo.class}, registers {@code resource} to
     * match {@link StoreInfo}, {@link DataStoreInfo}, {@link CoverageStoreInfo}, {@link
     * WMSStoreInfo}, and {@link WMTSStoreInfo})
     */
    @SuppressWarnings("unchecked")
    public <T extends CatalogInfo> CatalogInfoTypeRegistry<R> registerRecursively(
            Class<T> type, R resource) {

        ClassMappings key = determineKey(type);
        Class<T> mainType = (Class<T>) key.getInterface();
        register(mainType, resource);
        Class<? extends Info>[] subtypes = key.concreteInterfaces();
        for (int i = 0; subtypes.length > 1 && i < subtypes.length; i++) {
            Class<? extends T> subtype = (Class<? extends T>) subtypes[i];
            register(subtype, resource);
        }
        return (CatalogInfoTypeRegistry<R>) this;
    }

    public R forObject(CatalogInfo object) {
        requireNonNull(object);
        return of(object.getClass());
    }

    public <T extends CatalogInfo> R of(Class<T> type) {
        requireNonNull(type);
        return of(determineKey(type));
    }

    public R of(ClassMappings key) {
        requireNonNull(key);
        return mappings.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Info> Class<T> resolveType(T object) {
        ClassMappings cm = ClassMappings.fromImpl(object.getClass());
        if (cm == null) {
            // don't really care if it's a proxy, can't assume ModificationProxy and
            // ResolvingProxy are the only ones
            for (int i = 0; i < instanceOfLookup.size(); i++) {
                if (instanceOfLookup.get(i).isInstance(object)) {
                    cm = ClassMappings.fromInterface(instanceOfLookup.get(i));
                    break;
                }
            }
        }
        if (cm == null)
            throw new IllegalArgumentException(
                    "Unable to determine CatalogInfo subtype from object " + object);
        return (Class<T>) cm.getInterface();
    }

    public static <T extends Info> ClassMappings determineKey(Class<T> type) {
        ClassMappings cm =
                type.isInterface()
                        ? ClassMappings.fromInterface(type)
                        : ClassMappings.fromImpl(type);
        if (cm != null) {
            return cm;
        }
        throw new IllegalArgumentException(
                "Unable to determine CatalogInfo subtype from class " + type.getName());
    }

    private static final List<Class<? extends CatalogInfo>> instanceOfLookup =
            Arrays.asList(
                    WorkspaceInfo.class,
                    NamespaceInfo.class,
                    DataStoreInfo.class,
                    CoverageStoreInfo.class,
                    WMSStoreInfo.class,
                    WMTSStoreInfo.class,
                    StoreInfo.class,
                    FeatureTypeInfo.class,
                    CoverageInfo.class,
                    WMSLayerInfo.class,
                    WMTSLayerInfo.class,
                    ResourceInfo.class,
                    LayerInfo.class,
                    LayerGroupInfo.class,
                    PublishedInfo.class,
                    StyleInfo.class,
                    MapInfo.class);
}
