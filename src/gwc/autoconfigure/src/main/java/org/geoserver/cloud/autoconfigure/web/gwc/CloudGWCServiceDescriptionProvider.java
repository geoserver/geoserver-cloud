package org.geoserver.cloud.autoconfigure.web.gwc;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.autoconfigure.gwc.GoServerWebUIConfigurationProperties;
import org.geoserver.cloud.autoconfigure.gwc.GoServerWebUIConfigurationProperties.CapabilitiesConfig;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.GWCServiceDescriptionProvider;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceDescriptionProvider;
import org.geoserver.web.ServiceLinkDescription;
import org.geotools.util.Version;

/**
 * GeoServer-Cloud replacement of {@link GWCServiceDescriptionProvider}
 * <p>
 * Doesn't check for the availability of the GWC service beans in the
 * application context, as they're not present in the web-ui service. E.g.:
 *
 * <pre>
 * {@code if (gwcConfig.isTMSEnabled())...}
 * </pre>
 *
 * instead of
 *
 * <pre>
 * {@code if (gwcConfig.isTMSEnabled() && null != app.getBean("gwcServiceTMS"))...
 * }
 */
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web.gwc")
class CloudGWCServiceDescriptionProvider extends ServiceDescriptionProvider {

    /** Service type to cross-link between service description and service link description. */
    public static final String SERVICE_TYPE = "WMTS";

    private final GWC gwc;
    private final GeoServer geoserver;

    /**
     * whether gwc services are available at all (i.e. deployed), regardless of its enabled config
     * state in {@link GWCConfig}
     */
    private GoServerWebUIConfigurationProperties staticConfig;

    public CloudGWCServiceDescriptionProvider(
            GWC gwc, GeoServer gs, GoServerWebUIConfigurationProperties staticConfig) {
        this.gwc = gwc;
        this.geoserver = gs;
        this.staticConfig = staticConfig;

        CapabilitiesConfig caps = staticConfig.getCapabilities();
        if (caps == null) {
            caps = new CapabilitiesConfig();
            staticConfig.setCapabilities(caps);
        }
        logEnabled(caps.isTms(), GoServerWebUIConfigurationProperties.CAPABILITIES_TMS);
        logEnabled(caps.isWmts(), GoServerWebUIConfigurationProperties.CAPABILITIES_WMTS);
        logEnabled(caps.isWmsc(), GoServerWebUIConfigurationProperties.CAPABILITIES_WMSC);
    }

    private void logEnabled(boolean value, String key) {
        log.info("{} {}", key, value ? "enabled" : "disabled");
    }

    /**
     * Lookup WMTSInfo using workspaceInfo / layerInfo context.
     *
     * @param workspaceInfo Workspace, or null for global.
     * @param layerInfo Layer, LayerGroup, or null for any
     * @return WMTSInfo if available for workspace, or global WMTSInfo.
     */
    protected WMTSInfo info(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        WMTSInfo info = null;
        if (workspaceInfo != null) {
            info = geoserver.getService(workspaceInfo, WMTSInfo.class);
        }
        if (info == null) {
            info = geoserver.getService(WMTSInfo.class);
        }
        return info;
    }

    /** GWC-based services don't have layer-specific enabling... */
    @Override
    protected boolean isAvailable(String serviceType, ServiceInfo service, PublishedInfo layer) {
        boolean layerEnabled = layer == null || layer.isEnabled();
        return service.isEnabled() && layerEnabled;
    }

    @Override
    public List<ServiceDescription> getServices(WorkspaceInfo workspace, PublishedInfo layer) {

        WMTSInfo info = info(workspace, layer);

        if (workspace != null || geoserver.getGlobal().isGlobalServices()) {
            return List.of(description(SERVICE_TYPE, info, workspace, layer));
        }
        return List.of();
    }

    private String basePath(WorkspaceInfo workspace, PublishedInfo layer) {

        String basePath = "/gwc/service";
        if (workspace != null) {
            basePath = "/%s%s".formatted(workspace.getName(), basePath);
        }
        if (layer != null) {
            basePath = "/%s%s".formatted(layer.getName(), basePath);
        }
        return "..%s".formatted(basePath);
    }

    private String createLinkWMSC(WorkspaceInfo workspace, PublishedInfo layer) {
        String basePath = basePath(workspace, layer);
        return "%s/wms?service=WMS&version=1.1.1&request=GetCapabilities&tiled=true".formatted(basePath);
    }

    private String createLinkWMTS(WorkspaceInfo workspace, PublishedInfo layer) {
        String basePath = basePath(workspace, layer);
        return "%s/wmts?service=WMTS&version=1.1.1&request=GetCapabilities".formatted(basePath);
    }

    private String createLinkTMS(WorkspaceInfo workspace, PublishedInfo layer) {
        String basePath = basePath(workspace, layer);
        return "%s/tms/1.0.0".formatted(basePath);
    }

    @Override
    public List<ServiceLinkDescription> getServiceLinks(WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {

        if (workspaceInfo == null && !geoserver.getGlobal().isGlobalServices()) {
            return List.of();
        }

        WMTSInfo wmtsInfo = info(workspaceInfo, layerInfo);
        CapabilitiesConfig caps = staticConfig.getCapabilities();

        final GWCConfig gwcConfig = gwc.getConfig();

        String workspace = workspaceInfo != null ? workspaceInfo.getName() : null;
        String layer = layerInfo != null ? layerInfo.getName() : null;

        List<ServiceLinkDescription> links = new ArrayList<>();

        boolean wmtsDeployed = caps.isWmts();
        boolean wmscDeployed = caps.isWmsc();
        boolean tmsDeployed = caps.isTms();

        if (wmtsDeployed && wmtsInfo.isEnabled()) {
            Version version = new Version("1.1.1");
            String link = createLinkWMTS(workspaceInfo, layerInfo);
            String protocol = "WMTS";
            links.add(serviceLink(workspace, layer, version, protocol, link));
        }

        if (wmscDeployed && gwcConfig.isWMSCEnabled()) {
            Version version = new Version("1.1.1");
            String protocol = "WMS-C";
            String link = createLinkWMSC(workspaceInfo, layerInfo);
            links.add(serviceLink(workspace, layer, version, protocol, link));
        }

        if (tmsDeployed && gwcConfig.isTMSEnabled()) {
            Version version = new Version("1.0.0");
            String link = createLinkTMS(workspaceInfo, layerInfo);
            String protocol = "TMS";
            links.add(serviceLink(workspace, layer, version, protocol, link));
        }

        return links;
    }

    private ServiceLinkDescription serviceLink(
            String workspaceName, String layerName, Version version, String protocol, String link) {
        return new ServiceLinkDescription(SERVICE_TYPE, version, link, workspaceName, layerName, protocol);
    }
}
