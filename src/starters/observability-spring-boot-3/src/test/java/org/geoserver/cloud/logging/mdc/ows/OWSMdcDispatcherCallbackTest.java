/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.ows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.cloud.logging.mdc.config.GeoServerMdcConfigProperties;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Tests for the OWSMdcDispatcherCallback.
 * <p>
 * This test class ensures that the {@link OWSMdcDispatcherCallback} correctly adds
 * GeoServer OWS-specific information to the MDC.
 */
class OWSMdcDispatcherCallbackTest {

    private GeoServerMdcConfigProperties.OWSMdcConfigProperties config;

    private OWSMdcDispatcherCallback callback;

    private Request request;
    private Service service;

    @BeforeEach
    void setup() {
        // Clear MDC before each test
        MDC.clear();

        // Initialize config object
        config = new GeoServerMdcConfigProperties.OWSMdcConfigProperties();
        config.setServiceName(true);
        config.setServiceVersion(true);
        config.setServiceFormat(true);
        config.setOperationName(true);

        callback = new OWSMdcDispatcherCallback(config);

        request = new Request();
        request.setOutputFormat("image/png");
        service = service("wms", "1.1.1", "GetCapabilities", "GetMap", "DescribeLayer", "GetFeatureInfo");
    }

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void testServiceDispatched() {
        callback.serviceDispatched(request, service);

        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .isNotNull()
                .containsEntry("gs.ows.service.name", "wms")
                .containsEntry("gs.ows.service.version", "1.1.1")
                .containsEntry("gs.ows.service.format", "image/png");
    }

    @Test
    void testServiceDispatchedWithDisabledProperties() {
        // Disable some properties
        config.setServiceVersion(false);
        config.setServiceFormat(false);

        callback.serviceDispatched(request, service);

        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .isNotNull()
                .containsEntry("gs.ows.service.name", "wms")
                .doesNotContainKey("gs.ows.service.version")
                .doesNotContainKey("gs.ows.service.format");
    }

    @Test
    void testOperationDispatched() {
        String opName = "GetMap";
        Operation operation = operation(opName);

        callback.operationDispatched(request, operation);

        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap).isNotNull().containsEntry("gs.ows.service.operation", opName);
    }

    @Test
    void testOperationDispatchedWithDisabledProperties() {
        // Disable operation name
        config.setOperationName(false);

        Operation operation = operation("GetFeatureInfo");

        callback.operationDispatched(request, operation);

        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        if (mdcMap != null) {
            assertThat(mdcMap).doesNotContainKey("gs.ows.service.operation");
        }
    }

    @Test
    void testNullOutputFormat() {
        request.setOutputFormat(null);

        callback.serviceDispatched(request, service);

        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .isNotNull()
                .containsEntry("gs.ows.service.name", "wms")
                .containsEntry("gs.ows.service.version", "1.1.1")
                .doesNotContainKey("gs.ows.service.format");
    }

    private Service service(String id, String version, String... operations) {
        List<String> ops = Arrays.asList(operations);
        Object serviceObject = null; // unused
        return new Service(id, serviceObject, new Version(version), ops);
    }

    private Operation operation(String id) {
        // For simplicity, we only need the id for testing
        return new Operation(id, service, null, new Object[0]);
    }
}
