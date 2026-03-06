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
    @JsonSubTypes.Type(value = ExpressionDto.PropertyNameDto.class),
    @JsonSubTypes.Type(value = LiteralDto.class),
    @JsonSubTypes.Type(value = ExpressionDto.FunctionDto.class),
    @JsonSubTypes.Type(value = ExpressionDto.BinaryExpressionDto.class)
})
@Data
@Accessors(chain = true)
public abstract class ExpressionDto {

    protected ExpressionDto() {
        // default constructor
    }

    /** DTO dfor {@link org.geotools.api.filter.expression.PropertyName} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("PropertyName")
    public static class PropertyNameDto extends ExpressionDto {
        @SuppressWarnings("java:S1700")
        private String propertyName;

        private Map<String, String> namespaceContext;
    }

    /** Base abstract DTO for BinaryExpression */
    @JsonSubTypes({
        @JsonSubTypes.Type(value = AddDto.class, name = "Add"),
        @JsonSubTypes.Type(value = SubtractDto.class, name = "Subtract"),
        @JsonSubTypes.Type(value = MultiplyDto.class, name = "Multiply"),
        @JsonSubTypes.Type(value = DivideDto.class, name = "Divide")
    })
    @Data
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryExpressionDto extends ExpressionDto {
        private ExpressionDto expression1;
        private ExpressionDto expression2;
    }

    /** DTO for {@link org.geotools.api.filter.expression.Add} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @JsonTypeName("Add")
    public static class AddDto extends BinaryExpressionDto {}

    /** DTO for {@link org.geotools.api.filter.expression.Divide} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @JsonTypeName("Divide")
    public static class DivideDto extends BinaryExpressionDto {}

    /** DTO for {@link org.geotools.api.filter.expression.Multiply} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @JsonTypeName("Multiply")
    public static class MultiplyDto extends BinaryExpressionDto {}

    /** DTO for {@link org.geotools.api.filter.expression.Subtract} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @JsonTypeName("Subtract")
    public static class SubtractDto extends BinaryExpressionDto {}

    /** DTO for {@link org.geotools.api.filter.expression.Function} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @JsonTypeName("Function")
    public static class FunctionDto extends ExpressionDto {
        private String name;
        private List<ExpressionDto> parameters = new ArrayList<>();
    }

    /** DTO for {@link org.geotools.api.filter.capability.FunctionName} */
    @Data
    @JsonTypeName("FunctionName")
    public static class FunctionNameDto {
        private String name;
        private int argumentCount;
        private List<String> argumentNames = new ArrayList<>();
    }
}
