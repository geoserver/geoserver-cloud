/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import java.awt.geom.AffineTransform;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.jackson.databind.catalog.dto.AttributeType;
import org.geoserver.jackson.databind.catalog.dto.Attribution;
import org.geoserver.jackson.databind.catalog.dto.AuthorityURL;
import org.geoserver.jackson.databind.catalog.dto.CRS;
import org.geoserver.jackson.databind.catalog.dto.CoverageDimension;
import org.geoserver.jackson.databind.catalog.dto.DataLink;
import org.geoserver.jackson.databind.catalog.dto.Dimension;
import org.geoserver.jackson.databind.catalog.dto.GridGeometryDto;
import org.geoserver.jackson.databind.catalog.dto.LayerIdentifier;
import org.geoserver.jackson.databind.catalog.dto.Legend;
import org.geoserver.jackson.databind.catalog.dto.MetadataLink;
import org.geoserver.jackson.databind.catalog.dto.NumberRangeDto;
import org.geoserver.jackson.databind.catalog.dto.QueryDto;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableDto;
import org.geoserver.jackson.databind.mapper.SharedMappers;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.util.MeasureConverterFactory;
import org.geotools.jdbc.VirtualTable;
import org.geotools.measure.Measure;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.NumberRange;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

@Mapper(config = CatalogInfoMapperConfig.class)
public interface ValueMappers {

    org.geotools.util.Converter str2Measure =
            new MeasureConverterFactory().createConverter(String.class, Measure.class, null);
    org.geotools.util.Converter measure2Str =
            new MeasureConverterFactory().createConverter(Measure.class, String.class, null);

    @Mapping(target = "withFilter", ignore = true)
    @SuppressWarnings("rawtypes")
    Query dtoToQuery(QueryDto dto);

    QueryDto queryToDto(@SuppressWarnings("rawtypes") Query query);

    @SuppressWarnings("rawtypes")
    default Class classMappings(ClassMappings mappings) {
        return mappings == null ? null : mappings.getInterface();
    }

    @SuppressWarnings("unchecked")
    default ClassMappings classMappings(@SuppressWarnings("rawtypes") Class type) {
        return type == null ? null : ClassMappings.fromInterface(type);
    }

    default NumberRangeDto numberRangeToDto(NumberRange<?> source) {
        if (source == null) return null;
        NumberRangeDto dto = new NumberRangeDto();
        Number minValue = source.getMinValue();
        Number maxValue = source.getMaxValue();

        dto.setMin(minValue);
        dto.setMax(maxValue);
        dto.setMinIncluded(source.isMinIncluded());
        dto.setMaxIncluded(source.isMaxIncluded());
        return dto;
    }

    @SuppressWarnings("rawtypes")
    default NumberRange dtoToNumberRange(NumberRangeDto source) {
        if (source == null) return null;
        boolean minIncluded = source.isMinIncluded();
        boolean maxIncluded = source.isMaxIncluded();
        Number min = source.getMin();
        Number max = source.getMax();

        if (Long.class.isInstance(min) || Long.class.isInstance(max))
            return NumberRange.create(min.longValue(), minIncluded, max.longValue(), maxIncluded);
        if (Double.class.isInstance(min) || Double.class.isInstance(max))
            return NumberRange.create(
                    min.doubleValue(), minIncluded, max.doubleValue(), maxIncluded);
        if (Float.class.isInstance(min) || Float.class.isInstance(max))
            return NumberRange.create(min.floatValue(), minIncluded, max.floatValue(), maxIncluded);
        if (Integer.class.isInstance(min) || Integer.class.isInstance(max))
            return NumberRange.create(min.intValue(), minIncluded, max.intValue(), maxIncluded);
        if (Short.class.isInstance(min) || Short.class.isInstance(max))
            return NumberRange.create(min.shortValue(), minIncluded, max.shortValue(), maxIncluded);
        if (Byte.class.isInstance(min) || Byte.class.isInstance(max))
            return NumberRange.create(min.byteValue(), minIncluded, max.byteValue(), maxIncluded);

        return NumberRange.create(min.doubleValue(), minIncluded, max.doubleValue(), maxIncluded);
    }

