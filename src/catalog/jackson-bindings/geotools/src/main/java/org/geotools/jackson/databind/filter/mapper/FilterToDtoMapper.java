/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import lombok.Generated;
import org.geotools.api.filter.And;
import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.api.filter.NativeFilter;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNil;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.filter.identity.ResourceId;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.Beyond;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.Crosses;
import org.geotools.api.filter.spatial.DWithin;
import org.geotools.api.filter.spatial.Disjoint;
import org.geotools.api.filter.spatial.Equals;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Overlaps;
import org.geotools.api.filter.spatial.Touches;
import org.geotools.api.filter.spatial.Within;
import org.geotools.api.filter.temporal.After;
import org.geotools.api.filter.temporal.AnyInteracts;
import org.geotools.api.filter.temporal.Before;
import org.geotools.api.filter.temporal.Begins;
import org.geotools.api.filter.temporal.BegunBy;
import org.geotools.api.filter.temporal.During;
import org.geotools.api.filter.temporal.EndedBy;
import org.geotools.api.filter.temporal.Ends;
import org.geotools.api.filter.temporal.Meets;
import org.geotools.api.filter.temporal.MetBy;
import org.geotools.api.filter.temporal.OverlappedBy;
import org.geotools.api.filter.temporal.TContains;
import org.geotools.api.filter.temporal.TEquals;
import org.geotools.api.filter.temporal.TOverlaps;
import org.geotools.jackson.databind.filter.dto.FilterDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinaryComparisonOperatorDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinaryLogicOperatorDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinarySpatialOperatorDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinaryTemporalOperatorDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;

@Mapper(config = FilterMapperConfig.class)
@AnnotateWith(value = Generated.class)
interface FilterToDtoMapper {

    default FilterDto map(org.geotools.api.filter.Filter filter) {
        if (filter == null) {
            return null;
        }
        return (FilterDto) filter.accept(new MappingFilterVisitor(this), null);
    }

    FilterDto.NativeFilterDto toDto(NativeFilter filter);

    FilterDto.IncludeFilterDto toDto(IncludeFilter filter);

    FilterDto.ExcludeFilterDto toDto(ExcludeFilter filter);

    BinarySpatialOperatorDto.WithinDto toDto(Within filter);

    BinarySpatialOperatorDto.TouchesDto toDto(Touches filter);

    BinarySpatialOperatorDto.OverlapsDto toDto(Overlaps filter);

    BinarySpatialOperatorDto.IntersectsDto toDto(Intersects filter);

    BinarySpatialOperatorDto.EqualsDto toDto(Equals filter);

    BinarySpatialOperatorDto.DWithinDto toDto(DWithin filter);

    BinarySpatialOperatorDto.DisjointDto toDto(Disjoint filter);

    BinarySpatialOperatorDto.CrossesDto toDto(Crosses filter);

    BinarySpatialOperatorDto.ContainsDto toDto(Contains filter);

    BinarySpatialOperatorDto.BeyondDto toDto(Beyond filter);

    BinarySpatialOperatorDto.BBOXDto toDto(BBOX filter);

    FilterDto.PropertyIsNilDto toDto(PropertyIsNil filter);

    FilterDto.PropertyIsNullDto toDto(PropertyIsNull filter);

    FilterDto.PropertyIsLikeDto toDto(PropertyIsLike filter);

    BinaryComparisonOperatorDto.PropertyIsLessThanOrEqualToDto toDto(PropertyIsLessThanOrEqualTo filter);

    BinaryComparisonOperatorDto.PropertyIsLessThanDto toDto(PropertyIsLessThan filter);

    BinaryComparisonOperatorDto.PropertyIsGreaterThanOrEqualToDto toDto(PropertyIsGreaterThanOrEqualTo filter);

    BinaryComparisonOperatorDto.PropertyIsGreaterThanDto toDto(PropertyIsGreaterThan filter);

    BinaryComparisonOperatorDto.PropertyIsNotEqualToDto toDto(PropertyIsNotEqualTo filter);

    BinaryComparisonOperatorDto.PropertyIsEqualToDto toDto(PropertyIsEqualTo filter);

    FilterDto.PropertyIsBetweenDto toDto(PropertyIsBetween filter);

    BinaryLogicOperatorDto.And toDto(And filter);

    BinaryLogicOperatorDto.Or toDto(Or filter);

    FilterDto.NotDto toDto(Not filter);

    FilterDto.IdDto toDto(Id filter);

    default FilterDto.IdDto.FeatureId map(org.geotools.api.filter.identity.Identifier id) {
        if (id == null) {
            return null;
        }
        FilterDto.IdDto.FeatureId fid;
        if (id instanceof ResourceId rid) {
            FilterDto.IdDto.ResourceId resourceId = new FilterDto.IdDto.ResourceId();
            fid = resourceId;
            resourceId.setStartTime(rid.getStartTime()).setEndTime(rid.getEndTime());
        } else if (id instanceof FeatureId) {
            fid = new FilterDto.IdDto.FeatureId();
        } else {
            throw new IllegalArgumentException(
                    "Identifier type not supported: %s".formatted(id.getClass().getCanonicalName()));
        }
        fid.setId(((FeatureId) id).getID());
        fid.setPreviousRid(((FeatureId) id).getPreviousRid());
        fid.setFeatureVersion(((FeatureId) id).getFeatureVersion());
        return fid;
    }

    BinaryTemporalOperatorDto.TEqualsDto toDto(TEquals filter);

    BinaryTemporalOperatorDto.TContainsDto toDto(TContains filter);

    BinaryTemporalOperatorDto.OverlappedByDto toDto(OverlappedBy filter);

    BinaryTemporalOperatorDto.MetByDto toDto(MetBy filter);

    BinaryTemporalOperatorDto.MeetsDto toDto(Meets filter);

    BinaryTemporalOperatorDto.EndsDto toDto(Ends filter);

    BinaryTemporalOperatorDto.EndedByDto toDto(EndedBy filter);

    BinaryTemporalOperatorDto.DuringDto toDto(During filter);

    BinaryTemporalOperatorDto.BegunByDto toDto(BegunBy filter);

    BinaryTemporalOperatorDto.BeginsDto toDto(Begins filter);

    BinaryTemporalOperatorDto.BeforeDto toDto(Before filter);

    BinaryTemporalOperatorDto.AnyInteractsDto toDto(AnyInteracts filter);

    BinaryTemporalOperatorDto.AfterDto toDto(After filter);

    BinaryTemporalOperatorDto.TOverlapsDto toDto(TOverlaps overlaps);
}
