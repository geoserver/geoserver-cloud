/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.mapper;

import java.awt.geom.AffineTransform;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Generated;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.jackson.databind.catalog.dto.AttributeTypeInfoDto;
import org.geoserver.jackson.databind.catalog.dto.AuthorityURLInfoDto;
import org.geoserver.jackson.databind.catalog.dto.CoverageDimensionInfoDto;
import org.geoserver.jackson.databind.catalog.dto.DataLinkInfoDto;
import org.geoserver.jackson.databind.catalog.dto.DimensionInfoDto;
import org.geoserver.jackson.databind.catalog.dto.GridGeometryDto;
import org.geoserver.jackson.databind.catalog.dto.KeywordInfoDto;
import org.geoserver.jackson.databind.catalog.dto.LayerIdentifierInfoDto;
import org.geoserver.jackson.databind.catalog.dto.LegendInfoDto;
import org.geoserver.jackson.databind.catalog.dto.MetadataLinkInfoDto;
import org.geoserver.jackson.databind.catalog.dto.MetadataMapDto;
import org.geoserver.jackson.databind.catalog.dto.QueryDto;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableDto;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableParameterDto;
import org.geoserver.jackson.databind.mapper.ResolvingProxyMapper;
import org.geoserver.wfs.GMLInfo;
import org.geotools.api.coverage.SampleDimensionType;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.jackson.databind.dto.CoordinateReferenceSystemDto;
import org.geotools.jackson.databind.filter.dto.LiteralDto;
import org.geotools.jackson.databind.filter.dto.SortByDto;
import org.geotools.jackson.databind.filter.mapper.FilterMapper;
import org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.locationtech.jts.geom.Geometry;
import org.mapstruct.AnnotateWith;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "default",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {GeoToolsValueMappers.class, ObjectFacotries.class, ResolvingProxyMapper.class, FilterMapper.class})
@AnnotateWith(value = Generated.class)
public interface GeoServerValueObjectsMapper {

    @Mapping(target = "withFilter", ignore = true)
    @SuppressWarnings("rawtypes")
    Query dtoToQuery(QueryDto dto);

    QueryDto queryToDto(@SuppressWarnings("rawtypes") Query query);

    List<SortBy> sortByListToSortByDtoList(List<SortByDto> dtos);

    List<SortByDto> sortByDtoListToSortByList(List<SortBy> sortBy);

    /**
     * @see XStreamPersister#GridGeometry2DConverter
     */
    default GridGeometry dtoToGridGeometry2D(GridGeometryDto value) {
        if (value == null) {
            return null;
        }
        CoordinateReferenceSystem crs = Mappers.getMapper(
                        org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers.class)
                .dtoToCrs(value.getCrs());
        int[] high = value.getHigh();
        int[] low = value.getLow();
        MathTransform gridToCRS = affineTransform(value.getTransform());
        GridEnvelope gridRange;
        gridRange = new GeneralGridEnvelope(low, high);
        return new GridGeometry2D(gridRange, gridToCRS, crs);
    }

    default GridGeometryDto gridGeometry2DToDto(GridGeometry value) {
        if (value == null) {
            return null;
        }

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
        CoordinateReferenceSystemDto crs = Mappers.getMapper(
                        org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers.class)
                .crsToDto(g.getCoordinateReferenceSystem());
        dto.setCrs(crs);
        return dto;
    }

    default double[] affineTransform(MathTransform tx) {
        double[] flatmatrix = null;
        if (tx instanceof AffineTransform atx) {
            flatmatrix = new double[6];
            atx.getMatrix(flatmatrix);
        }
        return flatmatrix;
    }

    default MathTransform affineTransform(double[] flatmatrix) {
        if (flatmatrix == null) {
            return null;
        }
        AffineTransform affineTransform = new AffineTransform(flatmatrix);
        return new AffineTransform2D(affineTransform);
    }

    AttributeTypeInfoDto infoToDto(AttributeTypeInfo o);

    @Mapping(target = "attribute", ignore = true)
    AttributeTypeInfo dtoToInfo(AttributeTypeInfoDto o);

    AttributionInfo infoToDto(AttributionInfo info);

    AttributionInfo dtoToInfo(AttributionInfo dto);

    AuthorityURLInfoDto infoToDto(AuthorityURLInfo info);

    AuthorityURLInfo dtoToInfo(AuthorityURLInfoDto authorityURL);

    CoverageDimensionInfoDto infoToDto(CoverageDimensionInfo info);

    CoverageDimensionInfo dtoToInfo(CoverageDimensionInfoDto info);

    default SampleDimensionType stringToSampleDimensionType(String value) {
        return value == null ? null : SampleDimensionType.valueOf(value);
    }

    default String sampleDimensionTypeToString(SampleDimensionType type) {
        return type == null ? null : type.name();
    }

    /** Stored in {@link FeatureTypeInfo#getMetadata()} */
    @Mapping(target = "defaultValueStrategy", source = "defaultValue.strategyType")
    @Mapping(target = "defaultValueReferenceValue", source = "defaultValue.referenceValue")
    DimensionInfoDto infoToDto(DimensionInfo info);

    @InheritInverseConfiguration
    DimensionInfo dtoToInfo(DimensionInfoDto info);

    DataLinkInfoDto infoToDto(DataLinkInfo info);

    DataLinkInfo dtoToInfo(DataLinkInfoDto dto);

    LayerIdentifierInfoDto infoToDto(LayerIdentifierInfo info);

    LayerIdentifierInfo dtoToInfo(LayerIdentifierInfoDto dto);

    LegendInfoDto infoToDto(LegendInfo info);

