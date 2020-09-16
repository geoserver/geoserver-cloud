package org.geotools.jackson.databind.filter.mapper;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.jackson.databind.filter.dto.Expression.Literal;
import org.geotools.jackson.databind.filter.dto.Expression.PropertyName;
import org.mapstruct.ObjectFactory;
import org.mapstruct.factory.Mappers;
import org.opengis.filter.FilterFactory2;
import org.xml.sax.helpers.NamespaceSupport;

public class ExpressionFactory {

    private final FilterFactory2 factory = CommonFactoryFinder.getFilterFactory2();

    public @ObjectFactory org.opengis.filter.expression.PropertyName propertyName(
            PropertyName source) {
        ValueMappers values = Mappers.getMapper(ValueMappers.class);
        String localName = source.getPropertyName();
        NamespaceSupport namespaceSupport = values.map(source.getNamespaceContext());
        return factory.property(localName, namespaceSupport);
    }

    public @ObjectFactory org.opengis.filter.expression.Literal literal(Literal source) {
        return factory.literal(source.getValue());
    }
}
