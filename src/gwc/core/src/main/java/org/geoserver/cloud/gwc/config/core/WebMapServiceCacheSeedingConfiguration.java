/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import static com.google.common.base.Preconditions.checkArgument;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.wms.CacheSeedingWebMapService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.util.HTTPWarningAppender;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration to create a decorator around {@link WebMapService} to set {@link
 * GeoServerTileLayer#WEB_MAP} as expected by {@link GeoServerTileLayer} when requested to seed a
 * tile.
 *
 * @since 1.0
 */
@Configuration
public class WebMapServiceCacheSeedingConfiguration {

    /**
     * AspectJ around advise on {@link DefaultWebMapService#getMap} to set {@link
     * GeoServerTileLayer#WEB_MAP} if the request came from a tile layer for seeding.
     *
     * <p>Replaces {@link CacheSeedingWebMapService} declared in {@literal
     * geowebcache-geoserver-wms-integration.xml} for simplicity and because its {@literal
     * wmsServiceInterceptor_SeedingWMS} pointcut advisor forces eager loading of the wms context
     * before the catalog is initialized, making {@link GetMapKvpRequestReader} constructor throw a
     * NPE.
     */
    @Bean
    WmsGetMapSeedingAspect gwcSeedingGetMapAdvise() {
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
