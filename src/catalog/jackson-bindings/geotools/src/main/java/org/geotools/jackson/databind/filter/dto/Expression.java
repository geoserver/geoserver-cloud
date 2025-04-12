/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
    @JsonSubTypes.Type(value = Literal.class, name = "Literal"),
    @JsonSubTypes.Type(value = Expression.Function.class, name = "Function"),
    @JsonSubTypes.Type(value = Expression.BinaryExpression.class, name = "BinaryExpression")
})
@Data
@Accessors(chain = true)
public abstract class Expression {

    protected Expression() {
        // default constructor
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PropertyName extends Expression {
        private String propertyName;
        private Map<String, String> namespaceContext;
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = Add.class, name = "Add"),
        @JsonSubTypes.Type(value = Subtract.class, name = "Subtract"),
        @JsonSubTypes.Type(value = Multiply.class, name = "Multiply"),
        @JsonSubTypes.Type(value = Divide.class, name = "Divide")
    })
    @Data
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryExpression extends Expression {
        private Expression expression1;
        private Expression expression2;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class Add extends BinaryExpression {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class Divide extends BinaryExpression {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class Multiply extends BinaryExpression {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class Subtract extends BinaryExpression {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class Function extends Expression {
        private String name;
        private List<Expression> parameters = new ArrayList<>();
    }

    @JsonTypeName("FunctionName")
    @Data
    public static class FunctionName {
        private String name;
        private int argumentCount;
        private List<String> argumentNames = new ArrayList<>();
    }
}
