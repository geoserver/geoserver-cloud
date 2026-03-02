/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.mapper;

import java.util.function.Supplier;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataLinkInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.MapInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WMTSLayerInfoImpl;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.jackson.databind.catalog.dto.CoverageDimensionInfoDto;
import org.geoserver.jackson.databind.catalog.dto.CoverageInfoDto;
import org.geoserver.jackson.databind.catalog.dto.CoverageStoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.DataStoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.FeatureTypeInfoDto;
import org.geoserver.jackson.databind.catalog.dto.KeywordInfoDto;
import org.geoserver.jackson.databind.catalog.dto.LayerGroupInfoDto;
import org.geoserver.jackson.databind.catalog.dto.LayerInfoDto;
import org.geoserver.jackson.databind.catalog.dto.LegendInfoDto;
import org.geoserver.jackson.databind.catalog.dto.MapInfoDto;
import org.geoserver.jackson.databind.catalog.dto.MetadataLinkInfoDto;
import org.geoserver.jackson.databind.catalog.dto.NamespaceInfoDto;
import org.geoserver.jackson.databind.catalog.dto.StyleInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMSLayerInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMSStoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMTSLayerInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMTSStoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WorkspaceInfoDto;
import org.geoserver.ows.util.OwsUtils;
import org.mapstruct.ObjectFactory;
import org.mapstruct.TargetType;

/**
 * Auto-wired object factory for Catalog info interfaces, so the mapstruct code-generated mappers
 * know how to instantiate them
 */
public class ObjectFacotries {

    public @ObjectFactory WorkspaceInfo workspaceInfo(WorkspaceInfoDto source, @TargetType Class<WorkspaceInfo> type) {
        return create(source.getId(), WorkspaceInfoImpl::new);
    }

    public @ObjectFactory NamespaceInfo namespaceInfo(NamespaceInfoDto source, @TargetType Class<NamespaceInfo> type) {
        return create(source.getId(), NamespaceInfoImpl::new);
    }

    public @ObjectFactory DataStoreInfo dataStoreInfo(DataStoreInfoDto source, @TargetType Class<DataStoreInfo> type) {
        return create(source.getId(), () -> new DataStoreInfoImpl((Catalog) null));
    }

    public @ObjectFactory CoverageStoreInfo coverageStoreInfo(
            CoverageStoreInfoDto source, @TargetType Class<CoverageStoreInfo> type) {
        return create(source.getId(), () -> new CoverageStoreInfoImpl((Catalog) null));
    }

    public @ObjectFactory WMSStoreInfo wmsStoreInfo(WMSStoreInfoDto source, @TargetType Class<WMSStoreInfo> type) {
        return create(source.getId(), () -> new WMSStoreInfoImpl((Catalog) null));
    }

    public @ObjectFactory WMTSStoreInfo wmtsStoreInfo(WMTSStoreInfoDto source, @TargetType Class<WMTSStoreInfo> type) {
        return create(source.getId(), () -> new WMTSStoreInfoImpl((Catalog) null));
    }

    public @ObjectFactory FeatureTypeInfo featureTypeInfo(
            FeatureTypeInfoDto source, @TargetType Class<FeatureTypeInfo> type) {
        return create(source.getId(), () -> new FeatureTypeInfoImpl((Catalog) null));
    }

    public @ObjectFactory CoverageInfo coverageInfo(CoverageInfoDto source, @TargetType Class<CoverageInfo> type) {
        return create(source.getId(), () -> new CoverageInfoImpl((Catalog) null));
    }

    public @ObjectFactory WMSLayerInfo wmsLayerInfo(WMSLayerInfoDto source, @TargetType Class<WMSLayerInfo> type) {
        return create(source.getId(), () -> new WMSLayerInfoImpl((Catalog) null));
    }

    public @ObjectFactory WMTSLayerInfo wmtsLayerInfo(WMTSLayerInfoDto source, @TargetType Class<WMTSLayerInfo> type) {
        return create(source.getId(), () -> new WMTSLayerInfoImpl((Catalog) null));
    }

    public @ObjectFactory LayerInfo layerInfo(LayerInfoDto source, @TargetType Class<LayerInfo> type) {
        return create(source.getId(), LayerInfoImpl::new);
    }

    public @ObjectFactory LayerGroupInfo layerGroupInfo(
            LayerGroupInfoDto source, @TargetType Class<LayerGroupInfo> type) {
        return create(source.getId(), LayerGroupInfoImpl::new);
    }

    public @ObjectFactory LayerGroupStyle layerGroupStyle( //
            org.geoserver.jackson.databind.catalog.dto.LayerGroupStyleDto source, //
            @TargetType Class<LayerGroupStyle> type) {

        return create(source.getId(), LayerGroupStyleImpl::new);
    }

    public @ObjectFactory StyleInfo styleInfo(StyleInfoDto source, @TargetType Class<StyleInfo> type) {
        return create(source.getId(), () -> new StyleInfoImpl((Catalog) null));
    }

    public @ObjectFactory MapInfo mapInfo(MapInfoDto source, @TargetType Class<MapInfo> type) {
        return create(source.getId(), MapInfoImpl::new);
    }

    private <T extends Info> T create(String id, Supplier<T> factoryMethod) {
        T info = factoryMethod.get();
        OwsUtils.set(info, "id", id);
        return info;
    }

    public @ObjectFactory LegendInfo legendInfo(LegendInfoDto source) {
        LegendInfoImpl l = new LegendInfoImpl();
        l.setId(source.getId());
        return l;
    }

    public @ObjectFactory MetadataLinkInfo metadataLinkInfo(MetadataLinkInfoDto source) {
        MetadataLinkInfoImpl l = new MetadataLinkInfoImpl();
        l.setId(source.getId());
        return l;
    }

    public @ObjectFactory DataLinkInfo dataLinkInfo() {
        return new DataLinkInfoImpl();
    }

    public @ObjectFactory AttributionInfo attributionInfo() {
        return new AttributionInfoImpl();
    }

    public @ObjectFactory AttributeTypeInfo attributeTypeInfo() {
        return new AttributeTypeInfoImpl();
    }

    public @ObjectFactory CoverageDimensionInfo coverageDimensionInfo(CoverageDimensionInfoDto dto) {
        CoverageDimensionImpl impl = new CoverageDimensionImpl();
        impl.setId(dto.getId());
        return impl;
    }

    public @ObjectFactory DimensionInfo dimensionInfo() {
        return new DimensionInfoImpl();
    }

    public @ObjectFactory KeywordInfo keywordInfo(KeywordInfoDto source) {
        return new org.geoserver.catalog.Keyword(source.getValue());
    }

    public @ObjectFactory MetadataLinkInfo metadataLinkInfo() {
        return new MetadataLinkInfoImpl();
    }

    public @ObjectFactory AuthorityURLInfo authorityURLInfo() {
        return new org.geoserver.catalog.impl.AuthorityURL();
    }

    public @ObjectFactory LayerIdentifierInfo layerIdentifierInfo() {
        return new org.geoserver.catalog.impl.LayerIdentifier();
    }
}
