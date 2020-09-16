package org.geotools.jackson.databind.filter.mapper;

import org.geotools.jackson.databind.filter.dto.Filter;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryComparisonOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryLogicOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinarySpatialOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryTemporalOperator;
import org.mapstruct.Mapper;
import org.opengis.filter.And;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.NativeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.ResourceId;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;

@Mapper(config = FilterMapperConfig.class)
interface FilterToDtoMapper {

    default Filter map(org.opengis.filter.Filter filter) {
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

    default Filter.Id.FeatureId map(org.opengis.filter.identity.Identifier id) {
        if (id == null) return null;
        Filter.Id.FeatureId fid;
        if (id instanceof ResourceId) {
            ResourceId rid = (ResourceId) id;
            Filter.Id.ResourceId resourceId = new Filter.Id.ResourceId();
            fid = resourceId;
            resourceId.setStartTime(rid.getStartTime()).setEndTime(rid.getEndTime());
            //                    .setVersion(rid.getVersion() == null ? null :
            // rid.getVersion().toString());
        } else if (id instanceof FeatureId) {
            fid = new Filter.Id.FeatureId();
        } else {
            throw new IllegalArgumentException(
                    "Identifier type not supported: " + id.getClass().getCanonicalName());
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
