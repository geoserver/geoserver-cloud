/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.geoserver.function.IsInstanceOf;
import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.Subtract;
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

        builder.addName(IsInstanceOf.NAME.getName());

        CAPABILITIES = builder.getContents();
    }

    private Capabilities capabilities;

    public CatalogClientFilterSupport(@NonNull List<FunctionName> supportedServerFunctions) {
        capabilities = createCapabilities(supportedServerFunctions);
    }

    public CapabilitiesFilterSplitter split(@NonNull Filter filter) {
        CapabilitiesFilterSplitter splitter =
                new CapabilitiesFilterSplitter(capabilities, null, null);
        filter.accept(splitter, null);
        return splitter;
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
