/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import lombok.NonNull;
import org.geotools.api.filter.And;
import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.FilterVisitor;
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

class MappingFilterVisitor implements FilterVisitor {

    private @NonNull FilterToDtoMapper mapper;

    MappingFilterVisitor(@NonNull FilterToDtoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object visitNullFilter(Object extraData) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Object visit(NativeFilter filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(TOverlaps filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(TEquals filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(TContains filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(OverlappedBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(MetBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Meets filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Ends filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(EndedBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(During filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(BegunBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Begins filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Before filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(AnyInteracts filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(After filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Within filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Touches filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Overlaps filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Intersects filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Equals filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(DWithin filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Disjoint filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Crosses filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Contains filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Beyond filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsNull filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Or filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Not filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(And filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        return mapper.toDto(filter);
    }

    @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        return mapper.toDto(filter);
    }
}
