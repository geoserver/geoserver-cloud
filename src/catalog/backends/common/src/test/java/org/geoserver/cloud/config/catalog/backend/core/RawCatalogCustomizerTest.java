/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class RawCatalogCustomizerTest {

    @Test
    void rawCatalogAppliesCustomizers() {
        CoreBackendConfiguration config = new CoreBackendConfiguration();
        ResourcePool replacement = mock(ResourcePool.class);
        RawCatalogCustomizer customizer = catalog -> catalog.setResourcePool(replacement);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("customizer", customizer);
        ObjectProvider<RawCatalogCustomizer> customizers = beanFactory.getBeanProvider(RawCatalogCustomizer.class);

        CatalogPlugin catalog = config.rawCatalog(
                mock(GeoServerResourceLoader.class),
                mock(ExtendedCatalogFacade.class),
                new CatalogProperties(),
                customizers);

        assertThat(catalog.getResourcePool()).isSameAs(replacement);
    }
}
