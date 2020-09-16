package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Expression.PropertyName.class, name = "PropertyName"),
    @JsonSubTypes.Type(value = Expression.Literal.class, name = "Literal"),
    @JsonSubTypes.Type(value = Expression.Function.class, name = "Function"),
    @JsonSubTypes.Type(value = Expression.BinaryExpression.class, name = "BinaryExpression")
})
@Accessors(chain = true)
public @Data abstract class Expression {

    @EqualsAndHashCode(callSuper = true)
    public static @Data class Literal extends Expression {
        /**
         * JsonTypeInfo necessary for jackson to resolve which deserializer to use, adds an "@type"
         * property to the {@code Literal} object, without messing with the value representation.
         */
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "@type"
        )
        private Object value;
    }

    @EqualsAndHashCode(callSuper = true)
    public static @Data class PropertyName extends Expression {
        private String propertyName;
        private Map<String, String> namespaceContext;
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = Add.class, name = "Add"),
        @JsonSubTypes.Type(value = Subtract.class, name = "Subtract"),
        @JsonSubTypes.Type(value = Multiply.class, name = "Multiply"),
        @JsonSubTypes.Type(value = Divide.class, name = "Divide")
    })
    @EqualsAndHashCode(callSuper = true)
    public abstract static @Data class BinaryExpression extends Expression {
        private Expression expression1;
        private Expression expression2;
    }

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data class Add extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data class Divide extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data class Multiply extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data class Subtract extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data class Function extends Expression {
        private String name;
        private List<Expression> parameters = new ArrayList<>();
    }
}
