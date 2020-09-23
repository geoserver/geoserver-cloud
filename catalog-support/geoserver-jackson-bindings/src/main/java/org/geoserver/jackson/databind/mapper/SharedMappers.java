/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geoserver.jackson.databind.catalog.dto.VersionDto;
import org.geoserver.jackson.databind.config.dto.NameDto;
import org.geoserver.wfs.GMLInfo;
import org.geotools.feature.NameImpl;
import org.geotools.jackson.databind.filter.dto.Expression.Literal;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.geotools.util.Version;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.factory.Mappers;
import org.opengis.feature.type.Name;

@Mapper
public abstract class SharedMappers {

    public Version versionToString(String v) {
        return v == null ? null : new Version(v);
    }

    public String stringToVersion(Version v) {
        return v == null ? null : v.toString();
    }

    public VersionDto versionToDto(Version v) {
        return v == null ? null : new VersionDto().setValue(v.toString());
    }

    public Version dtoToVersion(VersionDto v) {
        return v == null ? null : new Version(v.getValue());
    }

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

    public Patch dtoToPatch(PatchDto dto) {
        if (dto == null) return null;
        ExpressionMapper expressionMapper = Mappers.getMapper(ExpressionMapper.class);
        Patch patch = new Patch();
        dto.getPatches()
                .forEach(
                        (k, literalDto) -> {
                            org.opengis.filter.expression.Literal literal =
                                    expressionMapper.map(literalDto);
                            Object v = literal.getValue();
                            if (v instanceof InfoReference) {
                                v = referenceToInfo((InfoReference) v);
                            } else if (v instanceof Collection) {
                                v = resolveCollection((Collection<?>) v);
                            }
                            patch.add(new Patch.Property(k, v));
                        });
        return patch;
    }

    private Collection<?> resolveCollection(Collection<?> v) {
        @SuppressWarnings("unchecked")
        Collection<Object> resolved = (Collection<Object>) newCollectionInstance(v.getClass());
        for (Object o : v) {
            Object resolvedMember = o;
            if (o instanceof InfoReference) {
                resolvedMember = referenceToInfo((InfoReference) o);
            }
            resolved.add(resolvedMember);
        }
        return resolved;
    }

    private Object newCollectionInstance(Class<?> class1) {
        try {
            return class1.getConstructor().newInstance();
        } catch (Exception e) {
            if (List.class.isAssignableFrom(class1)) return new ArrayList<>();
            if (Set.class.isAssignableFrom(class1)) return new HashSet<>();
            if (Map.class.isAssignableFrom(class1)) return new HashMap<>();
        }
        return null;
    }

    public PatchDto patchToDto(Patch patch) {
        if (patch == null) return null;

        PatchDto dto = new PatchDto();
        for (Patch.Property propChange : patch.getPatches()) {
            String name = propChange.getName();
            Object value = resolvePatchValue(propChange);
            Literal literal = new org.geotools.jackson.databind.filter.dto.Expression.Literal();
            literal.setValue(value);
            dto.getPatches().put(name, literal);
        }
        return dto;
    }

    /**
     * If value is an identified {@link Info} (catalog or config object), returns an {@link
     * InfoReference} instead, to be resolved at the receiving end
     */
    private Object resolvePatchValue(Patch.Property prop) {
        if (prop == null) return null;
        Object value = patchPropertyValueToDto(prop.getValue());
        return value;
    };

    private Object patchPropertyValueToDto(Object value) {
        if (value instanceof Info) {
            return resolveReferenceOrValueObject((Info) value);
        } else if (value instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> col = (Collection<Object>) newCollectionInstance(value.getClass());
            for (Object v : ((Collection<?>) value)) {
                col.add(patchPropertyValueToDto(v));
            }
            return col;
        }

        return value;
    }

    private Object resolveReferenceOrValueObject(Info value) {
        value = ModificationProxy.unwrap(value);
        ClassMappings cm = ClassMappings.fromImpl(value.getClass());
        boolean useReference =
                cm != null; // && !(ClassMappings.GLOBAL == cm || ClassMappings.LOGGING == cm);
        if (useReference) {
            return this.infoToReference((Info) value);
        }
        return value;
    };
}
