/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/** Abstract DTO for {@link org.geotools.api.filter.Filter} */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FilterDto.IdDto.class),
    @JsonSubTypes.Type(value = FilterDto.IncludeFilterDto.class),
    @JsonSubTypes.Type(value = FilterDto.ExcludeFilterDto.class),
    @JsonSubTypes.Type(value = FilterDto.NativeFilterDto.class),
    @JsonSubTypes.Type(value = FilterDto.NotDto.class),
    @JsonSubTypes.Type(value = FilterDto.PropertyIsNilDto.class),
    @JsonSubTypes.Type(value = FilterDto.PropertyIsNullDto.class),
    @JsonSubTypes.Type(value = FilterDto.MultiValuedFilterDto.class),
    @JsonSubTypes.Type(value = FilterDto.BinaryLogicOperatorDto.class)
})
@Data
@Accessors(chain = true)
public abstract class FilterDto {
    public static final IncludeFilterDto INCLUDE = new IncludeFilterDto();
    public static final ExcludeFilterDto EXCLUDE = new ExcludeFilterDto();

    protected FilterDto() {
        // default constructor
    }

    /** DTO for {@link org.geotools.api.filter.IncludeFilter} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("Include")
    public static class IncludeFilterDto extends FilterDto {}

    /** DTO for {@link org.geotools.api.filter.ExcludeFilter} */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("Exclude")
    public static class ExcludeFilterDto extends FilterDto {}

    /** DTO for {@link org.geotools.api.filter.Id} */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("Id")
    public static class IdDto extends FilterDto {
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

    /** DTO for {@link org.geotools.api.filter.NativeFilter} */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("NativeFilter")
    public static class NativeFilterDto extends FilterDto {
        @SuppressWarnings("java:S116")
        @JsonProperty("native")
        private String Native;
    }

    /** DTO for {@link org.geotools.api.filter.Not} */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("Not")
    public static class NotDto extends FilterDto {
        private FilterDto filter;
    }

