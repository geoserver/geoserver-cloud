/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.AbstractCatalogFacade;

/**
 * {@link CatalogFacade} that applies a possibly side-effect producing identity {@link Function} to
 * each {@link CatalogInfo} right before returning it.
 *
 * <p>One example use case is to resolve proxied inner references to other {@link CatalogInfo}
 * objects
 */
public abstract class ResolvingCatalogFacade extends AbstractCatalogFacade
        implements CatalogFacade {

    private Function<CatalogInfo, CatalogInfo> proxyRefsResolver = i -> i;

    public ResolvingCatalogFacade() {
        super();
    }

    public void setObjectResolver(Function<CatalogInfo, CatalogInfo> proxyRefsResolver) {
        Objects.requireNonNull(proxyRefsResolver);
        this.proxyRefsResolver = proxyRefsResolver;
    }

    @SuppressWarnings("unchecked")
    protected <I extends CatalogInfo> Function<I, I> proxyRefsResolver() {
        return (Function<I, I>) proxyRefsResolver;
    }

    protected <I extends CatalogInfo> I resolveProxyReferences(I info) {
        if (info == null) return null;
        Function<I, I> resolver = proxyRefsResolver();
        return resolver.apply(info);
    }

    @Override
    protected <T extends CatalogInfo> T verifyBeforeReturning(T ci, Class<T> clazz) {
        return ci == null ? null : super.verifyBeforeReturning(resolveProxyReferences(ci), clazz);
    }

    @Override
    protected <T extends CatalogInfo> List<T> verifyBeforeReturning(List<T> list, Class<T> clazz) {
        return super.verifyBeforeReturning(lazyResolveReferences(list), clazz);
    }

    protected <T extends CatalogInfo> List<T> lazyResolveReferences(List<T> list) {
        return Lists.transform(list, this::resolveProxyReferences);
    }
}
