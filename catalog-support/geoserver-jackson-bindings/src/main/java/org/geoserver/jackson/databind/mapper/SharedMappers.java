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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
import org.geoserver.jackson.databind.catalog.dto.CRS;
import org.geoserver.jackson.databind.catalog.dto.Envelope;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geoserver.jackson.databind.catalog.dto.VersionDto;
import org.geoserver.jackson.databind.config.dto.NameDto;
import org.geoserver.wfs.GMLInfo;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jackson.databind.filter.dto.Expression.Literal;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.wkt.Formattable;
import org.geotools.util.Version;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.factory.Mappers;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Mapper
@Slf4j
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

    public <T extends Info> InfoReference infoToReference(final T info) {
        if (info == null) return null;
        final String id = info.getId();
        ClassMappings type = resolveType(info);
        Objects.requireNonNull(id, () -> "Object has no id: " + info);
        Objects.requireNonNull(type, "Bad info class: " + info.getClass());
        return new InfoReference(type, id);
    }

    public <T extends Info> T referenceToInfo(InfoReference ref) {
        if (ref == null) return null;
        String id = ref.getId();
        Objects.requireNonNull(id, () -> "Object Reference has no id: " + ref);
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

    public abstract KeywordInfo dtoToKeyword(Keyword dto);

    public abstract Keyword keywordToDto(KeywordInfo keyword);

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
        boolean useReference = cm != null; // && !(ClassMappings.GLOBAL == cm ||
        // ClassMappings.LOGGING == cm);
        if (useReference) {
            return this.infoToReference((Info) value);
        }
        return value;
    };

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

    public CoordinateReferenceSystem crs(CRS source) {
        if (source == null) return null;
        try {
            if (null != source.getSrs()) {
                String srs = source.getSrs();
                boolean longitudeFirst = srs.startsWith("EPSG:");
                return org.geotools.referencing.CRS.decode(source.getSrs(), longitudeFirst);
            }
            return org.geotools.referencing.CRS.parseWKT(source.getWKT());
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    public CRS crs(CoordinateReferenceSystem source) {
        if (source == null) return null;
        CRS crs = new CRS();

        String srs = null;
        AxisOrder axisOrder = org.geotools.referencing.CRS.getAxisOrder(source, false);
        try {
            boolean fullScan = false;
            Integer code = org.geotools.referencing.CRS.lookupEpsgCode(source, fullScan);
            if (code != null) {
                if (axisOrder == AxisOrder.NORTH_EAST) {
                    srs = "urn:ogc:def:crs:EPSG::" + code;
                } else {
                    srs = "EPSG:" + code;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to determine EPSG code", e);
        }
        if (srs != null) {
            crs.setSrs(srs);
        } else {
            boolean strict = false;
            String wkt = ((Formattable) source).toWKT(0, strict);
            crs.setWKT(wkt);
        }
        return crs;
    }

    public Envelope referencedEnvelopeToDto(ReferencedEnvelope env) {
        if (env == null) return null;
        Envelope dto = new Envelope();
        int dimension = env.getDimension();
        double[] coordinates = new double[2 * dimension];
        for (int dim = 0, j = 0; dim < dimension; dim++, j += 2) {
            coordinates[j] = env.getMinimum(dim);
            coordinates[j + 1] = env.getMaximum(dim);
        }
        dto.setCoordinates(coordinates);
        dto.setCrs(crs(env.getCoordinateReferenceSystem()));
        return dto;
    }

    public ReferencedEnvelope dtoToReferencedEnvelope(Envelope source) {
        if (source == null) return null;
        CoordinateReferenceSystem crs = crs(source.getCrs());
        ReferencedEnvelope env = new ReferencedEnvelope(crs);
        double[] coords = source.getCoordinates();
        env.init(coords[0], coords[1], coords[2], coords[3]);
        return env;
    }
}
