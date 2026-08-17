/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.List;
import java.util.Properties;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.flow.DefaultFlowControllerProvider;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.FlowControllerProvider;
import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.IpFlowController;
import org.geoserver.flow.controller.PriorityThreadBlocker;
import org.geoserver.ows.Request;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ControlFlowMetricsTest {

    @Test
    void registersGlobalRequestGauges() {
        ControlFlowCallback callback = mock(ControlFlowCallback.class);
        when(callback.getRunningRequests()).thenReturn(6L);
        when(callback.getBlockedRequests()).thenReturn(2L);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ControlFlowMetrics(callback, stubProvider(List.of()), "test-instance").bindTo(registry);

        assertThat(registry.get("geoserver.controlflow.requests.running")
                        .tag("instance-id", "test-instance")
                        .gauge()
                        .value())
                .isEqualTo(6.0);
        assertThat(registry.get("geoserver.controlflow.requests.blocked")
                        .tag("instance-id", "test-instance")
                        .gauge()
                        .value())
                .isEqualTo(2.0);
    }

    @Test
    void omitsInstanceIdTagWhenNotConfigured() {
        ControlFlowCallback callback = mock(ControlFlowCallback.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ControlFlowMetrics(callback, stubProvider(List.of()), null).bindTo(registry);

        assertThat(registry.get("geoserver.controlflow.requests.running")
                        .gauge()
                        .getId()
                        .getTag("instance-id"))
                .isNull();
    }

    @Test
    void perRuleGaugesFromExternalizedConfiguration() {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");
        rules.setProperty("ows.wms.getmap", "2");
        rules.setProperty("user.ows.wfs.getfeature", "5/s");

        SimpleMeterRegistry registry = bindToRegistry(externalizedProvider(rules));

        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.global", 8);
        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.wms.getmap", 2);
        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "ows.global", 0);
        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "ows.wms.getmap", 0);
        assertRuleGauge(registry, "geoserver.controlflow.rule.rate.limit", "user.ows.wfs.getfeature", 5);
        assertThat(registry.find("geoserver.controlflow.rule.waiting").gauges())
                .as("no waiting gauges without a priority provider")
                .isEmpty();
    }

    @Test
    void controllerTagReportsUpstreamType() {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");

        SimpleMeterRegistry registry = bindToRegistry(externalizedProvider(rules));

        Gauge limit = registry.get("geoserver.controlflow.rule.limit")
                .tag("rule", "ows.global")
                .gauge();
        assertThat(limit.getId().getTag("controller")).isEqualTo("GlobalFlowController");
    }

    @Test
    void priorityRulesReportRunningAndWaiting() {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");
        rules.setProperty("ows.priority.http", "gs-priority,3");

        SimpleMeterRegistry registry = bindToRegistry(externalizedProvider(rules));

        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "ows.global", 0);
        assertRuleGauge(registry, "geoserver.controlflow.rule.waiting", "ows.global", 0);
    }

    @Test
    void plainPriorityBlockerOmitsRunningGauge() {
        GlobalFlowController plain = new GlobalFlowController(8, new PriorityThreadBlocker(8, request -> 0));

        SimpleMeterRegistry registry = bindToRegistry(stubProvider(List.of(plain)));

        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.global", 8);
        assertRuleGauge(registry, "geoserver.controlflow.rule.waiting", "ows.global", 0);
        assertThat(registry.find("geoserver.controlflow.rule.running")
                        .tag("rule", "ows.global")
                        .gauge())
                .as("running is not observable on a plain PriorityThreadBlocker")
                .isNull();
    }

    @Test
    void admittedRequestSeparatesRunningFromWaiting() throws InterruptedException {
        MonitoredPriorityThreadBlocker blocker = new MonitoredPriorityThreadBlocker(8, request -> 0);
        blocker.requestIncoming(new Request(), 0);
        GlobalFlowController controller = new GlobalFlowController(8, blocker);

        SimpleMeterRegistry registry = bindToRegistry(stubProvider(List.of(controller)));

        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "ows.global", 1);
        assertRuleGauge(registry, "geoserver.controlflow.rule.waiting", "ows.global", 0);
    }

    @Test
    void queueRuleGaugesFromExternalizedConfiguration() {
        Properties rules = new Properties();
        rules.setProperty("user", "4");
        rules.setProperty("ip", "6");

        SimpleMeterRegistry registry = bindToRegistry(externalizedProvider(rules));

        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "user", 4);
        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ip", 6);
        assertRuleGauge(registry, "geoserver.controlflow.rule.queues.active", "user", 0);
        assertRuleGauge(registry, "geoserver.controlflow.rule.queues.active", "ip", 0);
        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "user", 0);
        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "ip", 0);
    }

    @Test
    void queueRuleGaugesTrackLiveQueues() {
        IpFlowController controller = new IpFlowController(2);
        SimpleMeterRegistry registry = bindToRegistry(stubProvider(List.of(controller)));

        Request req1 = owsRequest("10.0.0.1");
        Request req2 = owsRequest("10.0.0.2");

        controller.requestIncoming(req1, -1);
        controller.requestIncoming(req2, -1);

        assertRuleGauge(registry, "geoserver.controlflow.rule.queues.active", "ip", 2);
        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "ip", 2);

        controller.requestComplete(req2);
        // requestComplete resolves the queue from the thread-local of the last
        // incoming request on this thread (10.0.0.2), one request leaves
        assertRuleGauge(registry, "geoserver.controlflow.rule.running", "ip", 1);
    }

    @Test
    void ruleChangesApplyOnNextSample() {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");
        PropertiesControlFlowConfigurator configurator = new PropertiesControlFlowConfigurator(rules);
        DefaultFlowControllerProvider provider = new DefaultFlowControllerProvider(configurator);
        configurator.setStale(false);

        SimpleMeterRegistry registry = bindToRegistry(provider);
        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.global", 8);

        rules.setProperty("ows.global", "16");
        rules.setProperty("ows.wms.getmap", "2");
        configurator.setStale(true);
        sampleGlobalGauge(registry);

        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.global", 16);
        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.wms.getmap", 2);
    }

    @Test
    void removedRulesStopBeingSampled() {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");
        rules.setProperty("ows.wms.getmap", "2");
        PropertiesControlFlowConfigurator configurator = new PropertiesControlFlowConfigurator(rules);
        DefaultFlowControllerProvider provider = new DefaultFlowControllerProvider(configurator);
        configurator.setStale(false);

        SimpleMeterRegistry registry = bindToRegistry(provider);
        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.wms.getmap", 2);

        rules.remove("ows.wms.getmap");
        configurator.setStale(true);
        sampleGlobalGauge(registry);

        assertThat(registry.find("geoserver.controlflow.rule.limit")
                        .tag("rule", "ows.wms.getmap")
                        .gauge())
                .as("row of a removed rule")
                .isNull();
        assertRuleGauge(registry, "geoserver.controlflow.rule.limit", "ows.global", 8);
    }

    @Test
    void providerFailureKeepsGlobalGaugesAlive() {
        ControlFlowCallback callback = mock(ControlFlowCallback.class);
        when(callback.getRunningRequests()).thenReturn(3L);
        FlowControllerProvider failing = new FlowControllerProvider() {
            @Override
            public List<FlowController> getFlowControllers(Request request) {
                throw new IllegalStateException("boom");
            }

            @Override
            public long getTimeout(Request request) {
                return -1;
            }
        };

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ControlFlowMetrics(callback, failing, null).bindTo(registry);

        assertThat(registry.get("geoserver.controlflow.requests.running")
                        .gauge()
                        .value())
                .isEqualTo(3.0);
    }

    @Test
    void prometheusRenderedNamesMatchDocumentation() {
        Properties rules = new Properties();
        rules.setProperty("ows.global", "8");
        rules.setProperty("user", "4");
        rules.setProperty("user.ows.wfs.getfeature", "5/s");

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new ControlFlowMetrics(mock(ControlFlowCallback.class), externalizedProvider(rules), null).bindTo(registry);

        String scrape = registry.scrape();

        assertThat(scrape)
                .as("global running gauge keeps its documented name")
                .contains("geoserver_controlflow_requests_running")
                .as("rule limit gauge keeps its documented name")
                .contains("geoserver_controlflow_rule_limit")
                .as("rule active queues gauge keeps its documented name")
                .contains("geoserver_controlflow_rule_queues_active")
                .as("rule rate limit gauge keeps its documented name")
                .contains("geoserver_controlflow_rule_rate_limit")
                .as("no base-unit suffix on the global running gauge")
                .doesNotContain("_running_requests")
                .as("no base-unit suffix on the rule active queues gauge")
                .doesNotContain("_active_queues");
    }

    static FlowControllerProvider stubProvider(List<FlowController> controllers) {
        return new FlowControllerProvider() {
            @Override
            public List<FlowController> getFlowControllers(Request request) {
                return controllers;
            }

            @Override
            public long getTimeout(Request request) {
                return -1;
            }
        };
    }

    private SimpleMeterRegistry bindToRegistry(FlowControllerProvider provider) {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ControlFlowMetrics(mock(ControlFlowCallback.class), provider, null).bindTo(registry);
        return registry;
    }

    private DefaultFlowControllerProvider externalizedProvider(Properties rules) {
        PropertiesControlFlowConfigurator configurator = new PropertiesControlFlowConfigurator(rules);
        DefaultFlowControllerProvider provider = new DefaultFlowControllerProvider(configurator);
        // mirrors the auto-configuration wiring: the constructor built the
        // controllers, mark the configurator fresh to stop per-call rebuilds
        configurator.setStale(false);
        return provider;
    }

    private void assertRuleGauge(SimpleMeterRegistry registry, String meter, String rule, double expected) {
        Gauge gauge = registry.find(meter).tag("rule", rule).gauge();
        assertThat(gauge).as("%s{rule=%s}", meter, rule).isNotNull();
        assertThat(gauge.value()).as("%s{rule=%s}", meter, rule).isEqualTo(expected);
    }

    private void sampleGlobalGauge(SimpleMeterRegistry registry) {
        registry.get("geoserver.controlflow.requests.running").gauge().value();
    }

    private Request owsRequest(String remoteAddr) {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setRemoteAddr(remoteAddr);
        Request request = new Request();
        request.setHttpRequest(http);
        request.setHttpResponse(new MockHttpServletResponse());
        return request;
    }
}