    default String measureToString(Measure value) {
        try {
            return measure2Str.convert(value, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default Measure stringToMeasure(String value) {
        try {
            return str2Measure.convert(value, Measure.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** @see XStreamPersister#GridGeometry2DConverter */
    default GridGeometry dtoToGridGeometry2D(GridGeometryDto value) {
        if (value == null) return null;
        CoordinateReferenceSystem crs = Mappers.getMapper(SharedMappers.class).crs(value.getCrs());
        int[] high = value.getHigh();
        int[] low = value.getLow();
        MathTransform gridToCRS = affineTransform(value.getTransform());
        GridEnvelope gridRange;
        gridRange = new GeneralGridEnvelope(low, high);
        return new GridGeometry2D(gridRange, gridToCRS, crs);
    }

    default GridGeometryDto gridGeometry2DToDto(GridGeometry value) {
        if (value == null) return null;

        GridGeometryDto dto = new GridGeometryDto();
        GridGeometry2D g = (GridGeometry2D) value;

        final int dimension = g.getGridRange().getDimension();
        dto.setLow(new int[dimension]);
        dto.setHigh(new int[dimension]);
        for (int r = 0; r < dimension; r++) {
            dto.getLow()[r] = g.getGridRange().getLow(r);
            dto.getHigh()[r] = g.getGridRange().getHigh(r) + 1;
        }

        dto.setTransform(affineTransform(g.getGridToCRS()));
        CRS crs = Mappers.getMapper(SharedMappers.class).crs(g.getCoordinateReferenceSystem());
        dto.setCrs(crs);
        return dto;
    }

    default double[] affineTransform(MathTransform tx) {
        double[] flatmatrix = null;
        if (tx instanceof AffineTransform) {
            AffineTransform atx = (AffineTransform) tx;
            flatmatrix = new double[6];
            atx.getMatrix(flatmatrix);
        }
        return flatmatrix;
    }

    default MathTransform affineTransform(double[] flatmatrix) {
        if (flatmatrix == null) return null;
        AffineTransform affineTransform = new AffineTransform(flatmatrix);
        return new AffineTransform2D(affineTransform);
    }

    AttributeType infoToDto(AttributeTypeInfo o);

    @Mapping(target = "featureType", ignore = true)
    @Mapping(target = "attribute", ignore = true)
    AttributeTypeInfo dtoToInfo(AttributeType o);

    Attribution infoToDto(AttributionInfo info);

    AttributionInfo dtoToInfo(Attribution dto);

    AuthorityURL infoToDto(AuthorityURLInfo info);

    AuthorityURLInfo dtoToInfo(AuthorityURL authorityURL);

    @Mapping(target = "dimensionType", expression = "java(info.getDimensionType().name())")
    CoverageDimension infoToDto(CoverageDimensionInfo info);

    CoverageDimensionInfo dtoToInfo(CoverageDimension info);

    default SampleDimensionType fromString(String value) {
        return value == null ? null : SampleDimensionType.valueOf(value);
    }

    default String toString(SampleDimensionType type) {
        return type == null ? null : type.name();
    }

    /** Stored in {@link FeatureTypeInfo#getMetadata()} */
    @Mapping(target = "defaultValueStrategy", source = "defaultValue.strategyType")
    @Mapping(target = "defaultValueReferenceValue", source = "defaultValue.referenceValue")
    Dimension infoToDto(DimensionInfo info);

    @InheritInverseConfiguration
    DimensionInfo dtoToInfo(Dimension info);

    DataLink infoToDto(DataLinkInfo info);

    DataLinkInfo dtoToInfo(DataLink dto);

    LayerIdentifier infoToDto(LayerIdentifierInfo info);

    LayerIdentifierInfo dtoToInfo(LayerIdentifier dto);

    Legend infoToDto(LegendInfo info);

    LegendInfo dtoToInfo(Legend info);

    MetadataLink infoToDto(MetadataLinkInfo info);

    MetadataLinkInfo dtoToInfo(MetadataLink dto);

    VirtualTableDto virtualTableToDto(VirtualTable value);

    default VirtualTable dtoToVirtualTable(VirtualTableDto dto) {
        if (dto == null) return null;
        return new VirtualTable(dto.getName(), dto.getSql(), dto.isEscapeSql());
    }

    // there's no implementation for ImagingInfo and ImageFormatInfo, looks like dead code
    // ImageFormatInfo infoToDto();
}
