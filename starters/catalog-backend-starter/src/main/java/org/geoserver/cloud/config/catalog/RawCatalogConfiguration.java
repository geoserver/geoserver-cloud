package org.geoserver.cloud.config.catalog;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.DefaultCatalogFacade;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Overrides the "rawCatalog" initialization to make sure its default catalog facade is a pluggable
 * {@link DefaultCatalogFacade}
 */
@Configuration
// @ImportResource( //
//        reader = FilteringXmlBeanDefinitionReader.class, //
//        locations = "classpath:pluggable-catalog.xml" //
// )
public class RawCatalogConfiguration {

    public @Bean CatalogFacade catalogFacade() {
        return new org.geoserver.catalog.plugin.DefaultCatalogFacade();
    }

    public @Bean CatalogImpl rawCatalog(GeoServerResourceLoader resourceLoader) {
        CatalogImpl catalog = new CatalogImpl();
        CatalogFacade facade = catalogFacade();
        catalog.setFacade(facade);
        catalog.setResourceLoader(resourceLoader);
        return catalog;
    }
}
