/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wms;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Service;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.geoserver.wms.WMSServiceExceptionHandler;
import org.geoserver.wms.capabilities.GetCapabilitiesTransformer;
import org.geoserver.wms.capabilities.LegendSample;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;

/**
 * Transpiled XML configuration from {@literal jar:gs-wms-.*!/applicationContext.xml}
 */
@Configuration
@TranspileXmlConfig(
        locations = "jar:gs-wms-.*!/applicationContext.xml",
        excludes = {"legendSample", "wmsExceptionHandler"})
@Import(WmsConfiguration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class WmsConfiguration {

    /**
     * Required by {@link GetCapabilitiesTransformer}, excluded from gs-wms.jar
     *
     * @param catalog using {@code rawCatalog} instead of {@code catalog}, to avoid
     *                the local workspace and secured catalog decorators
     */
    @Bean
    LegendSample legendSample(@Qualifier("rawCatalog") Catalog catalog, GeoServerResourceLoader loader) {
        return new LegendSampleImpl(catalog, loader);
    }
    //
    //    @Bean
    //    WFSConfiguration wfsConfiguration(GeoServer geoServer) {
    //        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
    //        return new WFSConfiguration(geoServer, schemaBuilder, new WFS(schemaBuilder));
    //    }
    //
    /**
     * Overrides the {@code @TranspileXmlConfig} excluded {@code wmsExceptionHandler} bean
     * with a {@link StatusCodeWmsExceptionHandler} to support setting a non 200
     * status code on http responses.
     * <p>
     * The original bean definition is as follows, which this bean method respects:
     *
     * <pre>
     * <code>
     *  <!-- service exception handler -->
     *  <bean id="wmsExceptionHandler" class=
     * "org.geoserver.wms.WMSServiceExceptionHandler">
     *          <constructor-arg>
     *                  <list>
     *                          <ref bean="wms-1_1_1-ServiceDescriptor"/>
     *                          <ref bean="wms-1_3_0-ServiceDescriptor"/>
     *                  </list>
     *          </constructor-arg>
     *          <constructor-arg ref="geoServer"/>
     *  </bean>
     * </code>
     * </pre>
     * @param propertyResolver
     *
     * @return
     */
    @Bean
    WMSServiceExceptionHandler wmsExceptionHandler(
            @SuppressWarnings("java:S6830") @Qualifier("wms-1_1_1-ServiceDescriptor") Service wms11,
            @SuppressWarnings("java:S6830") @Qualifier("wms-1_3_0-ServiceDescriptor") Service wms13,
            GeoServer geoServer,
            PropertyResolver propertyResolver) {
        return new StatusCodeWmsExceptionHandler(List.of(wms11, wms13), geoServer, propertyResolver);
    }
}
