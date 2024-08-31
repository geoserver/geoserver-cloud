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

@RequiredArgsConstructor
public class OWSMdcDispatcherCallback extends AbstractDispatcherCallback implements DispatcherCallback {

    private final @NonNull GeoServerMdcConfigProperties.OWSMdcConfigProperties config;

    @Override
    public Service serviceDispatched(Request request, Service service) {
        if (config.isServiceName()) MDC.put("gs.ows.service.name", service.getId());

        if (config.isServiceVersion()) MDC.put("gs.ows.service.version", String.valueOf(service.getVersion()));

        if (config.isServiceFormat() && null != request.getOutputFormat()) {
            MDC.put("gs.ows.service.format", request.getOutputFormat());
        }

        return super.serviceDispatched(request, service);
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if (config.isOperationName()) {
            MDC.put("gs.ows.service.operation", operation.getId());
        }
        return super.operationDispatched(request, operation);
    }
}
