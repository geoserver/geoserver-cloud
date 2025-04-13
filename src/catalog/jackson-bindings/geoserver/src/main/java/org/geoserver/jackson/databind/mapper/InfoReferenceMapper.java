/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.mapper;

import java.util.Objects;
import lombok.Generated;
import lombok.NonNull;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper
@AnnotateWith(value = Generated.class)
public abstract class InfoReferenceMapper {

    public String id(Info info) {
        return info == null ? null : info.getId();
    }

    public WorkspaceInfo workspaceInfo(String id) {
        return info(id, WorkspaceInfo.class);
    }

    public NamespaceInfo namespaceInfo(String id) {
        return info(id, NamespaceInfo.class);
    }

    @Named("storeInfo")
    public StoreInfo storeInfo(String id) {
        return info(id, StoreInfo.class);
    }

    @Named("coverageStoreInfo")
    public CoverageStoreInfo coverageStoreInfo(String id) {
        return info(id, CoverageStoreInfo.class);
    }

    @Named("dataStoreInfo")
    public DataStoreInfo dataStoreInfo(String id) {
        return info(id, DataStoreInfo.class);
    }

    @Named("wmsStoreInfo")
    public WMSStoreInfo wmsStoreInfo(String id) {
        return info(id, WMSStoreInfo.class);
    }

    @Named("wmtsStoreInfo")
    public WMTSStoreInfo wmtsStoreInfo(String id) {
        return info(id, WMTSStoreInfo.class);
    }

    @Named("resourceInfo")
    public ResourceInfo resourceInfo(String id) {
        return info(id, ResourceInfo.class);
    }

    public FeatureTypeInfo featureTypeInfo(String id) {
        return info(id, FeatureTypeInfo.class);
    }

    @Named("publishedInfo")
    public PublishedInfo publishedInfo(String id) {
        return info(id, PublishedInfo.class);
    }

    public LayerInfo layerInfo(String id) {
        return info(id, LayerInfo.class);
    }

    public LayerGroupInfo layerGroupInfo(String id) {
        return info(id, LayerGroupInfo.class);
    }

    public StyleInfo styleInfo(String id) {
        return info(id, StyleInfo.class);
    }

    private <T extends Info> T info(String id, Class<T> type) {
        return id == null ? null : ResolvingProxy.create(id, type);
    }

    public <T extends Info> InfoReference infoToReference(final T info) {
        if (info == null) {
            return null;
        }
        final String id = info.getId();
        final ClassMappings type = resolveType(info);

        // beware of remote styles that have no id
        if (ClassMappings.STYLE.equals(type)) {
            StyleInfo s = (StyleInfo) info;
            MetadataMap metadata = s.getMetadata();
            boolean isRemoteStyle = metadata != null
                    && Boolean.valueOf(metadata.getOrDefault(StyleInfoImpl.IS_REMOTE, "false")
                            .toString());
            if (isRemoteStyle) {
                return null;
            }
        }
        Objects.requireNonNull(id, () -> "Object has no id: " + info);
        Objects.requireNonNull(type, () -> "Bad info class: " + info.getClass());
        return new InfoReference(type, id);
    }

    private ClassMappings resolveType(@NonNull Info value) {
        value = ModificationProxy.unwrap(value);
        ClassMappings type = ClassMappings.fromImpl(value.getClass());
        if (type == null) {
            Class<?>[] interfaces = value.getClass().getInterfaces();
            for (Class<?> i : interfaces) {
                if (Info.class.isAssignableFrom(i)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Info> infoClass = (Class<? extends Info>) i;
                    type = ClassMappings.fromInterface(infoClass);
                    if (type != null) {
                        break;
                    }
                }
            }
        }
        return type;
    }

    public <T extends Info> T referenceToInfo(InfoReference ref) {
        if (ref == null) {
            return null;
        }
        String id = ref.getId();
        Objects.requireNonNull(id, () -> "Object Reference has no id: " + ref);
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) ref.getType().getInterface();
        return ResolvingProxy.create(id, type);
    }
}
