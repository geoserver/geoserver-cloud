/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.integration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import static org.geowebcache.conveyor.Conveyor.CacheResult.MISS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnDirectWMSIntegrationEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.GeoServerIntegrationAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.geoserver.gwc.wms.CachingExtendedCapabilitiesProvider;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.web.HeaderContribution;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.RawMap;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletResponse;

/**
 * Autoconfiguration to integrate GWC with GeoServer's WMS
 *
 * <p>GeoWebCache can be transparently integrated with the GeoServer WMS, effectively converting the
 * regular WMS in a <a href="https://wiki.osgeo.org/wiki/WMS_Tile_Caching">WMS-C</a>.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Import({
    WMSIntegrationAutoConfiguration.Enabled.class,
    WMSIntegrationAutoConfiguration.Disabled.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.integration")
public class WMSIntegrationAutoConfiguration {

    @ConditionalOnDirectWMSIntegrationEnabled
    static @Configuration class Enabled {

        /**
         * Originally declared in {@literal geowebcache-geoserver-context.xml} and excluded by
         * {@link GeoServerIntegrationAutoConfiguration}. Contributed to the application context
         * here only if direct-WMS integration is {@link ConditionalOnDirectWMSIntegrationEnabled
         * enabled}.
         *
         * @param gwc
         */
        @ConditionalOnBean(name = {"wmsServiceTarget", "wms_1_1_1_GetCapabilitiesResponse"})
        public @Bean CachingExtendedCapabilitiesProvider gwcWMSExtendedCapabilitiesProvider(
                GWC gwc) {
            log.info("GeoWebCache direct WMS integration enabled");
            return new CachingExtendedCapabilitiesProvider(gwc);
        }

        /**
         * AspectJ around advise on {@link DefaultWebMapService#getMap} to serve the WMS GetMap
         * request through GWC if the parameters match a tile.
         *
         * <p>Replaces {@link org.geoserver.gwc.wms.CachingWebMapService} declared in {@literal
         * geowebcache-geoserver-wms-integration.xml} for simplicity and because its {@literal
         * wmsServiceInterceptor_SeedingWMS} pointcut advisor forces eager loading of the wms
         * context before the catalog is initialized, making {@link GetMapKvpRequestReader}
         * constructor throw a NPE.
         *
         * @param gwc
         */
        @ConditionalOnBean(name = {"wmsServiceTarget", "wms_1_1_1_GetCapabilitiesResponse"})
        public @Bean ForwardGetMapToGwcAspect gwcGetMapAdvise(GWC gwc) {
            return new ForwardGetMapToGwcAspect(gwc);
        }
    }

    @ConditionalOnProperty(
            name = GeoWebCacheConfigurationProperties.WMS_INTEGRATION_ENABLED,
            havingValue = "false",
            matchIfMissing = true)
    @Import(Disabled.GeoServerWebUI.class)
    static @Configuration class Disabled {

        /**
         * If direct-WMS integration is disabled, contribute {@literal wms-integration-disabled.css}
         * to the Wicket {@link GWCSettingsPage} to hide the {@literal Enable direct integration
         * with GeoServer WMS} and {@literal Explicitly require TILED Parameter} form elements.
         */
        @ConditionalOnGeoServerWebUIEnabled
        static @Configuration class GeoServerWebUI {

            public @Bean HeaderContribution GWCSettingsPage_WMSIntegationDisabledCssContribution() {
                log.info("GeoWebCache direct WMS integration disabled in GWCSettingsPage");
                HeaderContribution contribution = new HeaderContribution();
                contribution.setScope(GWCSettingsPage.class);
                contribution.setCSSFilename("wms-integration-disabled.css");
                return contribution;
            }
        }
    }

    @Aspect
    @Slf4j(topic = "org.geoserver.cloud.gwc.integration.wms")
    @RequiredArgsConstructor
    static class ForwardGetMapToGwcAspect {

        private final GWC gwc;

        /**
         * Wraps {@link WebMapService#getMap(GetMapRequest)}, called by the {@link Dispatcher}
         *
         * @see WebMapService#getMap(GetMapRequest)
         * @see
         *     org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
         */
        @Around("execution (* org.geoserver.wms.DefaultWebMapService.getMap(..))")
        public WebMap getMap(ProceedingJoinPoint joinPoint) throws Throwable {
            final GWCConfig config = gwc.getConfig();
            final GetMapRequest request = getRequest(joinPoint);
            final boolean enabled = config.isDirectWMSIntegrationEnabled();
            final boolean tiled = request.isTiled() || !config.isRequireTiledParameter();
            if (!(enabled && tiled)) {
                return (WebMap) joinPoint.proceed();
            }

            final StringBuilder requestMistmatchTarget = new StringBuilder();
            ConveyorTile cachedTile = gwc.dispatch(request, requestMistmatchTarget);

            if (cachedTile == null) {
                WebMap dynamicResult = (WebMap) joinPoint.proceed();
                dynamicResult.setResponseHeader("geowebcache-cache-result", MISS.toString());
                dynamicResult.setResponseHeader(
                        "geowebcache-miss-reason", requestMistmatchTarget.toString());
                return dynamicResult;
            }
            checkState(cachedTile.getTileLayer() != null);
            final TileLayer layer = cachedTile.getTileLayer();

            log.trace("GetMap request intercepted, serving cached content: {}", request);

            final byte[] tileBytes;
            {
                final Resource mapContents = cachedTile.getBlob();
                if (mapContents instanceof ByteArrayResource) {
                    tileBytes = ((ByteArrayResource) mapContents).getContents();
                } else {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    mapContents.transferTo(Channels.newChannel(out));
                    tileBytes = out.toByteArray();
                }
            }

            // Handle Etags
            final String ifNoneMatch = request.getHttpRequestHeader("If-None-Match");
            final String etag = GWC.getETag(tileBytes);
            if (etag.equals(ifNoneMatch)) {
                // Client already has the current version
                log.trace("ETag matches, returning 304");
                throw new HttpErrorCodeException(HttpServletResponse.SC_NOT_MODIFIED);
            }

            log.trace("No matching ETag, returning cached tile");
            final String mimeType = cachedTile.getMimeType().getMimeType();

            RawMap map = new RawMap((WMSMapContent) null, tileBytes, mimeType);

            map.setContentDispositionHeader(
                    (WMSMapContent) null, "." + cachedTile.getMimeType().getFileExtension(), false);

            LinkedHashMap<String, String> headers = new LinkedHashMap<>();
            GWC.setCacheControlHeaders(headers, layer, (int) cachedTile.getTileIndex()[2]);
            GWC.setConditionalGetHeaders(
                    headers, cachedTile, etag, request.getHttpRequestHeader("If-Modified-Since"));
            GWC.setCacheMetadataHeaders(headers, cachedTile, layer);
            headers.forEach((k, v) -> map.setResponseHeader(k, v));

            return map;
        }

        private GetMapRequest getRequest(ProceedingJoinPoint joinPoint) {

            final Object[] arguments = joinPoint.getArgs();

            checkArgument(arguments.length == 1);
            checkArgument(arguments[0] instanceof GetMapRequest);

            return (GetMapRequest) arguments[0];
        }
    }
}
