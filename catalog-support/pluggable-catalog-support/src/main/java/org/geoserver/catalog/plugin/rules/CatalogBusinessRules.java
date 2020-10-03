/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import java.util.EnumMap;
import java.util.Objects;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.PropertyDiff;

/** */
public class CatalogBusinessRules {
    private Catalog catalog;

    private EnumMap<ClassMappings, CatalogInfoBusinessRules<?>> rulesByType =
            new EnumMap<>(ClassMappings.class);

    public CatalogBusinessRules(Catalog catalog) {
        this.catalog = catalog;
        register(ClassMappings.WORKSPACE, new DefaultWorkspaceInfoRules());
        register(ClassMappings.NAMESPACE, new DefaultNamespaceInfoRules());
        register(ClassMappings.STORE, new DefaultStoreInfoRules());
        register(ClassMappings.RESOURCE, new DefaultResourceInfoRules());
        register(ClassMappings.LAYER, new DefaultLayerInfoRules());
        register(ClassMappings.LAYERGROUP, new DefaultLayerGroupInfoRules());
        register(ClassMappings.STYLE, new DefaultStyleInfoRules());
        register(ClassMappings.MAP, new CatalogInfoBusinessRules<MapInfo>() {});
    }

    private void register(ClassMappings cm, CatalogInfoBusinessRules<?> rules) {
        rulesByType.put(cm, rules);
        Class<? extends Info>[] concreteInterfaces = cm.concreteInterfaces();
        if (concreteInterfaces.length > 1) {
            for (Class<? extends Info> c : concreteInterfaces) {
                rulesByType.put(ClassMappings.fromInterface(c), rules);
            }
        }
    }

    public <T extends CatalogInfo> CatalogInfoBusinessRules<T> rulesFor(T info) {
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) ModificationProxy.unwrap(info).getClass();
        return rulesOf(type);
    }

    public <T extends CatalogInfo> CatalogInfoBusinessRules<T> rulesOf(Class<? extends T> type) {
        ClassMappings cm =
                type.isInterface()
                        ? ClassMappings.fromInterface(type)
                        : ClassMappings.fromImpl(type);

        Objects.requireNonNull(
                cm, "Unable to determine type enum for class " + type.getCanonicalName());

        @SuppressWarnings("unchecked")
        CatalogInfoBusinessRules<T> rules = (CatalogInfoBusinessRules<T>) rulesByType.get(cm);
        Objects.requireNonNull(
                rules, () -> "Rules for type " + type.getCanonicalName() + " not found");
        return rules;
    }

    public <T extends CatalogInfo> void onAfterAdd(T info) {
        rulesFor(info).onAfterAdd(catalog, info);
    }

    public <T extends CatalogInfo> void onBeforeSave(T info, PropertyDiff diff) {
        rulesFor(info).onBeforeSave(catalog, info, diff);
    }

    public <T extends CatalogInfo> void onAfterSave(T info, PropertyDiff diff) {
        rulesFor(info).onAfterSave(catalog, info, diff);
    }

    public <T extends CatalogInfo> void onSaveError(T info, PropertyDiff diff, Throwable error) {
        rulesFor(info).onSaveError(catalog, info, diff, error);
    }

    public <T extends CatalogInfo> void onRemoved(T info) {
        rulesFor(info).onRemoved(catalog, info);
    }
}
