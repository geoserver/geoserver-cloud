/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import static org.geoserver.catalog.impl.ClassMappings.PUBLISHED;
import static org.geoserver.catalog.impl.ClassMappings.RESOURCE;
import static org.geoserver.catalog.impl.ClassMappings.STORE;

import static java.util.Objects.requireNonNull;

import lombok.ToString;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoTypeRegistry;
import org.springframework.cache.interceptor.SimpleKey;

import java.io.Serializable;
import java.util.Objects;

/**
 * A key to for a cached {@link Info}, easier than implementing multiple key generators; it's also a
 * smaller memory footprint than {@link SimpleKey}
 */
@ToString
public class CatalogInfoKey implements Serializable {
    private static final long serialVersionUID = 8016140044040386038L;

    private String id;
    private ClassMappings type;

    /**
     * Caching constructor, resolves {@code info}'s type to its concrete {@link
     * ClassMappings#getInterface()}
     */
    public CatalogInfoKey(Info info) {
        requireNonNull(info);
        this.id = info.getId();
        this.type = resolveTypeId(info);
    }

    /**
     * Query constructor by concrete type, throws {@link IllegalArgumentException} if {@code type}
     * is a base type like {@link ResourceInfo} or {@link StoreInfo}
     */
    public CatalogInfoKey(String id, ClassMappings type) {
        requireNonNull(id);
        requireNonNull(type);
        if (type.concreteInterfaces().length > 1) {
            throw new IllegalArgumentException(
                    "type shall be a concrete type, got " + type.getInterface().getName());
        }
        this.id = id;
        this.type = type;
    }

    /**
     * Query constructor, allows {@code type} to be a base type like {@link ResourceInfo} or {@link
     * StoreInfo}
     */
    public CatalogInfoKey(String id, Class<? extends Info> type) {
        this.id = id;
        this.type = resolveTypeId(type);
    }

    private ClassMappings resolveTypeId(Info info) {
        Class<? extends Info> type = CatalogInfoTypeRegistry.resolveType(info);
        return resolveTypeId(type);
    }

    private ClassMappings resolveTypeId(Class<? extends Info> type) {
        return CatalogInfoTypeRegistry.determineKey(type);
    }

    public @Override int hashCode() {
        return id.hashCode();
        // return 31 * id.hashCode() + (type == null ? 0 : type.hashCode());
    }

    public @Override boolean equals(Object o) {
        if (!CatalogInfoKey.class.isInstance(o)) return false;

        CatalogInfoKey k = (CatalogInfoKey) o;

        if (!id.equals(k.id)) return false;
        ClassMappings t1 = this.type;
        ClassMappings t2 = k.type;
        return typeIsCompatible(t1, t2);
    }

    private boolean typeIsCompatible(ClassMappings c1, ClassMappings c2) {
        if (Objects.equals(c1, c2)) return true;
        if (c1 == null || c2 == null) return false;

        if (c1.equals(RESOURCE)) return ResourceInfo.class.isAssignableFrom(c2.getInterface());
        if (c2.equals(RESOURCE)) return ResourceInfo.class.isAssignableFrom(c1.getInterface());

        if (c1.equals(STORE)) return StoreInfo.class.isAssignableFrom(c2.getInterface());
        if (c2.equals(STORE)) return StoreInfo.class.isAssignableFrom(c1.getInterface());

        if (c1.equals(PUBLISHED)) return PublishedInfo.class.isAssignableFrom(c2.getInterface());
        if (c2.equals(PUBLISHED)) return PublishedInfo.class.isAssignableFrom(c1.getInterface());

        return false;
    }
}
