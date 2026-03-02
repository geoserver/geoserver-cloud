/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import java.util.List;
import lombok.Generated;
import org.geotools.api.filter.expression.ExpressionVisitor;
import org.geotools.api.filter.expression.NilExpression;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFinder;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.jackson.databind.filter.dto.ExpressionDto;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.AddDto;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.DivideDto;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.FunctionDto;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.MultiplyDto;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.PropertyNameDto;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.SubtractDto;
import org.geotools.jackson.databind.filter.dto.LiteralDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "default",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {ExpressionFactory.class, FilterFactory.class, GeoToolsValueMappers.class})
@AnnotateWith(value = Generated.class)
public abstract class ExpressionMapper {

    private final org.geotools.api.filter.FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    private final ExpressionVisitor visitor = new ExpressionVisitor() {

        @Override
        public SubtractDto visit(org.geotools.api.filter.expression.Subtract expression, Object extraData) {
            return map(expression);
        }

        @Override
        public PropertyNameDto visit(org.geotools.api.filter.expression.PropertyName expression, Object extraData) {
            return map(expression);
        }

        @Override
        public MultiplyDto visit(org.geotools.api.filter.expression.Multiply expression, Object extraData) {
            return map(expression);
        }

        @Override
        public LiteralDto visit(org.geotools.api.filter.expression.Literal expression, Object extraData) {
            return map(expression);
        }

        @Override
        public FunctionDto visit(org.geotools.api.filter.expression.Function expression, Object extraData) {
            return map(expression);
        }

        @Override
        public DivideDto visit(org.geotools.api.filter.expression.Divide expression, Object extraData) {
            return map(expression);
        }

        @Override
        public AddDto visit(org.geotools.api.filter.expression.Add expression, Object extraData) {
            return map(expression);
        }

        @Override
        public ExpressionDto visit(NilExpression expression, Object extraData) {
            return map(expression);
        }
    };

    public ExpressionDto map(org.geotools.api.filter.expression.Expression source) {
        return (ExpressionDto) source.accept(visitor, null);
    }

    public org.geotools.api.filter.expression.Expression map(ExpressionDto source) {
        if (source == null) {
            return null;
        }
        if (source instanceof LiteralDto literal) {
            return map(literal);
        } else if (source instanceof PropertyNameDto prop) {
            return map(prop);
        } else if (source instanceof AddDto add) {
            return map(add);
        } else if (source instanceof SubtractDto subtract) {
            return map(subtract);
        } else if (source instanceof MultiplyDto multiply) {
            return map(multiply);
        } else if (source instanceof DivideDto divide) {
            return map(divide);
        } else if (source instanceof FunctionDto function) {
            return map(function);
        }
        throw new IllegalArgumentException("Unrecognized expression type %s: %s"
                .formatted(source.getClass().getName(), source));
    }

    public @ObjectFactory org.geotools.api.filter.capability.FunctionName functionName(
            ExpressionDto.FunctionNameDto dto) {
        FunctionFinder finder = new FunctionFinder(null);
        String functionName = dto.getName();
        org.geotools.api.filter.capability.FunctionName name = finder.findFunctionDescription(functionName);
        if (name == null) {
            int argumentCount = dto.getArgumentCount();
            List<String> argumentNames = dto.getArgumentNames();
            if (argumentNames != null) {
                name = new FunctionNameImpl(functionName, argumentCount, argumentNames);
            } else {
                name = new FunctionNameImpl(functionName, argumentCount);
            }
        }
        return name;
    }

    public org.geotools.api.filter.capability.FunctionName map(ExpressionDto.FunctionNameDto dto) {
        return functionName(dto);
    }

    public ExpressionDto.FunctionNameDto map(org.geotools.api.filter.capability.FunctionName value) {
        ExpressionDto.FunctionNameDto dto = new ExpressionDto.FunctionNameDto();
        dto.setName(value.getName())
                .setArgumentCount(value.getArgumentCount())
                .setArgumentNames(value.getArgumentNames());
        return dto;
    }

    public abstract PropertyNameDto map(org.geotools.api.filter.expression.PropertyName expression);

    public abstract org.geotools.api.filter.expression.PropertyName map(PropertyNameDto dto);

    public abstract org.geotools.api.filter.expression.Literal map(LiteralDto dto);

    public abstract LiteralDto map(org.geotools.api.filter.expression.Literal expression);

    protected org.geotools.api.filter.expression.Function map(FunctionDto dto) {
        if (dto == null) {
            return null;
        }
        org.geotools.api.filter.expression.Expression[] parameters;
        parameters = dtoListToExpressionList(dto.getParameters());
        if (parameters == null) {
            parameters = new org.geotools.api.filter.expression.Expression[0];
        }
        return ff.function(dto.getName(), parameters);
    }

    protected abstract org.geotools.api.filter.expression.Expression[] dtoListToExpressionList(
            List<ExpressionDto> list);

    protected abstract ExpressionDto.FunctionDto map(org.geotools.api.filter.expression.Function expression);

    protected abstract AddDto map(org.geotools.api.filter.expression.Add expression);

    @Mapping(target = "expr1", source = "expression1")
    @Mapping(target = "expr2", source = "expression2")
    @Mapping(target = "expression1", ignore = true)
    @Mapping(target = "expression2", ignore = true)
    protected abstract org.geotools.filter.expression.AddImpl map(AddDto dto);

    protected abstract SubtractDto map(org.geotools.api.filter.expression.Subtract expression);

    @Mapping(target = "expr1", source = "expression1")
    @Mapping(target = "expr2", source = "expression2")
    @Mapping(target = "expression1", ignore = true)
    @Mapping(target = "expression2", ignore = true)
    protected abstract org.geotools.filter.expression.SubtractImpl map(SubtractDto dto);

    protected abstract DivideDto map(org.geotools.api.filter.expression.Divide expression);

    @Mapping(target = "expr1", source = "expression1")
    @Mapping(target = "expr2", source = "expression2")
    @Mapping(target = "expression1", ignore = true)
    @Mapping(target = "expression2", ignore = true)
    protected abstract org.geotools.filter.expression.DivideImpl map(DivideDto dto);

    protected abstract MultiplyDto map(org.geotools.api.filter.expression.Multiply expression);

    @Mapping(target = "expr1", source = "expression1")
    @Mapping(target = "expr2", source = "expression2")
    @Mapping(target = "expression1", ignore = true)
    @Mapping(target = "expression2", ignore = true)
    protected abstract org.geotools.filter.expression.MultiplyImpl map(MultiplyDto dto);
}