    LegendInfoImpl dtoToInfo(LegendInfoDto info);

    MetadataLinkInfoDto infoToDto(MetadataLinkInfo info);

    MetadataLinkInfoImpl dtoToInfo(MetadataLinkInfoDto dto);

    default VirtualTableDto virtualTableToDto(VirtualTable value) {
        if (value == null) {
            return null;
        }
        VirtualTableDto dto = new VirtualTableDto();
        dto.setName(value.getName());
        dto.setSql(value.getSql());
        dto.setEscapeSql(value.isEscapeSql());
        dto.setPrimaryKeyColumns(new ArrayList<>(value.getPrimaryKeyColumns()));

        // Map geometry types
        Map<String, Class<? extends Geometry>> geometryTypesDto = new HashMap<>();
        value.getGeometries().forEach(geometryName -> {
            Class<? extends Geometry> geometryType = value.getGeometryType(geometryName);
            geometryTypesDto.put(geometryName, geometryType);
        });
        dto.setGeometryTypes(geometryTypesDto);

        // Map native SRIDs
        Map<String, Integer> nativeSridsDto = new HashMap<>();
        value.getGeometries().forEach(geometryName -> {
            int srid = value.getNativeSrid(geometryName);
            if (srid != -1) {
                nativeSridsDto.put(geometryName, srid);
            }
        });
        dto.setNativeSrids(nativeSridsDto);

        // Map dimensions
        Map<String, Integer> dimensionsDto = new HashMap<>();
        value.getGeometries().forEach(geometryName -> {
            int dimension = value.getDimension(geometryName);
            if (dimension != 2) { // Only store non-default values
                dimensionsDto.put(geometryName, dimension);
            }
        });
        dto.setDimensions(dimensionsDto);

        // Map parameters
        Map<String, VirtualTableParameterDto> parametersDto = new HashMap<>();
        value.getParameterNames().forEach(paramName -> {
            VirtualTableParameter param = value.getParameter(paramName);
            if (param != null) {
                parametersDto.put(paramName, virtualTableParameterToDto(param));
            }
        });
        dto.setParameters(parametersDto);

        return dto;
    }

    @SuppressWarnings("java:S3776")
    default VirtualTable dtoToVirtualTable(VirtualTableDto dto) {
        if (dto == null) {
            return null;
        }
        VirtualTable vt = new VirtualTable(dto.getName(), dto.getSql(), dto.isEscapeSql());

        // Set primary key columns
        if (dto.getPrimaryKeyColumns() != null) {
            vt.setPrimaryKeyColumns(new ArrayList<>(dto.getPrimaryKeyColumns()));
        }

        // Map geometry types, native SRIDs, and dimensions
        if (dto.getGeometryTypes() != null) {
            dto.getGeometryTypes().forEach((geometryName, geometryType) -> {
                if (geometryType != null) {
                    int nativeSrid =
                            dto.getNativeSrids() != null ? dto.getNativeSrids().getOrDefault(geometryName, -1) : -1;
                    int dimension =
                            dto.getDimensions() != null ? dto.getDimensions().getOrDefault(geometryName, 2) : 2;
                    vt.addGeometryMetadatata(geometryName, geometryType, nativeSrid, dimension);
                }
            });
        }

        // Map parameters
        if (dto.getParameters() != null) {
            dto.getParameters().forEach((paramName, paramDto) -> {
                VirtualTableParameter param = dtoToVirtualTableParameter(paramDto);
                if (param != null) {
                    vt.addParameter(param);
                }
            });
        }

        return vt;
    }

    default VirtualTableParameterDto virtualTableParameterToDto(VirtualTableParameter param) {
        if (param == null) {
            return null;
        }
        VirtualTableParameterDto dto = new VirtualTableParameterDto();
        dto.setName(param.getName());
        dto.setDefaultValue(param.getDefaultValue());

        // Convert validator to string representation for DTO
        if (param.getValidator() instanceof RegexpValidator regexpValidator) {
            dto.setValidator(regexpValidator.getPattern().pattern());
        }

        return dto;
    }

    default VirtualTableParameter dtoToVirtualTableParameter(VirtualTableParameterDto dto) {
        if (dto == null) {
            return null;
        }
        VirtualTableParameter param = new VirtualTableParameter(dto.getName(), dto.getDefaultValue());

        // Convert validator from DTO string back to proper RegexpValidator
        if (dto.getValidator() != null && !dto.getValidator().trim().isEmpty()) {
            param.setValidator(new RegexpValidator(dto.getValidator()));
        }

        return param;
    }

    default MetadataMapDto metadataMap(MetadataMap md) {
        if (md == null) {
            return null;
        }
        MetadataMapDto dto = new MetadataMapDto();
        md.forEach((k, v) -> {
            LiteralDto l = LiteralDto.valueOf(v);
            dto.put(k, l);
        });
        return dto;
    }

    @SuppressWarnings("java:S1168") // returning null on purpose if dto is null
    default MetadataMap metadataMap(MetadataMapDto dto) {
        if (dto == null) {
            return null;
        }
        MetadataMap md = new MetadataMap();
        dto.forEach((k, l) -> {
            Object v = l.getValue();
            md.put(k, (Serializable) v);
        });
        return md;
    }

    KeywordInfo keyword(KeywordInfoDto dto);

    KeywordInfoDto keyword(KeywordInfo keyword);

    /** Added due to {@link GMLInfo#getMimeTypeToForce()} */
    default String optToString(Optional<String> value) {
        return value.orElse(null);
    }

    default Optional<String> stringToOpt(String value) {
        return Optional.ofNullable(value);
    }
}
