/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

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
import org.geotools.jackson.databind.filter.dto.Filter;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryComparisonOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryLogicOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinarySpatialOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryTemporalOperator;
import org.mapstruct.Mapper;

@Mapper(config = FilterMapperConfig.class)
interface FilterToDtoMapper {

    default Filter map(org.geotools.api.filter.Filter filter) {
        if (filter == null) return null;
        return (Filter) filter.accept(new MappingFilterVisitor(this), null);
    }

    Filter.NativeFilter toDto(NativeFilter filter);

    Filter.IncludeFilter toDto(IncludeFilter filter);

    Filter.ExcludeFilter toDto(ExcludeFilter filter);

    BinarySpatialOperator.Within toDto(Within filter);

    BinarySpatialOperator.Touches toDto(Touches filter);

    BinarySpatialOperator.Overlaps toDto(Overlaps filter);

    BinarySpatialOperator.Intersects toDto(Intersects filter);

    BinarySpatialOperator.Equals toDto(Equals filter);

    BinarySpatialOperator.DWithin toDto(DWithin filter);

    BinarySpatialOperator.Disjoint toDto(Disjoint filter);

    BinarySpatialOperator.Crosses toDto(Crosses filter);

    BinarySpatialOperator.Contains toDto(Contains filter);

    BinarySpatialOperator.Beyond toDto(Beyond filter);

    BinarySpatialOperator.BBOX toDto(BBOX filter);

    Filter.PropertyIsNil toDto(PropertyIsNil filter);

    Filter.PropertyIsNull toDto(PropertyIsNull filter);

    Filter.PropertyIsLike toDto(PropertyIsLike filter);

    BinaryComparisonOperator.PropertyIsLessThanOrEqualTo toDto(PropertyIsLessThanOrEqualTo filter);

    BinaryComparisonOperator.PropertyIsLessThan toDto(PropertyIsLessThan filter);

    BinaryComparisonOperator.PropertyIsGreaterThanOrEqualTo toDto(
            PropertyIsGreaterThanOrEqualTo filter);

    BinaryComparisonOperator.PropertyIsGreaterThan toDto(PropertyIsGreaterThan filter);

    BinaryComparisonOperator.PropertyIsNotEqualTo toDto(PropertyIsNotEqualTo filter);

    BinaryComparisonOperator.PropertyIsEqualTo toDto(PropertyIsEqualTo filter);

    Filter.PropertyIsBetween toDto(PropertyIsBetween filter);

    BinaryLogicOperator.And toDto(And filter);

    BinaryLogicOperator.Or toDto(Or filter);

    Filter.Not toDto(Not filter);

    Filter.Id toDto(Id filter);

    default Filter.Id.FeatureId map(org.geotools.api.filter.identity.Identifier id) {
        if (id == null) return null;
        Filter.Id.FeatureId fid;
        if (id instanceof ResourceId rid) {
            Filter.Id.ResourceId resourceId = new Filter.Id.ResourceId();
            fid = resourceId;
            resourceId.setStartTime(rid.getStartTime()).setEndTime(rid.getEndTime());
        } else if (id instanceof FeatureId) {
            fid = new Filter.Id.FeatureId();
        } else {
            throw new IllegalArgumentException(
                    "Identifier type not supported: %s"
                            .formatted(id.getClass().getCanonicalName()));
        }
        fid.setId(((FeatureId) id).getID());
        fid.setPreviousRid(((FeatureId) id).getPreviousRid());
        fid.setFeatureVersion(((FeatureId) id).getFeatureVersion());
        return fid;
    }

    BinaryTemporalOperator.TEquals toDto(TEquals filter);

    BinaryTemporalOperator.TContains toDto(TContains filter);

    BinaryTemporalOperator.OverlappedBy toDto(OverlappedBy filter);

    BinaryTemporalOperator.MetBy toDto(MetBy filter);

    BinaryTemporalOperator.Meets toDto(Meets filter);

    BinaryTemporalOperator.Ends toDto(Ends filter);

    BinaryTemporalOperator.EndedBy toDto(EndedBy filter);

    BinaryTemporalOperator.During toDto(During filter);

    BinaryTemporalOperator.BegunBy toDto(BegunBy filter);

    BinaryTemporalOperator.Begins toDto(Begins filter);

    BinaryTemporalOperator.Before toDto(Before filter);

    BinaryTemporalOperator.AnyInteracts toDto(AnyInteracts filter);

    BinaryTemporalOperator.After toDto(After filter);

    BinaryTemporalOperator.TOverlaps toDto(TOverlaps overlaps);
}
