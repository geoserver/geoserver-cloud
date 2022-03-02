/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.gwc;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.wicket.Component;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GoServerWebUIConfigurationProperties;
import org.geoserver.cloud.autoconfigure.gwc.GoServerWebUIConfigurationProperties.CapabilitiesConfig;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.GWCCapabilitiesHomePageProvider;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.CapabilitiesHomePageLinkProvider;
import org.geoserver.web.CapabilitiesHomePagePanel;
import org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo;
import org.geotools.util.Version;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

/**
 * Auto configuration to enabled GWC Wicket Web UI components.
 *
 * <p>Conditionals:
 *
 * <ul>
 *   <li>The {@literal gs-web-gwc} jar is in the classpath
 *   <li>{@literal gwc.enabled=true}: Core gwc integration is enabled
 *   <li>{@literal geoserver.web-ui.gwc.=true}: gwc web-ui integration is enabled
 * </ul>
 *
 * @since 1.0
 */
@Configuration
@ConditionalOnGeoServerWebUIEnabled
@EnableConfigurationProperties(GoServerWebUIConfigurationProperties.class)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class,
        locations = {
            "jar:gs-web-gwc-.*!/applicationContext.xml#name=^(?!"
                    + GeoServerWebUIAutoConfiguration.EXCLUDED_BEANS
                    + ").*$"
        })
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web.gwc")
public class GeoServerWebUIAutoConfiguration {

    /*
     * Disable disk-quota by brute force for now. We need to resolve how and where to store the
     * configuration and database.
     */
    static final String EXCLUDED_BEANS = "diskQuotaMenuPage|GWCCapabilitiesHomePageProvider";

    public @PostConstruct void log() {
        log.info("{} enabled", GoServerWebUIConfigurationProperties.ENABLED);
    }

    private void logEnabled(boolean value, String key) {
        log.info("{} {}", key, value ? "enabled" : "disabled");
    }

    @Bean(name = "GWCCapabilitiesHomePageProvider")
    CloudGwcCapabilitiesHomePageProvider cloudGwcCapabilitiesHomePageProvider(
            GoServerWebUIConfigurationProperties props, GeoServer geoServer) {

        CapabilitiesConfig caps = props.getCapabilities();
        if (caps == null) {
            caps = new CapabilitiesConfig();
            props.setCapabilities(caps);
        }
        logEnabled(caps.isTms(), GoServerWebUIConfigurationProperties.CAPABILITIES_TMS);
        logEnabled(caps.isWmts(), GoServerWebUIConfigurationProperties.CAPABILITIES_WMTS);
        logEnabled(caps.isWmsc(), GoServerWebUIConfigurationProperties.CAPABILITIES_WMSC);

        return new CloudGwcCapabilitiesHomePageProvider(props, geoServer);
    }

    /**
     * Replacement of {@link GWCCapabilitiesHomePageProvider} not requiring the actual GWC services
     * being exposed, since this is supposed to run on gs-web, and the capabilities won't be served
     * by the web-ui microservice, but by gwc-service
     *
     * @since 1.0
     */
    @RequiredArgsConstructor
    static class CloudGwcCapabilitiesHomePageProvider implements CapabilitiesHomePageLinkProvider {

        private final @NonNull GoServerWebUIConfigurationProperties staticConfig;
        private final @NonNull GeoServer geoServer;

        @Override
        public Component getCapabilitiesComponent(String id) {
            List<CapsInfo> getCapsUIContribs = new ArrayList<>();

            final GWCConfig liveConfig = GWC.get().getConfig();
            final CapabilitiesConfig staticCapabilitiesConfig = staticConfig.getCapabilities();

            if (staticCapabilitiesConfig.isWmsc() && liveConfig.isWMSCEnabled()) {
                getCapsUIContribs.add(
                        newCaps(
                                "WMS-C",
                                "1.1.1",
                                "../gwc/service/wms?request=GetCapabilities&version=1.1.1&tiled=true"));
            }
            WMTSInfo tmsInfo = geoServer.getService(WMTSInfo.class);
            if (staticCapabilitiesConfig.isTms() && tmsInfo != null && tmsInfo.isEnabled()) {
                getCapsUIContribs.add(
                        newCaps("WMTS", "1.0.0", "../gwc/service/wmts?REQUEST=GetCapabilities"));
            }

            if (staticCapabilitiesConfig.isTms() && liveConfig.isTMSEnabled()) {
                getCapsUIContribs.add(newCaps("TMS", "1.0.0", "../gwc/service/tms/1.0.0"));
            }

            return new CapabilitiesHomePagePanel(id, getCapsUIContribs);
        }

        private CapsInfo newCaps(String name, String version, String uri) {
            return new CapsInfo(name, new Version(version), uri);
        }
    }
}
