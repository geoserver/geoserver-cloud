/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.mapper;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.jackson.databind.catalog.dto.CRS;
import org.geoserver.jackson.databind.catalog.dto.Envelope;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.jackson.databind.catalog.dto.VersionDto;
import org.geoserver.jackson.databind.config.dto.NameDto;
import org.geoserver.wfs.GMLInfo;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.wkt.Formattable;
import org.geotools.util.Version;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Optional;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
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

    public String classToCanonicalName(Class<?> value) {
        return Mappers.getMapper(org.geotools.jackson.databind.filter.mapper.ValueMappers.class)
                .classToCanonicalName(value);
    }

    @SuppressWarnings("rawtypes")
    public Class canonicalNameToClass(String value) {
        return Mappers.getMapper(org.geotools.jackson.databind.filter.mapper.ValueMappers.class)
                .canonicalNameToClass(value);
    }

    public @ObjectFactory KeywordInfo keywordInfo(Keyword source) {
        return new org.geoserver.catalog.Keyword(source.getValue());
    }

    public abstract KeywordInfo keyword(Keyword dto);

    public abstract Keyword keyword(KeywordInfo keyword);

    public @ObjectFactory MetadataLinkInfoImpl metadataLinkInfo() {
        return new MetadataLinkInfoImpl();
    }

    public @ObjectFactory AuthorityURLInfo authorityURLInfo() {
        return new AuthorityURL();
    }

    public @ObjectFactory LayerIdentifierInfo layerIdentifierInfo() {
        return new LayerIdentifier();
    }

    /** Added due to {@link GMLInfo#getMimeTypeToForce()} */
    public String optToString(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }

    public Optional<String> stringToOpt(String value) {
        return Optional.ofNullable(value);
    }

    public abstract NameDto map(org.geotools.api.feature.type.Name name);

    public Name map(NameDto dto) {
        return new NameImpl(dto.getNamespaceURI(), dto.getLocalPart());
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

    public Envelope referencedEnvelope(ReferencedEnvelope env) {
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

    public ReferencedEnvelope referencedEnvelope(Envelope source) {
        if (source == null) return null;
        CoordinateReferenceSystem crs = crs(source.getCrs());
        ReferencedEnvelope env = new ReferencedEnvelope(crs);
        double[] coords = source.getCoordinates();
        env.init(coords[0], coords[1], coords[2], coords[3]);
        return env;
    }
}
