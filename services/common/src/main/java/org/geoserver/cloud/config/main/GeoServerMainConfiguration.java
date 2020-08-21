/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.main;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.core.FilteringXmlBeanDefinitionReader;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@Configuration
@Import(UrlProxifyingConfiguration.class)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = "jar:gs-main-.*!/applicationContext.xml" //
)
public class GeoServerMainConfiguration {
    /** Required when {@link GeoServerSecurityConfiguration} is not included */
    @Bean(name = "secureCatalog")
    @ConditionalOnMissingBean(org.geoserver.security.GeoServerSecurityManager.class)
    @DependsOn({"extensions"})
    public SecureCatalogImpl secureCatalog(@Qualifier("rawCatalog") Catalog rawCatalog)
            throws Exception {
        return new SecureCatalogImpl(rawCatalog);
    }

    /** Required when {@link GeoServerSecurityConfiguration} is not included */
    @ConditionalOnMissingBean(org.geoserver.security.GeoServerSecurityManager.class)
    @DependsOn({"extensions"})
    public DataAccessRuleDAO accessRulesDao(
            GeoServerDataDirectory dd, @Qualifier("rawCatalog") Catalog rawCatalog)
            throws Exception {
        return new DataAccessRuleDAO(dd, rawCatalog);
    }
}
