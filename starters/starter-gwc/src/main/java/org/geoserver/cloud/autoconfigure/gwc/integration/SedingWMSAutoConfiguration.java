/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.integration;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.wms.CacheSeedingWebMapService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.util.HTTPWarningAppender;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ImportResource;

/**
 * Autoconfiguration to create a minimal {@link WebMapService} bean and its collaborators, to be
 * used by GeoWebCache for creating tile images.
 *
 * @since 1.0
 */
@Configuration
@ComponentScan
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = { //
        "jar:gs-wms-.*!/applicationContext.xml#name=^(?!getMapKvpReader).*$", //
        "jar:gs-wfs-.*!/applicationContext.xml#name="
                + SedingWMSAutoConfiguration.WFS_BEANS_REGEX //
    }
)
public class SedingWMSAutoConfiguration {

    static final String WFS_BEANS_REGEX =
            "^(gml.*OutputFormat|bboxKvpParser|xmlConfiguration.*|gml[1-9]*SchemaBuilder|wfsXsd.*|wfsSqlViewKvpParser).*$";

    @Bean
    @Autowired
    @DependsOn({"geoServerLoader", "wms"})
    @ConditionalOnMissingBean(GetMapKvpRequestReader.class)
    GetMapKvpRequestReader getMapKvpReader(WMS wms) {
        return new GetMapKvpRequestReader(wms);
    }

    /**
     * AspectJ around advise on {@link DefaultWebMapService#getMap} to set {@link
     * GeoServerTileLayer#WEB_MAP} if the request came from a tile layer for seeding.
     *
     * <p>Replaces {@link CacheSeedingWebMapService} declared in {@literal
     * geowebcache-geoserver-wms-integration.xml} for simplicity and because its {@literal
     * wmsServiceInterceptor_CachingWMS} pointcut advisor forces eager loading of the wms context
     * before the catalog is initialized, making {@link GetMapKvpRequestReader} constructor throw a
     * NPE.
     */
    public @Bean WmsGetMapSeedingAspect gwcSeedingGetMapAdvise() {
        return new WmsGetMapSeedingAspect();
    }

    @Aspect
    static class WmsGetMapSeedingAspect {

        /**
         * Wraps {@link WebMapService#getMap(GetMapRequest)}, called by the {@link Dispatcher}
         *
         * @see WebMapService#getMap(GetMapRequest)
         * @see
         *     org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
         */
        @Around("execution (* org.geoserver.wms.DefaultWebMapService.getMap(..))")
        public WebMap getMap(ProceedingJoinPoint joinPoint) throws Throwable {
            Object[] arguments = joinPoint.getArgs();
            checkArgument(arguments.length == 1);
            checkArgument(arguments[0] instanceof GetMapRequest);

            final GetMapRequest request = (GetMapRequest) arguments[0];

            WebMap map = (WebMap) joinPoint.proceed();

            final boolean isSeedingRequest = isInternalRequestForSeeding(request);
            if (isSeedingRequest) {
                GeoServerTileLayer.WEB_MAP.set(map);
                GeoServerTileLayer.DIMENSION_WARNINGS.set(HTTPWarningAppender.getWarnings());

                // returning null makes the Dispatcher ignore further processing the request
                return null;
            }

            return map;
        }

        protected boolean isInternalRequestForSeeding(final GetMapRequest request) {
            final Map<String, String> rawKvp = request.getRawKvp();
            boolean isSeedingRequest =
                    rawKvp != null
                            && rawKvp.containsKey(GeoServerTileLayer.GWC_SEED_INTERCEPT_TOKEN);
            return isSeedingRequest;
        }
    }
}
