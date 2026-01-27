/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wms;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.gwc.integration.WMSIntegrationAutoConfiguration;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.cloud.wms.app.StatusCodeWmsExceptionHandler;
import org.geoserver.cloud.wms.controller.GetMapReflectorController;
import org.geoserver.cloud.wms.controller.WMSController;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Service;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geoserver.wms.WMSServiceExceptionHandler;
import org.geoserver.wms.capabilities.GetCapabilitiesTransformer;
import org.geoserver.wms.capabilities.LegendSample;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;

// auto-configure before GWC's wms-integration to avoid it precluding to load beans from
// jar:gs-wms-.*
@AutoConfiguration(before = WMSIntegrationAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ImportFilteredResource({
    "jar:gs-wms-.*!/applicationContext.xml#name=" + WmsApplicationAutoConfiguration.WMS_BEANS_BLACKLIST,
    "jar:gs-wms-gml-.*!/applicationContext.xml",
    "jar:gs-wfs-core-.*!/applicationContext.xml#name=" + WmsApplicationAutoConfiguration.WFS_BEANS_WHITELIST,
    "jar:gs-wfs1_x-.*!/applicationContext.xml#name=" + WmsApplicationAutoConfiguration.WFS_BEANS_WHITELIST,
    "jar:gs-wfs2_x-.*!/applicationContext.xml#name=" + WmsApplicationAutoConfiguration.WFS_BEANS_WHITELIST
})
public class WmsApplicationAutoConfiguration {

    static final String WFS_BEANS_WHITELIST =
            """
            ^(\
            gml.*OutputFormat\
            |bboxKvpParser\
            |featureIdKvpParser\
            |filter.*_KvpParser\
            |cqlKvpParser\
            |maxFeatureKvpParser\
            |sortByKvpParser\
            |xmlConfiguration.*\
            |gml[1-9]*SchemaBuilder\
            |wfsXsd.*\
            |wfsSqlViewKvpParser\
            ).*$\
            """;

    /** wms beans black-list */
    static final String WMS_BEANS_BLACKLIST =
            """
            ^(?!\
            legendSample\
            |wmsExceptionHandler\
            ).*$\
            """;

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

    @Bean
    WFSConfiguration wfsConfiguration(GeoServer geoServer) {
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        return new WFSConfiguration(geoServer, schemaBuilder, new WFS(schemaBuilder));
    }

    @Bean
    WMSController webMapServiceController(
            Dispatcher geoserverDispatcher,
            org.geoserver.ows.ClasspathPublisher classPathPublisher,
            VirtualServiceVerifier virtualServiceVerifier) {
        return new WMSController(geoserverDispatcher, classPathPublisher, virtualServiceVerifier);
    }

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }

    @ConditionalOnProperty(
            prefix = "geoserver.wms",
            name = "reflector.enabled",
            havingValue = "true",
            matchIfMissing = true)
    @Bean
    GetMapReflectorController getMapReflectorController(Dispatcher geoserverDispatcher) {
        return new GetMapReflectorController(geoserverDispatcher);
    }

    /**
     * Overrides the {@link #WMS_BEANS_BLACKLIST excluded wmsExceptionHandler} bean
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
