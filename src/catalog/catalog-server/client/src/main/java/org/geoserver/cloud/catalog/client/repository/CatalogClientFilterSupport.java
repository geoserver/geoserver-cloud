/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNil;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.capability.FilterCapabilities;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Add;
import org.geotools.api.filter.expression.Divide;
import org.geotools.api.filter.expression.Multiply;
import org.geotools.api.filter.expression.Subtract;
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
import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;

import java.util.List;

/** */
@RequiredArgsConstructor
@Accessors(fluent = true, chain = true)
public class CatalogClientFilterSupport {

    public static final FilterCapabilities CAPABILITIES;

    static {
        Capabilities builder = new Capabilities();
        builder.addAll(Capabilities.LOGICAL);
        builder.addAll(Capabilities.SIMPLE_COMPARISONS);
        builder.addType(Id.class);
        builder.addType(PropertyIsNil.class);
        builder.addType(PropertyIsNull.class);
        builder.addType(PropertyIsLike.class);
        builder.addType(PropertyIsBetween.class);

        builder.addName(Add.NAME);
        builder.addName(Subtract.NAME);
        builder.addName(Multiply.NAME);
        builder.addName(Divide.NAME);

        builder.addType(BBOX.class);
        builder.addType(Contains.class);
        builder.addType(Crosses.class);
        builder.addType(Disjoint.class);
        builder.addType(Beyond.class);
        builder.addType(DWithin.class);
        builder.addType(Equals.class);
        builder.addType(Intersects.class);
        builder.addType(Overlaps.class);
        builder.addType(Touches.class);
        builder.addType(Within.class);

        builder.addType(After.class);
        builder.addType(AnyInteracts.class);
        builder.addType(Before.class);
        builder.addType(Begins.class);
        builder.addType(BegunBy.class);
        builder.addType(During.class);
        builder.addType(EndedBy.class);
        builder.addType(Ends.class);
        builder.addType(Meets.class);
        builder.addType(MetBy.class);
        builder.addType(OverlappedBy.class);
        builder.addType(TContains.class);
        builder.addType(TEquals.class);
        builder.addType(TOverlaps.class);

        CAPABILITIES = builder.getContents();
    }

    private Capabilities capabilities;

    public CatalogClientFilterSupport(@NonNull List<FunctionName> supportedServerFunctions) {
        capabilities = createCapabilities(supportedServerFunctions);
    }

    public static @Value class PrePostFilterTuple {
        private final @NonNull Filter pre;
        private final @NonNull Filter post;
    }

    public PrePostFilterTuple split(@NonNull Filter filter) {
        CapabilitiesFilterSplitter splitter =
                new CapabilitiesFilterSplitter(capabilities, null, null);
        filter.accept(splitter, null);
        return new PrePostFilterTuple(splitter.getFilterPre(), splitter.getFilterPost());
    }

    private static Capabilities createCapabilities(List<FunctionName> supportedFunctionNames) {
        Capabilities builder = new Capabilities(CAPABILITIES);
        for (FunctionName fn : supportedFunctionNames) {
            String name = fn.getName();
            int argumentCount = fn.getArgumentCount();
            builder.addName(name, argumentCount);
        }
        return builder;
    }
}
