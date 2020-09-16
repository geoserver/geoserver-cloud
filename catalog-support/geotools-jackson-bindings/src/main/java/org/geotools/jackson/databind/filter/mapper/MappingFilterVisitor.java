/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import lombok.NonNull;
import org.opengis.filter.And;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.FilterVisitor;
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
