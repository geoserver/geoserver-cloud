/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.mapper;

import java.awt.geom.AffineTransform;
import java.io.Serializable;
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
import org.geoserver.catalog.plugin.Query;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.jackson.databind.catalog.dto.AttributeType;
import org.geoserver.jackson.databind.catalog.dto.Attribution;
import org.geoserver.jackson.databind.catalog.dto.AuthorityURL;
import org.geoserver.jackson.databind.catalog.dto.CoverageDimension;
import org.geoserver.jackson.databind.catalog.dto.DataLink;
import org.geoserver.jackson.databind.catalog.dto.Dimension;
import org.geoserver.jackson.databind.catalog.dto.GridGeometryDto;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.jackson.databind.catalog.dto.LayerIdentifier;
import org.geoserver.jackson.databind.catalog.dto.Legend;
import org.geoserver.jackson.databind.catalog.dto.MetadataLink;
import org.geoserver.jackson.databind.catalog.dto.MetadataMapDto;
import org.geoserver.jackson.databind.catalog.dto.QueryDto;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableDto;
import org.geoserver.jackson.databind.mapper.InfoReferenceMapper;
import org.geoserver.wfs.GMLInfo;
import org.geotools.api.coverage.SampleDimensionType;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.jackson.databind.dto.CRS;
import org.geotools.jackson.databind.filter.dto.Literal;
import org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers;
import org.geotools.jdbc.VirtualTable;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapstruct.AnnotateWith;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "default",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {GeoToolsValueMappers.class, ObjectFacotries.class, InfoReferenceMapper.class})
@AnnotateWith(value = Generated.class)
public interface GeoServerValueObjectsMapper {

    @Mapping(target = "withFilter", ignore = true)
    @SuppressWarnings("rawtypes")
    Query dtoToQuery(QueryDto dto);

    QueryDto queryToDto(@SuppressWarnings("rawtypes") Query query);

    /**
     * @see XStreamPersister#GridGeometry2DConverter
     */
    default GridGeometry dtoToGridGeometry2D(GridGeometryDto value) {
        if (value == null) {
            return null;
        }
        CoordinateReferenceSystem crs = Mappers.getMapper(
                        org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers.class)
                .crs(value.getCrs());
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
        CRS crs = Mappers.getMapper(org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers.class)
                .crs(g.getCoordinateReferenceSystem());
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

    AttributeType infoToDto(AttributeTypeInfo o);

    @Mapping(target = "attribute", ignore = true)
    AttributeTypeInfo dtoToInfo(AttributeType o);

    Attribution infoToDto(AttributionInfo info);

    AttributionInfo dtoToInfo(Attribution dto);

    AuthorityURL infoToDto(AuthorityURLInfo info);

    AuthorityURLInfo dtoToInfo(AuthorityURL authorityURL);

    CoverageDimension infoToDto(CoverageDimensionInfo info);

    CoverageDimensionInfo dtoToInfo(CoverageDimension info);

    default SampleDimensionType stringToSampleDimensionType(String value) {
        return value == null ? null : SampleDimensionType.valueOf(value);
    }

    default String sampleDimensionTypeToString(SampleDimensionType type) {
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
        if (dto == null) {
            return null;
        }
        return new VirtualTable(dto.getName(), dto.getSql(), dto.isEscapeSql());
    }

    default MetadataMapDto metadataMap(MetadataMap md) {
        if (md == null) {
            return null;
        }
        MetadataMapDto dto = new MetadataMapDto();
        md.forEach((k, v) -> {
            Literal l = Literal.valueOf(v);
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

    KeywordInfo keyword(Keyword dto);

    Keyword keyword(KeywordInfo keyword);

    /** Added due to {@link GMLInfo#getMimeTypeToForce()} */
    default String optToString(Optional<String> value) {
        return value.orElse(null);
    }

    default Optional<String> stringToOpt(String value) {
        return Optional.ofNullable(value);
    }
}
