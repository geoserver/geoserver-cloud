/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.observability.logging.ows;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.observability.logging.config.MDCConfigProperties;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.slf4j.MDC;

@RequiredArgsConstructor
public class MDCDispatcherCallback extends AbstractDispatcherCallback implements DispatcherCallback {

    private final @NonNull MDCConfigProperties config;

    @Override
    public Service serviceDispatched(Request request, Service service) {
        if (config.isOws()) {
            MDC.put("gs.ows.service.name", service.getId());
            MDC.put("gs.ows.service.version", String.valueOf(service.getVersion()));
            if (null != request.getOutputFormat()) {
                MDC.put("gs.ows.service.format", request.getOutputFormat());
            }
        }
        return super.serviceDispatched(request, service);
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        if (config.isOws()) {
            MDC.put("gs.ows.service.operation", operation.getId());
        }
        return super.operationDispatched(request, operation);
    }
}
