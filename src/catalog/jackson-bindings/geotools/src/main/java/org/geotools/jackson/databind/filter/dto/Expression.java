/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Expression.PropertyName.class, name = "PropertyName"),
    @JsonSubTypes.Type(value = Expression.Literal.class, name = "Literal"),
    @JsonSubTypes.Type(value = Expression.Function.class, name = "Function"),
    @JsonSubTypes.Type(value = Expression.BinaryExpression.class, name = "BinaryExpression")
})
@Accessors(chain = true)
public @Data @Generated abstract class Expression {

    @EqualsAndHashCode(callSuper = true)
    public static @Data @Generated class Literal extends Expression {
        /**
         * JsonTypeInfo necessary for jackson to resolve which deserializer to use, adds an "@type"
         * property to the {@code Literal} object, without messing with the value representation.
         */
        //        @JsonTypeInfo(
        //                use = JsonTypeInfo.Id.CLASS,
        //                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        //                property = "@type")
        // REVISIT: @type produces an exception for null values: "Missing external type id property
        // '@type' (and no 'defaultImpl' specified)"
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        private Object value;

        private List<Literal> list;
        private Set<Literal> set;

        public Literal setValue(Object value) {
            if (value instanceof List)
                list =
                        ((List<?>) value)
                                .stream()
                                        .map(v -> new Literal().setValue(v))
                                        .collect(Collectors.toList());
            else if (value instanceof Set)
                set =
                        ((Set<?>) value)
                                .stream()
                                        .map(v -> new Literal().setValue(v))
                                        .collect(Collectors.toCollection(LinkedHashSet::new));
            else this.value = value;
            return this;
        }

        public Object resolveValue() {
            if (set != null)
                return set.stream()
                        .map(Literal::resolveValue)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            if (list != null)
                return list.stream().map(Literal::resolveValue).collect(Collectors.toList());
            return value;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    public static @Data @Generated class PropertyName extends Expression {
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
    public abstract static @Data @Generated class BinaryExpression extends Expression {
        private Expression expression1;
        private Expression expression2;
    }

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data @Generated class Add extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data @Generated class Divide extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data @Generated class Multiply extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data @Generated class Subtract extends BinaryExpression {}

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static @Data @Generated class Function extends Expression {
        private String name;
        private List<Expression> parameters = new ArrayList<>();
    }

    @JsonTypeName("FunctionName")
    public static @Data @Generated class FunctionName {
        private String name;
        private int argumentCount;
        private List<String> argumentNames = new ArrayList<>();
    }
}
