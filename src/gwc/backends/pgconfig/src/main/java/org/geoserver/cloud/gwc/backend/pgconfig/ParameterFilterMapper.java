/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import lombok.Generated;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.FloatParamFilter;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.IntegerParamFilter;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.ParamFilter;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.RegexParamFilter;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.StringParamFilter;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.StyleParamFilter;
import org.geoserver.gwc.layer.StyleParameterFilter;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.IntegerParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * @since 1.7
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Generated.class)
interface ParameterFilterMapper {

    default ParameterFilter map(ParamFilter filter) {
        if (null == filter) {
            return null;
        }
        if (filter instanceof RegexParamFilter regex) {
            return map(regex);
        }
        if (filter instanceof StringParamFilter s) {
            return map(s);
        }
        if (filter instanceof FloatParamFilter f) {
            return map(f);
        }
        if (filter instanceof IntegerParamFilter i) {
            return map(i);
        }
        if (filter instanceof StyleParamFilter s) {
            return map(s);
        }
        throw new IllegalArgumentException(
                "Unknown ParamFilter type " + filter.getClass().getName());
    }

    default ParamFilter map(ParameterFilter filter) {
        if (null == filter) {
            return null;
        }
        if (filter instanceof RegexParameterFilter regex) {
            return map(regex);
        }
        if (filter instanceof StringParameterFilter s) {
            return map(s);
        }
        if (filter instanceof FloatParameterFilter f) {
            return map(f);
        }
        if (filter instanceof IntegerParameterFilter i) {
            return map(i);
        }
        if (filter instanceof StyleParameterFilter s) {
            return map(s);
        }
        throw new IllegalArgumentException(
                "Unknown ParameterFilter type " + filter.getClass().getName());
    }

    @Mapping(source = "normalizerCase", target = "normalize.case")
    @Mapping(source = "normalizerLocale", target = "normalize.configuredLocale")
    @Mapping(target = "legalValues", ignore = true)
    @Mapping(target = "values", ignore = true)
    RegexParameterFilter map(RegexParamFilter filter);

    @Mapping(target = "normalizerCase", source = "normalize.case")
    @Mapping(target = "normalizerLocale", source = "normalize.configuredLocale")
    RegexParamFilter map(RegexParameterFilter filter);

    @Mapping(source = "normalizerCase", target = "normalize.case")
    @Mapping(source = "normalizerLocale", target = "normalize.configuredLocale")
    @Mapping(target = "legalValues", ignore = true)
    StringParameterFilter map(StringParamFilter filter);

    @Mapping(target = "normalizerCase", source = "normalize.case")
    @Mapping(target = "normalizerLocale", source = "normalize.configuredLocale")
    StringParamFilter map(StringParameterFilter filter);

    @Mapping(target = "legalValues", ignore = true)
    FloatParameterFilter map(FloatParamFilter filter);

    FloatParamFilter map(FloatParameterFilter filter);

    @Mapping(target = "legalValues", ignore = true)
    IntegerParameterFilter map(IntegerParamFilter filter);

    IntegerParamFilter map(IntegerParameterFilter filter);

    @Mapping(target = "legalValues", ignore = true)
    @Mapping(target = "layer", ignore = true)
    @Mapping(target = "realDefault", ignore = true)
    @Mapping(target = "layerStyles", ignore = true)
    StyleParameterFilter map(StyleParamFilter filter);

    StyleParamFilter map(StyleParameterFilter filter);
}
