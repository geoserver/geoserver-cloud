/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.jackson.databind.catalog.dto.AttributeType;
import org.geoserver.jackson.databind.catalog.dto.CRS;
import org.geoserver.jackson.databind.catalog.dto.Envelope;
import org.geoserver.jackson.databind.catalog.dto.GridGeometryDto;
import org.geoserver.jackson.databind.catalog.dto.MeasureDto;
import org.geoserver.jackson.databind.catalog.dto.NumberRangeDto;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.measure.Measure;
import org.geotools.util.NumberRange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface ValueMappers {
    default CoordinateReferenceSystem coordinateReferenceSystem(CRS source) {
        return null;
    }

    default CRS crs(CoordinateReferenceSystem source) {
        return null;
    }

    default NumberRangeDto numberRangeToDto(NumberRange<?> source) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    default NumberRange dtoToNumberRange(NumberRangeDto source) {
        return null;
    }

    default MeasureDto measureToDto(Measure value) {
        return null;
    }

    default Measure dtoToMeasure(MeasureDto value) {
        return null;
    }

    default Envelope referencedEnvelopeToDto(ReferencedEnvelope env) {
        return null;
    }

    default ReferencedEnvelope dtoToReferencedEnvelope(Envelope env) {
        return null;
    }

    default GridGeometry dtoToGridGeometry2D(GridGeometryDto value) {
        return null;
    }

    default GridGeometryDto gridGeometry2DToDto(GridGeometry value) {
        return null;
    }

    default String classToCanonicalName(Class<?> value) {
        return value == null ? null : value.getCanonicalName();
    }

    @SuppressWarnings("rawtypes")
    default Class canonicalNameToClass(String value) {
        try {
            return value == null ? null : Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    AttributeType map(AttributeTypeInfo o);

    @Mapping(target = "featureType", ignore = true)
    @Mapping(target = "attribute", ignore = true)
    AttributeTypeInfo map(AttributeType o);
}
