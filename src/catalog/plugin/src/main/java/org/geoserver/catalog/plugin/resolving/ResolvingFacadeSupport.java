/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** */
public class ResolvingFacadeSupport<T> implements ResolvingFacade<T> {

    private UnaryOperator<T> outboundResolver = UnaryOperator.identity();
    private UnaryOperator<T> inboundResolver = UnaryOperator.identity();

    @Override
    public void setOutboundResolver(UnaryOperator<T> resolvingFunction) {
        Objects.requireNonNull(resolvingFunction);
        this.outboundResolver = resolvingFunction;
    }

    @Override
    public UnaryOperator<T> getOutboundResolver() {
        return this.outboundResolver;
    }

    @Override
    public void setInboundResolver(UnaryOperator<T> resolvingFunction) {
        Objects.requireNonNull(resolvingFunction);
        this.inboundResolver = resolvingFunction;
    }

    @Override
    public UnaryOperator<T> getInboundResolver() {
        return this.inboundResolver;
    }

    @SuppressWarnings("unchecked")
    public <I extends T> UnaryOperator<I> outbound() {
        return (UnaryOperator<I>) outboundResolver;
    }

    @SuppressWarnings("unchecked")
    public <I extends T> UnaryOperator<I> inbound() {
        return (UnaryOperator<I>) inboundResolver;
    }

    @Override
    public <C extends T> C resolveOutbound(C info) {
        UnaryOperator<C> outboundResolve = outbound();
        return outboundResolve.apply(info);
    }

    @Override
    public <C extends T> C resolveInbound(C info) {
        UnaryOperator<C> inboundResolve = inbound();
        return inboundResolve.apply(info);
    }

    protected <C extends T> List<C> resolveOutbound(List<C> info) {
        return Lists.transform(info, this::resolveOutbound);
    }
}
