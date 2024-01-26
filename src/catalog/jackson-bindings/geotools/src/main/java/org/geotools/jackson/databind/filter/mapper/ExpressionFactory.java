/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.jackson.databind.filter.dto.Expression.PropertyName;
import org.geotools.jackson.databind.filter.dto.Literal;
import org.mapstruct.ObjectFactory;
import org.mapstruct.factory.Mappers;
import org.xml.sax.helpers.NamespaceSupport;

public class ExpressionFactory {

    private final FilterFactory factory = CommonFactoryFinder.getFilterFactory();

    public @ObjectFactory org.geotools.api.filter.expression.PropertyName propertyName(
            PropertyName source) {
        GeoToolsValueMappers values = Mappers.getMapper(GeoToolsValueMappers.class);
        String localName = source.getPropertyName();
        NamespaceSupport namespaceSupport = values.map(source.getNamespaceContext());
        return factory.property(localName, namespaceSupport);
    }

    public @ObjectFactory org.geotools.api.filter.expression.Literal literal(Literal source) {
        return factory.literal(source.getValue());
    }
}
