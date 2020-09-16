package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

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
@Accessors(chain = true)
public @Data class Filter {
    public static final IncludeFilter INCLUDE = new IncludeFilter();
    public static final ExcludeFilter EXCLUDE = new ExcludeFilter();

    @EqualsAndHashCode(callSuper = true)
    public static @Data class IncludeFilter extends Filter {}

    @EqualsAndHashCode(callSuper = true)
    public static @Data class ExcludeFilter extends Filter {}

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static @Data class Id extends Filter {
        private Set<FeatureId> identifiers;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = FeatureId.class, name = "FeatureId"),
            @JsonSubTypes.Type(value = ResourceId.class, name = "ResourceId")
        })
        @Accessors(chain = true)
        public static @Data class FeatureId {
            private String id;
            private String previousRid;
            private String featureVersion;
        }

        @Accessors(chain = true)
        @EqualsAndHashCode(callSuper = true)
        public static @Data class ResourceId extends FeatureId {
            String version;
            Date startTime;
            Date endTime;
        }
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static @Data class NativeFilter extends Filter {
        private String Native;
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static @Data class Not extends Filter {
        private Filter filter;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryLogicOperator.And.class, name = "And"),
        @JsonSubTypes.Type(value = BinaryLogicOperator.Or.class, name = "Or")
    })
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static @Data class BinaryLogicOperator extends Filter {
        private List<Filter> children;

        @EqualsAndHashCode(callSuper = true)
        public static @Data class And extends BinaryLogicOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Or extends BinaryLogicOperator {}
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static @Data class PropertyIsNil extends Filter {
        private Expression expression;
        private Object nilReason;
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static @Data class PropertyIsNull extends Filter {
        private Expression expression;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = PropertyIsBetween.class, name = "PropertyIsBetween"),
        @JsonSubTypes.Type(value = PropertyIsLike.class, name = "PropertyIsLike"),
        @JsonSubTypes.Type(value = BinaryOperator.class)
    })
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static @Data class MultiValuedFilter extends Filter {
        public enum MatchAction {
            ANY,
            ALL,
            ONE
        };

        private MatchAction matchAction;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryComparisonOperator.class),
        @JsonSubTypes.Type(value = BinarySpatialOperator.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.class)
    })
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static @Data class BinaryOperator extends MultiValuedFilter {
        private Expression expression1;
        private Expression expression2;
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static @Data class PropertyIsBetween extends MultiValuedFilter {
        private Expression expression;
        private Expression lowerBoundary;
        private Expression upperBoundary;
    }

    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static @Data class PropertyIsLike extends MultiValuedFilter {
        private Expression expression;
        private String literal;
        private String wildCard;
        private String singleChar;
        private String escape;
        private boolean matchingCase;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(
            value = BinaryComparisonOperator.PropertyIsEqualTo.class,
            name = "PropertyIsEqualTo"
        ),
        @JsonSubTypes.Type(
            value = BinaryComparisonOperator.PropertyIsNotEqualTo.class,
            name = "PropertyIsNotEqualTo"
        ),
        @JsonSubTypes.Type(
            value = BinaryComparisonOperator.PropertyIsGreaterThan.class,
            name = "PropertyIsGreaterThan"
        ),
        @JsonSubTypes.Type(
            value = BinaryComparisonOperator.PropertyIsGreaterThanOrEqualTo.class,
            name = "PropertyIsGreaterThanOrEqualTo"
        ),
        @JsonSubTypes.Type(
            value = BinaryComparisonOperator.PropertyIsLessThan.class,
            name = "PropertyIsLessThan"
        ),
        @JsonSubTypes.Type(
            value = BinaryComparisonOperator.PropertyIsLessThanOrEqualTo.class,
            name = "PropertyIsLessThanOrEqualTo"
        )
    })
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static @Data class BinaryComparisonOperator extends BinaryOperator {
        private boolean matchingCase;

        @EqualsAndHashCode(callSuper = true)
        public static @Data class PropertyIsEqualTo extends BinaryComparisonOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class PropertyIsGreaterThan extends BinaryComparisonOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class PropertyIsGreaterThanOrEqualTo extends BinaryComparisonOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class PropertyIsLessThan extends BinaryComparisonOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class PropertyIsLessThanOrEqualTo extends BinaryComparisonOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class PropertyIsNotEqualTo extends BinaryComparisonOperator {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.BBOX.class, name = "BBOX"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Contains.class, name = "Contains"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Crosses.class, name = "Crosses"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Disjoint.class, name = "Disjoint"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Equals.class, name = "Equals"),
        @JsonSubTypes.Type(
            value = Filter.BinarySpatialOperator.Intersects.class,
            name = "Intersects"
        ),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Overlaps.class, name = "Overlaps"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Touches.class, name = "Touches"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Within.class, name = "Within"),
        @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.DistanceBufferOperator.class)
    })
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static @Data class BinarySpatialOperator extends BinaryOperator {

        @EqualsAndHashCode(callSuper = true)
        public static @Data class BBOX extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Contains extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Crosses extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Disjoint extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Equals extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Intersects extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Overlaps extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Touches extends BinarySpatialOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Within extends BinarySpatialOperator {}

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        @JsonSubTypes({
            @JsonSubTypes.Type(
                value = Filter.BinarySpatialOperator.DWithin.class,
                name = "DWithin"
            ),
            @JsonSubTypes.Type(value = Filter.BinarySpatialOperator.Beyond.class, name = "Beyond")
        })
        @EqualsAndHashCode(callSuper = true)
        public static @Data abstract class DistanceBufferOperator extends BinarySpatialOperator {
            private double distance;
            private String distanceUnits;
        }

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Beyond extends DistanceBufferOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class DWithin extends DistanceBufferOperator {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Filter.BinaryTemporalOperator.After.class, name = "After"),
        @JsonSubTypes.Type(
            value = Filter.BinaryTemporalOperator.AnyInteracts.class,
            name = "AnyInteracts"
        ),
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
            name = "OverlappedBy"
        ),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.TContains.class, name = "TContains"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.TOverlaps.class, name = "TOverlaps"),
        @JsonSubTypes.Type(value = BinaryTemporalOperator.TEquals.class, name = "TEquals")
    })
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static @Data class BinaryTemporalOperator extends BinaryOperator {

        @EqualsAndHashCode(callSuper = true)
        @ToString(doNotUseGetters = true, callSuper = true)
        public static @Data class After extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class AnyInteracts extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Before extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Begins extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class BegunBy extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class During extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class EndedBy extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Ends extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class Meets extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class MetBy extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class OverlappedBy extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class TContains extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class TEquals extends BinaryTemporalOperator {}

        @EqualsAndHashCode(callSuper = true)
        public static @Data class TOverlaps extends BinaryTemporalOperator {}
    }
}
