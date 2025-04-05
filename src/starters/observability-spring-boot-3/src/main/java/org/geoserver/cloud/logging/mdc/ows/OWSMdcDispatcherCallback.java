/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.ows;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.logging.mdc.config.GeoServerMdcConfigProperties;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.slf4j.MDC;

/**
 * A GeoServer {@link DispatcherCallback} that adds OWS (OGC Web Service) request information to the MDC.
 * <p>
 * This callback hooks into GeoServer's request dispatching process and adds OWS-specific
 * information to the MDC (Mapped Diagnostic Context). This information can include:
 * <ul>
 *   <li>Service name (WMS, WFS, etc.)</li>
 *   <li>Service version</li>
 *   <li>Output format</li>
 *   <li>Operation name (GetMap, GetFeature, etc.)</li>
 * </ul>
 * <p>
 * Adding this information to the MDC makes it available to all logging statements during
 * request processing, providing valuable context for debugging and monitoring OGC service requests.
 * <p>
 * The callback extends {@link AbstractDispatcherCallback} and implements {@link DispatcherCallback},
 * allowing it to interact with GeoServer's request dispatching process.
 *
 * @see GeoServerMdcConfigProperties.OWSMdcConfigProperties
 * @see org.slf4j.MDC
 */
@RequiredArgsConstructor
public class OWSMdcDispatcherCallback extends AbstractDispatcherCallback implements DispatcherCallback {

    private final @NonNull GeoServerMdcConfigProperties.OWSMdcConfigProperties config;

    /**
     * Callback method invoked when a service is dispatched.
     * <p>
     * This method adds service-specific information to the MDC based on the configuration
     * in {@link GeoServerMdcConfigProperties.OWSMdcConfigProperties}. The information can include:
     * <ul>
     *   <li>Service name (e.g., WMS, WFS)</li>
     *   <li>Service version</li>
     *   <li>Output format</li>
     * </ul>
     *
     * @param request the OWS request being processed
     * @param service the service that will handle the request
     * @return the service (unchanged)
     */
    @Override
    public Service serviceDispatched(Request request, Service service) {
        if (config.isServiceName()) MDC.put("gs.ows.service.name", service.getId());

        if (config.isServiceVersion()) MDC.put("gs.ows.service.version", String.valueOf(service.getVersion()));

        if (config.isServiceFormat() && null != request.getOutputFormat()) {
            MDC.put("gs.ows.service.format", request.getOutputFormat());
        }

        return super.serviceDispatched(request, service);
    }

    /**
     * Callback method invoked when an operation is dispatched.
     * <p>
     * This method adds operation-specific information to the MDC based on the configuration
     * in {@link GeoServerMdcConfigProperties.OWSMdcConfigProperties}. Currently, it adds
     * the operation name (e.g., GetMap, GetFeature) if configured.
     *
     * @param request the OWS request being processed
     * @param operation the operation that will handle the request
     * @return the operation (unchanged)
     */
    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if (config.isOperationName()) {
            MDC.put("gs.ows.service.operation", operation.getId());
        }
        return super.operationDispatched(request, operation);
    }
}
