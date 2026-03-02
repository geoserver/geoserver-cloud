/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.PropertyNameDto;
import org.geotools.jackson.databind.filter.dto.LiteralDto;
import org.mapstruct.ObjectFactory;
import org.mapstruct.factory.Mappers;
import org.xml.sax.helpers.NamespaceSupport;

public class ExpressionFactory {

    private final FilterFactory factory = CommonFactoryFinder.getFilterFactory();

    public @ObjectFactory org.geotools.api.filter.expression.PropertyName propertyName(PropertyNameDto source) {
        GeoToolsValueMappers values = Mappers.getMapper(GeoToolsValueMappers.class);
        String localName = source.getPropertyName();
        NamespaceSupport namespaceSupport = values.mapToNamespaceSupport(source.getNamespaceContext());
        return factory.property(localName, namespaceSupport);
    }

    public @ObjectFactory org.geotools.api.filter.expression.Literal literal(LiteralDto source) {
        return factory.literal(source.getValue());
    }
}
