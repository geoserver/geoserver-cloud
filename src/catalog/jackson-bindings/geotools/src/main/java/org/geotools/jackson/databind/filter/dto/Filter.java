/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Filter.Id.class, name = "Id"),
    @JsonSubTypes.Type(value = Filter.IncludeFilter.class, name = "Include"),
    @JsonSubTypes.Type(value = Filter.ExcludeFilter.class, name = "Exclude"),
    @JsonSubTypes.Type(value = Filter.NativeFilter.class, name = "NativeFilter"),
    @JsonSubTypes.Type(value = Filter.Not.class, name = "Not"),
    @JsonSubTypes.Type(value = Filter.PropertyIsNil.class, name = "PropertyIsNil"),
    @JsonSubTypes.Type(value = Filter.PropertyIsNull.class, name = "PropertyIsNul"),
    @JsonSubTypes.Type(value = Filter.MultiValuedFilter.class),
    @JsonSubTypes.Type(value = Filter.BinaryLogicOperator.class)
})
@Data
@Accessors(chain = true)
public abstract class Filter {
    public static final IncludeFilter INCLUDE = new IncludeFilter();
    public static final ExcludeFilter EXCLUDE = new ExcludeFilter();

    protected Filter() {
        // default constructor
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class IncludeFilter extends Filter {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ExcludeFilter extends Filter {}

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Id extends Filter {
        private Set<FeatureId> identifiers;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = FeatureId.class, name = "FeatureId"),
            @JsonSubTypes.Type(value = ResourceId.class, name = "ResourceId")
        })
        @Data
        @Accessors(chain = true)
        public static class FeatureId {
            private String id;
            private String previousRid;
            private String featureVersion;
        }

        @Data
        @Accessors(chain = true)
        @EqualsAndHashCode(callSuper = true)
        public static class ResourceId extends FeatureId {
            String version;
            Date startTime;
            Date endTime;
        }
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class NativeFilter extends Filter {
        private String Native;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Not extends Filter {
        private Filter filter;
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryLogicOperator.And.class, name = "And"),
        @JsonSubTypes.Type(value = BinaryLogicOperator.Or.class, name = "Or")
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryLogicOperator extends Filter {
        private List<Filter> children;

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class And extends BinaryLogicOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Or extends BinaryLogicOperator {}
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class PropertyIsNil extends Filter {
        private Expression expression;
        private Object nilReason;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class PropertyIsNull extends Filter {
        private Expression expression;
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = PropertyIsBetween.class, name = "PropertyIsBetween"),
        @JsonSubTypes.Type(value = PropertyIsLike.class, name = "PropertyIsLike"),
        @JsonSubTypes.Type(value = BinaryOperator.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class MultiValuedFilter extends Filter {
        public enum MatchAction {
            ANY,
            ALL,
            ONE
        }

        private MatchAction matchAction;
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryComparisonOperator.class),
        @JsonSubTypes.Type(value = BinarySpatialOperator.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryOperator extends MultiValuedFilter {
        private Expression expression1;
        private Expression expression2;
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class PropertyIsBetween extends MultiValuedFilter {
        private Expression expression;
        private Expression lowerBoundary;
        private Expression upperBoundary;
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class PropertyIsLike extends MultiValuedFilter {
        private Expression expression;
        private String literal;
        private String wildCard;
        private String singleChar;
        private String escape;
        private boolean matchingCase;
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(
                value = BinaryComparisonOperator.PropertyIsEqualTo.class,
                name = "PropertyIsEqualTo"),
        @JsonSubTypes.Type(
                value = BinaryComparisonOperator.PropertyIsNotEqualTo.class,
                name = "PropertyIsNotEqualTo"),
        @JsonSubTypes.Type(
                value = BinaryComparisonOperator.PropertyIsGreaterThan.class,
                name = "PropertyIsGreaterThan"),
        @JsonSubTypes.Type(
                value = BinaryComparisonOperator.PropertyIsGreaterThanOrEqualTo.class,
                name = "PropertyIsGreaterThanOrEqualTo"),
        @JsonSubTypes.Type(
                value = BinaryComparisonOperator.PropertyIsLessThan.class,
                name = "PropertyIsLessThan"),
        @JsonSubTypes.Type(
                value = BinaryComparisonOperator.PropertyIsLessThanOrEqualTo.class,
                name = "PropertyIsLessThanOrEqualTo")
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryComparisonOperator extends BinaryOperator {
        private boolean matchingCase;

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class PropertyIsEqualTo extends BinaryComparisonOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class PropertyIsGreaterThan extends BinaryComparisonOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class PropertyIsGreaterThanOrEqualTo extends BinaryComparisonOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class PropertyIsLessThan extends BinaryComparisonOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class PropertyIsLessThanOrEqualTo extends BinaryComparisonOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class PropertyIsNotEqualTo extends BinaryComparisonOperator {}
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.BBOX.class, name = "BBOX"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Contains.class, name = "Contains"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Crosses.class, name = "Crosses"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Disjoint.class, name = "Disjoint"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Equals.class, name = "Equals"),
        @JsonSubTypes.Type(
                value = Filter.BinarySpatialOperator.Intersects.class,
                name = "Intersects"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Overlaps.class, name = "Overlaps"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Touches.class, name = "Touches"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Within.class, name = "Within"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.DistanceBufferOperator.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinarySpatialOperator extends BinaryOperator {

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class BBOX extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Contains extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Crosses extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Disjoint extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Equals extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Intersects extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Overlaps extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Touches extends BinarySpatialOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Within extends BinarySpatialOperator {}

        @JsonSubTypes({
            @JsonSubTypes.Type(
                    value = Filter.BinarySpatialOperator.DWithin.class,
                    name = "DWithin"),
            @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Beyond.class, name = "Beyond")
        })
        @Data
        @EqualsAndHashCode(callSuper = true)
        public abstract static class DistanceBufferOperator extends BinarySpatialOperator {
            private double distance;
            private String distanceUnits;
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Beyond extends DistanceBufferOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class DWithin extends DistanceBufferOperator {}
    }

    @JsonSubTypes({
        @JsonSubTypes.Type(value = Filter.BinaryTemporalOperator.After.class, name = "After"),
        @JsonSubTypes.Type(
                value = Filter.BinaryTemporalOperator.AnyInteracts.class,
                name = "AnyInteracts"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.Before.class, name = "Before"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.Begins.class, name = "Begins"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.BegunBy.class, name = "BegunBy"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.During.class, name = "During"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.EndedBy.class, name = "EndedBy"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.Ends.class, name = "Ends"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.Meets.class, name = "Meets"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.MetBy.class, name = "MetBy"),
        @JsonSubTypes.Type(
                value = BinaryTemporalOperator.OverlappedBy.class,
                name = "OverlappedBy"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.TContains.class, name = "TContains"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.TOverlaps.class, name = "TOverlaps"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.TEquals.class, name = "TEquals")
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryTemporalOperator extends BinaryOperator {

        @Data
        @EqualsAndHashCode(callSuper = true)
        @ToString(doNotUseGetters = true, callSuper = true)
        public static class After extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class AnyInteracts extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Before extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Begins extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class BegunBy extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class During extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class EndedBy extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Ends extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Meets extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class MetBy extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class OverlappedBy extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TContains extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TEquals extends BinaryTemporalOperator {}

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class TOverlaps extends BinaryTemporalOperator {}
    }
}
