/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.info;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.Catalog;
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
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ConfigInfoType {
    CATALOG(Catalog.class), //
    WORKSPACE(WorkspaceInfo.class), //
    NAMESPACE(NamespaceInfo.class), //
    COVERAGESTORE(CoverageStoreInfo.class), //
    DATASTORE(DataStoreInfo.class), //
    WMSSTORE(WMSStoreInfo.class), //
    WMTSSTORE(WMTSStoreInfo.class), //
    FEATURETYPE(FeatureTypeInfo.class), //
    COVERAGE(CoverageInfo.class), //
    WMSLAYER(WMSLayerInfo.class), //
    WMTSLAYER(WMTSLayerInfo.class), //
    LAYER(LayerInfo.class), //
    LAYERGROUP(LayerGroupInfo.class), //
    MAP(MapInfo.class), //
    STYLE(StyleInfo.class), //
    GEOSERVER(GeoServerInfo.class), //
    SERVICE(ServiceInfo.class), //
    SETTINGS(SettingsInfo.class), //
    LOGGING(LoggingInfo.class), //
    // These abstract types are declared the latest for valueOf(Info) to resolve to the most
    // concrete one during traversal of #values()
    STORE(StoreInfo.class), //
    RESOURCE(ResourceInfo.class), //
    PUBLISHED(PublishedInfo.class);

    private final @Getter @NonNull Class<? extends Info> type;

    public boolean isInstance(Info object) {
        return object != null && getType().isInstance(object);
    }

    @NonNull
    public static ConfigInfoType valueOf(@NonNull Info object) {
        for (ConfigInfoType enumVal : ConfigInfoType.values()) {
            if (enumVal.isInstance(object)) {
                return enumVal;
            }
        }
        throw new IllegalArgumentException("Unknown info type for object " + object);
    }

    public boolean isA(Class<? extends Info> type) {
        return type.isAssignableFrom(getType());
    }

    @NonNull
    public static ConfigInfoType valueOf(@NonNull Class<? extends Info> clazz) {
        for (ConfigInfoType enumVal : ConfigInfoType.values()) {
            if (enumVal.getType().equals(clazz)) {
                return enumVal;
            }
        }
        throw new IllegalArgumentException("Unknown info type for class " + clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> type() {
        return (Class<T>) type;
    }
}
