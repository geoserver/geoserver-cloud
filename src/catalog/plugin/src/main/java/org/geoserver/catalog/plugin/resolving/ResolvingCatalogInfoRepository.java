/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;

import java.util.function.UnaryOperator;

/**
 * @since 1.4
 */
public abstract class ResolvingCatalogInfoRepository<T extends CatalogInfo>
        implements ResolvingFacade<T>, CatalogInfoRepository<T> {

    private final ResolvingFacadeSupport<T> resolver;

    /**
     * @param template
     */
    protected ResolvingCatalogInfoRepository() {
        this.resolver = new ResolvingFacadeSupport<>();
    }

    @Override
    public void setOutboundResolver(UnaryOperator<T> resolvingFunction) {
        resolver.setOutboundResolver(resolvingFunction);
    }

    @Override
    public UnaryOperator<T> getOutboundResolver() {
        return resolver.getOutboundResolver();
    }

    @Override
    public void setInboundResolver(UnaryOperator<T> resolvingFunction) {
        resolver.setInboundResolver(resolvingFunction);
    }

    @Override
    public UnaryOperator<T> getInboundResolver() {
        return resolver.getInboundResolver();
    }

    @Override
    public <C extends T> C resolveOutbound(C info) {
        return resolver.resolveOutbound(info);
    }

    @Override
    public <C extends T> C resolveInbound(C info) {
        return resolver.resolveInbound(info);
    }
}
