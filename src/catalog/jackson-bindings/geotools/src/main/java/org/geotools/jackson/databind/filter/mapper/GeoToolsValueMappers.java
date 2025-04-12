/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.data.util.MeasureConverterFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jackson.databind.dto.CRS;
import org.geotools.jackson.databind.dto.Envelope;
import org.geotools.jackson.databind.dto.NumberRangeDto;
import org.geotools.jackson.databind.dto.VersionDto;
import org.geotools.jackson.databind.filter.dto.Filter.MultiValuedFilter.MatchAction;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.wkt.Formattable;
import org.geotools.util.Converters;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.Version;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.NamespaceSupport;

@Mapper(componentModel = "default", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Generated.class)
@Slf4j
public abstract class GeoToolsValueMappers {

    private static final org.geotools.util.Converter str2Measure =
            new MeasureConverterFactory().createConverter(String.class, Measure.class, null);
    private static final org.geotools.util.Converter measure2Str =
            new MeasureConverterFactory().createConverter(Measure.class, String.class, null);

    public abstract MatchAction matchAction(org.geotools.api.filter.MultiValuedFilter.MatchAction matchAction);

    public abstract org.geotools.api.filter.MultiValuedFilter.MatchAction matchAction(MatchAction matchAction);

    public NamespaceSupport map(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        NamespaceSupport s = new NamespaceSupport();
        map.forEach(s::declarePrefix);
        return s;
    }

    public Map<String, String> map(NamespaceSupport ns) {
        if (ns == null) {
            return null;
        }

        Map<String, String> map = new HashMap<>();
        Collections.list(ns.getPrefixes()).forEach(prefix -> map.put(prefix, ns.getURI(prefix)));
        return map;
    }

    public InternationalString map(String message) {
        return convert(message, SimpleInternationalString::new);
    }

    public String map(InternationalString string) {
        return convert(string, InternationalString::toString);
    }

    /**
     * @return {@code null} if {@code clazz} is null, the {@link Class#getCanonicalName() canonical
     *     name} otherwise. May include a {@code []} suffix for array classes
     */
    public String classToCanonicalName(Class<?> clazz) {
        return ClassUtils.getCanonicalName(clazz, null);
    }