    /** Base DTO for {@link org.geotools.api.filter.BinaryLogicOperator} */
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryLogicOperatorDto.And.class),
        @JsonSubTypes.Type(value = BinaryLogicOperatorDto.Or.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryLogicOperatorDto extends FilterDto {
        private List<FilterDto> children;

        /** DTO for {@link org.geotools.api.filter.And} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("And")
        public static class And extends BinaryLogicOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.Or} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Or")
        public static class Or extends BinaryLogicOperatorDto {}
    }

    /** DTO for {@link org.geotools.api.filter.PropertyIsNil} */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("PropertyIsNil")
    public static class PropertyIsNilDto extends FilterDto {
        private ExpressionDto expression;
        private Object nilReason;
    }

    /** DTO for {@link org.geotools.api.filter.PropertyIsNull} */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("PropertyIsNull")
    public static class PropertyIsNullDto extends FilterDto {
        private ExpressionDto expression;
    }

    /** Base DTO for {@link org.geotools.api.filter.MultiValuedFilter} */
    @JsonSubTypes({
        @JsonSubTypes.Type(value = PropertyIsBetweenDto.class),
        @JsonSubTypes.Type(value = PropertyIsLikeDto.class),
        @JsonSubTypes.Type(value = BinaryOperatorDto.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class MultiValuedFilterDto extends FilterDto {
        @JsonTypeName("MatchAction")
        public enum MatchActionDto {
            ANY,
            ALL,
            ONE
        }

        private MatchActionDto matchAction;
    }

    /**
     * Base DTO for BinaryOperator {@link org.geotools.api.filter.BinaryComparisonOperator},
     * {@link org.geotools.api.filter.spatial.BinarySpatialOperator}, and
     * {@link org.geotools.api.filter.temporal.BinaryTemporalOperator}
     */
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryComparisonOperatorDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryOperatorDto extends MultiValuedFilterDto {
        private ExpressionDto expression1;
        private ExpressionDto expression2;
    }

    /** DTO for {@link org.geotools.api.filter.PropertyIsBetween} */
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @Data
    @JsonTypeName("PropertyIsBetween")
    public static class PropertyIsBetweenDto extends MultiValuedFilterDto {
        private ExpressionDto expression;
        private ExpressionDto lowerBoundary;
        private ExpressionDto upperBoundary;
    }

    /** DTO for {@link org.geotools.api.filter.PropertyIsLike} */
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @Data
    @JsonTypeName("PropertyIsLike")
    public static class PropertyIsLikeDto extends MultiValuedFilterDto {
        private ExpressionDto expression;
        private String literal;
        private String wildCard;
        private String singleChar;
        private String escape;
        private boolean matchingCase;
    }

    /** Base DTO for {@link org.geotools.api.filter.BinaryComparisonOperator} */
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryComparisonOperatorDto.PropertyIsEqualToDto.class),
        @JsonSubTypes.Type(value = BinaryComparisonOperatorDto.PropertyIsNotEqualToDto.class),
        @JsonSubTypes.Type(value = BinaryComparisonOperatorDto.PropertyIsGreaterThanDto.class),
        @JsonSubTypes.Type(value = BinaryComparisonOperatorDto.PropertyIsGreaterThanOrEqualToDto.class),
        @JsonSubTypes.Type(value = BinaryComparisonOperatorDto.PropertyIsLessThanDto.class),
        @JsonSubTypes.Type(value = BinaryComparisonOperatorDto.PropertyIsLessThanOrEqualToDto.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryComparisonOperatorDto extends BinaryOperatorDto {
        private boolean matchingCase;

        /** DTO for {@link org.geotools.api.filter.PropertyIsEqualTo} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("PropertyIsEqualTo")
        public static class PropertyIsEqualToDto extends BinaryComparisonOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.PropertyIsGreaterThan} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("PropertyIsGreaterThan")
        public static class PropertyIsGreaterThanDto extends BinaryComparisonOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("PropertyIsGreaterThanOrEqualTo")
        public static class PropertyIsGreaterThanOrEqualToDto extends BinaryComparisonOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.PropertyIsLessThan} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("PropertyIsLessThan")
        public static class PropertyIsLessThanDto extends BinaryComparisonOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.PropertyIsLessThanOrEqualTo} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("PropertyIsLessThanOrEqualTo")
        public static class PropertyIsLessThanOrEqualToDto extends BinaryComparisonOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.PropertyIsNotEqualTo} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("PropertyIsNotEqualTo")
        public static class PropertyIsNotEqualToDto extends BinaryComparisonOperatorDto {}
    }

    /** Base DTO for {@link org.geotools.api.filter.spatial.BinarySpatialOperator} */
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.BBOXDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.ContainsDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.CrossesDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.DisjointDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.EqualsDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.IntersectsDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.OverlapsDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.TouchesDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.WithinDto.class),
        @JsonSubTypes.Type(value = BinarySpatialOperatorDto.DistanceBufferOperatorDto.class)
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinarySpatialOperatorDto extends BinaryOperatorDto {

        /** DTO for {@link org.geotools.api.filter.spatial.BBOX} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("BBOX")
        public static class BBOXDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Contains} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Contains")
        public static class ContainsDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Crosses} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Crosses")
        public static class CrossesDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Disjoint} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Disjoint")
        public static class DisjointDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Equals} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Equals")
        public static class EqualsDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Intersects} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Intersects")
        public static class IntersectsDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Overlaps} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Overlaps")
        public static class OverlapsDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Touches} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Touches")
        public static class TouchesDto extends BinarySpatialOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.Within} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Within")
        public static class WithinDto extends BinarySpatialOperatorDto {}

        /** Base DTO for {@link org.geotools.api.filter.spatial.DistanceBufferOperator} */
        @JsonSubTypes({
            @JsonSubTypes.Type(value = FilterDto.BinarySpatialOperatorDto.DWithinDto.class),
            @JsonSubTypes.Type(value = FilterDto.BinarySpatialOperatorDto.BeyondDto.class)
        })
        @Data
        @EqualsAndHashCode(callSuper = true)
        public abstract static class DistanceBufferOperatorDto extends BinarySpatialOperatorDto {
            private double distance;
            private String distanceUnits;
        }

        /** DTO for {@link org.geotools.api.filter.spatial.Beyond} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Beyond")
        @SuppressWarnings("java:S110")
        public static class BeyondDto extends DistanceBufferOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.spatial.DWithin} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("DWithin")
        @SuppressWarnings("java:S110")
        public static class DWithinDto extends DistanceBufferOperatorDto {}
    }

    /** DTO for */
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.AfterDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.AnyInteractsDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.BeforeDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.BeginsDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.BegunByDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.DuringDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.EndedByDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.EndsDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.MeetsDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.MetByDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.OverlappedByDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.TContainsDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.TOverlapsDto.class),
        @JsonSubTypes.Type(value = BinaryTemporalOperatorDto.TEqualsDto.class),
    })
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public abstract static class BinaryTemporalOperatorDto extends BinaryOperatorDto {

        /** DTO for {@link org.geotools.api.filter.temporal.After} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @ToString(callSuper = true)
        @JsonTypeName("After")
        public static class AfterDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.AnyInteracts} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("AnyInteracts")
        public static class AnyInteractsDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.Before} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Before")
        public static class BeforeDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.Begins} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Begins")
        public static class BeginsDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.BegunBy} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("BegunBy")
        public static class BegunByDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.During} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("During")
        public static class DuringDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.EndedBy} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("EndedBy")
        public static class EndedByDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.Ends} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Ends")
        public static class EndsDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.Meets} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("Meets")
        public static class MeetsDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.MetBy} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("MetBy")
        public static class MetByDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.OverlappedBy} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("OverlappedBy")
        public static class OverlappedByDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.TContains} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("TContains")
        public static class TContainsDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.TEquals} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("TEquals")
        public static class TEqualsDto extends BinaryTemporalOperatorDto {}

        /** DTO for {@link org.geotools.api.filter.temporal.TOverlaps} */
        @Data
        @EqualsAndHashCode(callSuper = true)
        @JsonTypeName("TOverlaps")
        public static class TOverlapsDto extends BinaryTemporalOperatorDto {}
    }
}
