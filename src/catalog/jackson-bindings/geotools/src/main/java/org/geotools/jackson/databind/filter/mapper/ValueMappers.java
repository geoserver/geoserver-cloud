/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import org.geotools.api.util.InternationalString;
import org.geotools.jackson.databind.filter.dto.Filter.MultiValuedFilter.MatchAction;
import org.geotools.util.Converters;
import org.geotools.util.SimpleInternationalString;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Mapper(componentModel = "default", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ValueMappers {

    public abstract MatchAction matchAction(
            org.geotools.api.filter.MultiValuedFilter.MatchAction matchAction);

    public abstract org.geotools.api.filter.MultiValuedFilter.MatchAction matchAction(
            MatchAction matchAction);

    public NamespaceSupport map(Map<String, String> map) {
        if (map == null) return null;
        NamespaceSupport s = new NamespaceSupport();
        map.forEach((prefix, uri) -> s.declarePrefix(prefix, uri));
        return s;
    }

    public Map<String, String> map(NamespaceSupport ns) {
        if (ns == null) return null;

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

    public String map(@SuppressWarnings("rawtypes") Class clazz) {
        return convert(clazz, Class::getCanonicalName);
    }

    public String classToCanonicalName(Class<?> value) {
        return value.getName();
    }

    @SuppressWarnings("rawtypes")
    public Class canonicalNameToClass(String value) {
        if (null == value) return null;

        try {
            return Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <F, T> T convert(F value, Function<F, T> nonnNullMapper) {
        return value == null ? null : nonnNullMapper.apply(value);
    }

    public String awtColorToString(java.awt.Color color) {
        if (null == color) return null;
        return Converters.convert(color, String.class);
    }

    public java.awt.Color stringToAwtColor(String color) {
        if (null == color) return null;
        return Converters.convert(color, java.awt.Color.class);
    }
}