    /**
     * Returns the (initialized) class represented by {@code className} using the current thread's
     * context class loader. This implementation supports the syntaxes "{@code
     * java.util.Map.Entry[]}", "{@code java.util.Map$Entry[]}", "{@code [Ljava.util.Map.Entry;}",
     * and "{@code [Ljava.util.Map$Entry;}".
     *
     * @param className the class name
     * @return the class represented by {@code className} using the current thread's context class
     *     loader
     * @throws IllegalArgumentException if the class is not found
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> canonicalNameToClass(String value) {
        try {
            return null == value ? null : (Class<T>) ClassUtils.getClass(value);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <F, T> T convert(F value, Function<F, T> nonnNullMapper) {
        return value == null ? null : nonnNullMapper.apply(value);
    }

    public String awtColorToString(java.awt.Color color) {
        if (null == color) {
            return null;
        }
        return Converters.convert(color, String.class);
    }

    public java.awt.Color stringToAwtColor(String color) {
        if (null == color) {
            return null;
        }
        return Converters.convert(color, java.awt.Color.class);
    }

    public CoordinateReferenceSystem crs(CRS source) {
        if (source == null) {
            return null;
        }
        try {
            if (null != source.getSrs()) {
                String srs = source.getSrs();
                boolean longitudeFirst = srs.startsWith("EPSG:");
                return org.geotools.referencing.CRS.decode(source.getSrs(), longitudeFirst);
            }
            return org.geotools.referencing.CRS.parseWKT(source.getWKT());
        } catch (FactoryException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public CRS crs(CoordinateReferenceSystem source) {
        if (source == null) {
            return null;
        }
        CRS crs = new CRS();

        String srs = null;
        AxisOrder axisOrder = org.geotools.referencing.CRS.getAxisOrder(source, false);
        try {
            boolean fullScan = false;
            Integer code = org.geotools.referencing.CRS.lookupEpsgCode(source, fullScan);
            if (code != null) {
                if (axisOrder == AxisOrder.NORTH_EAST) {
                    srs = "urn:ogc:def:crs:EPSG::%s".formatted(code);
                } else {
                    srs = "EPSG:%s".formatted(code);
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
        if (env == null) {
            return null;
        }
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
        if (source == null) {
            return null;
        }
        CoordinateReferenceSystem crs = crs(source.getCrs());
        ReferencedEnvelope env = new ReferencedEnvelope(crs);
        double[] coords = source.getCoordinates();
        env.init(coords[0], coords[1], coords[2], coords[3]);
        return env;
    }

    public abstract org.geotools.jackson.databind.dto.NameDto map(org.geotools.api.feature.type.Name name);

    public Name map(org.geotools.jackson.databind.dto.NameDto dto) {
        return new NameImpl(dto.getNamespaceURI(), dto.getLocalPart());
    }

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

    public String localeToString(Locale locale) {
        return locale == null ? "" : locale.toLanguageTag();
    }

    public Locale stringToLocale(String s) {
        return s == null || s.isBlank() ? null : Locale.forLanguageTag(s);
    }

    public Map<String, String> internationalStringToDto(InternationalString s) {
        if (s instanceof GrowableInternationalString gs) {
            Set<Locale> locales = gs.getLocales();
            Map<String, String> dto = HashMap.newHashMap(locales.size());
            locales.forEach(locale -> dto.put(localeToString(locale), gs.toString(locale)));
            return dto;
        }
        if (s instanceof SimpleInternationalString) {
            return Map.of("", s.toString());
        }
        if (s == null) {
            return null;
        }

        LoggerFactory.getLogger(getClass())
                .warn(
                        "Unknown InternationalString implementation: {}. Returning the default value",
                        s.getClass().getName());
        return Map.of("", s.toString());
    }

    public GrowableInternationalString dtoToInternationalString(Map<String, String> s) {
        if (s == null) {
            return null;
        }
        GrowableInternationalString gs = new GrowableInternationalString();
        s.forEach((locale, value) -> gs.add(stringToLocale(locale), value));
        return gs;
    }

    public String measureToString(Measure value) {
        try {
            return measure2Str.convert(value, String.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Measure stringToMeasure(String value) {
        try {
            return str2Measure.convert(value, Measure.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public NumberRangeDto numberRangeToDto(NumberRange<?> source) {
        if (source == null) {
            return null;
        }
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
    public NumberRange dtoToNumberRange(NumberRangeDto source) {
        if (source == null) {
            return null;
        }
        boolean minIncluded = source.isMinIncluded();
        boolean maxIncluded = source.isMaxIncluded();
        Number min = source.getMin();
        Number max = source.getMax();

        if (min instanceof Long || max instanceof Long) {
            return NumberRange.create(min.longValue(), minIncluded, max.longValue(), maxIncluded);
        } else if (min instanceof Double || max instanceof Double) {
            return NumberRange.create(min.doubleValue(), minIncluded, max.doubleValue(), maxIncluded);
        } else if (min instanceof Float || max instanceof Float) {
            return NumberRange.create(min.floatValue(), minIncluded, max.floatValue(), maxIncluded);
        } else if (min instanceof Integer || max instanceof Integer) {
            return NumberRange.create(min.intValue(), minIncluded, max.intValue(), maxIncluded);
        } else if (min instanceof Short || max instanceof Short) {
            return NumberRange.create(min.shortValue(), minIncluded, max.shortValue(), maxIncluded);
        } else if (min instanceof Byte || max instanceof Byte) {
            return NumberRange.create(min.byteValue(), minIncluded, max.byteValue(), maxIncluded);
        }
        return NumberRange.create(min.doubleValue(), minIncluded, max.doubleValue(), maxIncluded);
    }
}
