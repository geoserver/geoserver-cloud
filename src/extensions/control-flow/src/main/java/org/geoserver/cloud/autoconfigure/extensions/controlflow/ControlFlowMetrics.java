/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.FlowControllerProvider;

/**
 * Registers control-flow metrics to be exported by micrometer's {@link MeterRegistry}.
 *
 * <p>Global gauges, sampled from the {@link ControlFlowCallback} singleton:
 *
 * <ul>
 *   <li>{@literal geoserver.controlflow.requests.running}: requests admitted past flow control
 *   <li>{@literal geoserver.controlflow.requests.blocked}: requests blocked inside flow-control acquisition
 * </ul>
 *
 * <p>Per-rule gauges, resolved from the current {@link FlowControllerProvider} list via {@link ControlFlowRuleRows} and
 * tagged {@literal rule} (the rule key, e.g. {@literal ows.global}, {@literal ows.wms.getmap},
 * {@literal ip.192.168.1.1}) and {@literal controller} (the upstream {@link FlowController} implementation simple
 * name):
 *
 * <ul>
 *   <li>{@literal geoserver.controlflow.rule.limit}: configured concurrency or queue limit for the rule
 *   <li>{@literal geoserver.controlflow.rule.running}: requests currently admitted under the rule
 *   <li>{@literal geoserver.controlflow.rule.waiting}: requests waiting in the rule's priority queue
 *   <li>{@literal geoserver.controlflow.rule.queues.active}: live per-user or per-IP queues tracked by the rule
 *   <li>{@literal geoserver.controlflow.rule.rate.limit}: configured maximum requests per rate window
 * </ul>
 *
 * <p>{@literal running} and {@literal waiting} are separate meters on purpose: upstream's
 * {@code getRunningRequestsCount()} returns running requests on
 * {@link org.geoserver.flow.controller.SimpleThreadBlocker} but waiting requests on
 * {@link org.geoserver.flow.controller.PriorityThreadBlocker}; the binder normalizes the semantics instead of
 * propagating the trap.
 *
 * <p>All meters are tagged {@literal instance-id} when {@literal geoserver.metrics.instance-id} is configured.
 *
 * @since 3.1
 */
@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.metrics.controlflow")
class ControlFlowMetrics implements MeterBinder {

    private final @NonNull ControlFlowCallback callback;
    private final @NonNull FlowControllerProvider provider;
    private final String instanceId;

    private MultiGauge ruleLimit;
    private MultiGauge ruleRunning;
    private MultiGauge ruleWaiting;
    private MultiGauge ruleActiveQueues;
    private MultiGauge ruleRateLimit;

    private final AtomicReference<List<FlowController>> lastSeenControllers = new AtomicReference<>();
    private volatile boolean lastRefreshFailed;

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        registerGlobalGauge(
                registry,
                "geoserver.controlflow.requests.running",
                "Requests currently admitted past control-flow",
                ControlFlowCallback::getRunningRequests);
        registerGlobalGauge(
                registry,
                "geoserver.controlflow.requests.blocked",
                "Requests currently blocked inside control-flow acquisition",
                ControlFlowCallback::getBlockedRequests);
        ruleLimit = ruleGauge(
                registry,
                "geoserver.controlflow.rule.limit",
                "Configured concurrency or queue limit for the control-flow rule");
        ruleRunning = ruleGauge(
                registry,
                "geoserver.controlflow.rule.running",
                "Requests currently admitted under the control-flow rule");
        ruleWaiting = ruleGauge(
                registry, "geoserver.controlflow.rule.waiting", "Requests waiting in the rule's priority queue");
        ruleActiveQueues = ruleGauge(
                registry,
                "geoserver.controlflow.rule.queues.active",
                "Live per-user or per-IP queues tracked by the rule");
        ruleRateLimit = ruleGauge(
                registry, "geoserver.controlflow.rule.rate.limit", "Configured maximum requests per rate window");
        refreshRuleRows();
        log.info("GeoServer control-flow metrics enabled.");
    }

    private void registerGlobalGauge(
            MeterRegistry registry, String name, String description, ToDoubleFunction<ControlFlowCallback> value) {

        ToDoubleFunction<ControlFlowCallback> sampler = sampled -> {
            refreshRuleRows();
            return value.applyAsDouble(sampled);
        };
        Gauge.Builder<ControlFlowCallback> builder =
                Gauge.builder(name, callback, sampler).description(description);
        if (instanceId != null) {
            builder = builder.tag("instance-id", instanceId);
        }
        builder.register(registry);
    }

    private MultiGauge ruleGauge(MeterRegistry registry, String name, String description) {
        MultiGauge.Builder builder = MultiGauge.builder(name).description(description);
        if (instanceId != null) {
            builder = builder.tags(Tags.of("instance-id", instanceId));
        }
        return builder.register(registry);
    }

    /**
     * Re-resolves the current flow controller list and rebuilds the per-rule gauge rows when the list identity changed.
     * Sampling any global gauge invokes it, making rule changes take effect on the next scrape and dropping rows of
     * discarded controllers.
     */
    void refreshRuleRows() {
        if (ruleLimit == null) {
            // a scrape can hit the global gauges while bindTo() is still
            // registering the per-rule meters
            return;
        }
        List<FlowController> current;
        try {
            // DefaultFlowControllerProvider ignores the request argument and
            // returns the current list, rebuilding it if the configuration is stale
            current = provider.getFlowControllers(null);
            lastRefreshFailed = false;
        } catch (Exception e) {
            if (!lastRefreshFailed) {
                lastRefreshFailed = true;
                log.warn("Control-flow metrics could not resolve the flow controller list", e);
            }
            return;
        }
        if (current == lastSeenControllers.get()) {
            return;
        }
        synchronized (this) {
            if (current == lastSeenControllers.get()) {
                return;
            }
            registerRuleRows(ControlFlowRuleRows.resolve(current));
            lastSeenControllers.set(current);
        }
    }

    private void registerRuleRows(ControlFlowRuleRows rows) {
        ruleLimit.register(rows.limit(), true);
        ruleRunning.register(rows.running(), true);
        ruleWaiting.register(rows.waiting(), true);
        ruleActiveQueues.register(rows.activeQueues(), true);
        ruleRateLimit.register(rows.rateLimit(), true);
    }
}
