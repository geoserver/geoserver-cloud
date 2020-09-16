/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.mapper;

import java.util.Optional;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.wfs.GMLInfo;
import org.geotools.feature.NameImpl;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.opengis.feature.type.Name;

@Mapper
public abstract class SharedMappers {

    public <T extends Info> InfoReference infoToReference(T info) {
        if (info == null) return null;

        InfoReference ref = new InfoReference();
        ref.setId(info.getId());
        ClassMappings type = ClassMappings.fromImpl(ModificationProxy.unwrap(info).getClass());
        ref.setType(type);
        return ref;
    }

    public <T extends Info> T referenceToInfo(InfoReference ref) {
        if (ref == null) return null;
        String id = ref.getId();
        Class<T> type = ref.getType().getInterface();
        T proxy = ResolvingProxy.create(id, type);
        return proxy;
    }

    public String classToCanonicalName(Class<?> value) {
        return value == null ? null : value.getCanonicalName();
    }

    @SuppressWarnings("rawtypes")
    public Class canonicalNameToClass(String value) {
        try {
            return value == null ? null : Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public @ObjectFactory KeywordInfo keywordInfo(Keyword source) {
        return new org.geoserver.catalog.Keyword(source.getValue());
    }

    public @ObjectFactory MetadataLinkInfo metadataLinkInfo() {
        return new MetadataLinkInfoImpl();
    }

    public @ObjectFactory AuthorityURLInfo authorityURLInfo() {
        return new AuthorityURL();
    }

    public @ObjectFactory LayerIdentifierInfo layerIdentifierInfo() {
        return new LayerIdentifier();
    }

    /** Added sue to {@link GMLInfo#getMimeTypeToForce()} */
    public String optToString(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }

    public Optional<String> stringToOpt(String value) {
        return Optional.ofNullable(value);
    }

    public abstract NameDto map(org.opengis.feature.type.Name name);

    public Name map(NameDto dto) {
        return new NameImpl(dto.getNamespaceURI(), dto.getLocalPart());
    }
}
