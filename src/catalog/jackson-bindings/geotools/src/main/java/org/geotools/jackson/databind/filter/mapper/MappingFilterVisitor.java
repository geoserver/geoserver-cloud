/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
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

    public @Override Object visitNullFilter(Object extraData) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override Object visit(NativeFilter filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(TOverlaps filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(TEquals filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(TContains filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(OverlappedBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(MetBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Meets filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Ends filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(EndedBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(During filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(BegunBy filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Begins filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Before filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(AnyInteracts filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(After filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Within filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Touches filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Overlaps filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Intersects filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Equals filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(DWithin filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Disjoint filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Crosses filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Contains filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Beyond filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(BBOX filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsNil filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsNull filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsLike filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsLessThan filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsGreaterThan filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsEqualTo filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(PropertyIsBetween filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Or filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Not filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(Id filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(And filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(IncludeFilter filter, Object extraData) {
        return mapper.toDto(filter);
    }

    public @Override Object visit(ExcludeFilter filter, Object extraData) {
        return mapper.toDto(filter);
    }
}
