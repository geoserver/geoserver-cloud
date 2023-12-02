/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter.SilentCatalog;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.junit.jupiter.api.Test;

/**
 * Asserts that a {@link CatalogFacadeExtensionAdapter} does not result in double publishing of
 * catalog events, and that {@link CatalogFacadeExtensionAdapter#update} correctly forwards to
 * legacy {@link CatalogFacade#save} methods
 */
public class CatalogFacadeExtensionAdapterTest extends CatalogConformanceTest {

    private CatalogPlugin catalog;

    private CatalogFacade legacyFacade;

    protected @Override CatalogPlugin createCatalog() {
        catalog = new CatalogPlugin();
        legacyFacade = new DefaultCatalogFacade(catalog);
        catalog.setFacade(new CatalogFacadeExtensionAdapter(legacyFacade));
        return catalog;
    }

    @Test
    void testCatalogDecoratesLegacyFacade() {
        catalog.setFacade(legacyFacade);
        assertSame(legacyFacade, catalog.getRawFacade());
        assertThat(catalog.getFacade(), instanceOf(ResolvingCatalogFacadeDecorator.class));
        ResolvingCatalogFacadeDecorator resolving =
                (ResolvingCatalogFacadeDecorator) catalog.getFacade();
        assertThat(resolving.getSubject(), instanceOf(IsolatedCatalogFacade.class));
        IsolatedCatalogFacade isolated = (IsolatedCatalogFacade) resolving.getSubject();
        assertThat(isolated.getSubject(), instanceOf(CatalogFacadeExtensionAdapter.class));
        CatalogFacadeExtensionAdapter adapter =
                (CatalogFacadeExtensionAdapter) isolated.getSubject();
        assertSame(legacyFacade, adapter.getSubject());
    }

    @Test
    void testProvidedDecorator() {
        CatalogFacadeExtensionAdapter adapter = new CatalogFacadeExtensionAdapter(legacyFacade);
        catalog.setFacade(adapter);
        assertSame(adapter, catalog.getRawFacade());
        assertThat(catalog.getFacade(), instanceOf(ResolvingCatalogFacadeDecorator.class));
        ResolvingCatalogFacadeDecorator resolving =
                (ResolvingCatalogFacadeDecorator) catalog.getFacade();
        IsolatedCatalogFacade isolated = (IsolatedCatalogFacade) resolving.getSubject();
        assertSame(adapter, isolated.getSubject());
    }

    @Test
    void testAdapterReplacesLegacyCatalogFacadeCatalog() {
        CatalogFacadeExtensionAdapter adapter = new CatalogFacadeExtensionAdapter(legacyFacade);

        assertSame(legacyFacade, adapter.getSubject());
        assertNotSame(catalog, legacyFacade.getCatalog());
        assertThat(
                legacyFacade.getCatalog(),
                instanceOf(CatalogFacadeExtensionAdapter.SilentCatalog.class));

        SilentCatalog decoratorAtFacadeConstructor = (SilentCatalog) legacyFacade.getCatalog();
        catalog.setFacade(adapter);
        assertSame(adapter, catalog.getRawFacade());
        assertThat(
                legacyFacade.getCatalog(),
                instanceOf(CatalogFacadeExtensionAdapter.SilentCatalog.class));
        assertNotSame(decoratorAtFacadeConstructor, legacyFacade.getCatalog());
    }

    // @Test void testQuery() {
    // catalog.list(of, filter);
    // }
}
