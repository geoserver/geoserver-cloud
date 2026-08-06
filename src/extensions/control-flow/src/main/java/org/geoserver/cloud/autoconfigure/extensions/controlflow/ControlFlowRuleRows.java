/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import com.google.common.base.Predicate;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.controller.CookieKeyGenerator;
import org.geoserver.flow.controller.IpFlowController;
import org.geoserver.flow.controller.IpRequestMatcher;
import org.geoserver.flow.controller.OWSRequestMatcher;
import org.geoserver.flow.controller.PriorityThreadBlocker;
import org.geoserver.flow.controller.QueueController;
import org.geoserver.flow.controller.QueueControllerState;
import org.geoserver.flow.controller.RateFlowController;
import org.geoserver.flow.controller.SimpleThreadBlocker;
import org.geoserver.flow.controller.SingleQueueFlowController;
import org.geoserver.flow.controller.ThreadBlocker;
import org.geoserver.flow.controller.UserConcurrentFlowController;

/**
 * Resolves the per-rule gauge rows for a flow controller list.
 *
 * <p>Rule keys and limits derive from public state, identically for controllers built by
 * {@link PropertiesControlFlowConfigurator} (externalized config) and by the upstream data-directory configurator:
 * {@code getPriority()} returns the configured queue size on every queue-based controller, matchers expose their OWS
 * service/method/format and single IPs, and rate controllers expose their window limit and key generator. Rows sample
 * live controller objects; a sampling failure yields {@code NaN} instead of breaking the scrape.
 */
@Slf4j(topic = "org.geoserver.cloud.metrics.controlflow")
class ControlFlowRuleRows {

    private final List<MultiGauge.Row<?>> limit = new ArrayList<>();
    private final List<MultiGauge.Row<?>> running = new ArrayList<>();
    private final List<MultiGauge.Row<?>> waiting = new ArrayList<>();
    private final List<MultiGauge.Row<?>> activeQueues = new ArrayList<>();
    private final List<MultiGauge.Row<?>> rateLimit = new ArrayList<>();

    static ControlFlowRuleRows resolve(List<FlowController> controllers) {
        ControlFlowRuleRows rows = new ControlFlowRuleRows();
        controllers.forEach(rows::add);
        return rows;
    }

    List<MultiGauge.Row<?>> limit() {
        return limit;
    }

    List<MultiGauge.Row<?>> running() {
        return running;
    }

    List<MultiGauge.Row<?>> waiting() {
        return waiting;
    }

    List<MultiGauge.Row<?>> activeQueues() {
        return activeQueues;
    }

    List<MultiGauge.Row<?>> rateLimit() {
        return rateLimit;
    }

    private void add(FlowController controller) {
        if (controller instanceof RateFlowController rate) {
            addRateRule(rate);
        } else if (controller instanceof UserConcurrentFlowController user) {
            addQueueRule("user", user);
        } else if (controller instanceof IpFlowController ip) {
            addQueueRule("ip", ip);
        } else if (controller instanceof SingleQueueFlowController singleQueue) {
            addSingleQueueRule(singleQueue);
        }
        // other FlowController implementations contribute no per-rule rows
    }

    private void addSingleQueueRule(SingleQueueFlowController controller) {
        String rule = singleQueueRuleKey(controller);
        if (rule == null) {
            return;
        }
        Tags tags = ruleTags(rule, controller);
        limit.add(row(tags, controller, SingleQueueFlowController::getPriority));
        addBlockerRows(tags, controller.getBlocker());
    }

    private void addBlockerRows(Tags tags, ThreadBlocker blocker) {
        if (blocker instanceof MonitoredPriorityThreadBlocker monitored) {
            running.add(row(tags, monitored, MonitoredPriorityThreadBlocker::getAdmittedCount));
            waiting.add(row(tags, monitored, PriorityThreadBlocker::getRunningRequestsCount));
        } else if (blocker instanceof PriorityThreadBlocker priority) {
            // upstream getRunningRequestsCount() reports the waiting queue here;
            // the running count is private state with no accessor
            waiting.add(row(tags, priority, PriorityThreadBlocker::getRunningRequestsCount));
        } else if (blocker instanceof SimpleThreadBlocker simple) {
            running.add(row(tags, simple, SimpleThreadBlocker::getRunningRequestsCount));
        }
    }

    private String singleQueueRuleKey(SingleQueueFlowController controller) {
        // guava's Predicate, which upstream's OWSRequestMatcher and IpRequestMatcher implement
        Predicate<?> matcher = controller.getMatcher();
        if (matcher instanceof IpRequestMatcher ipMatcher) {
            return "ip." + ipMatcher.getIp();
        }
        if (matcher instanceof OWSRequestMatcher owsMatcher) {
            return owsRuleKey(owsMatcher);
        }
        return null;
    }

    private String owsRuleKey(OWSRequestMatcher matcher) {
        if (matcher.getService() == null) {
            return "ows.global";
        }
        StringBuilder key = new StringBuilder("ows.").append(matcher.getService());
        if (matcher.getMethod() != null) {
            key.append('.').append(matcher.getMethod());
            if (matcher.getOutputFormat() != null) {
                key.append('.').append(matcher.getOutputFormat());
            }
        }
        return key.toString();
    }

    private void addRateRule(RateFlowController controller) {
        Tags tags = ruleTags(rateRuleKey(controller), controller);
        rateLimit.add(row(tags, controller, RateFlowController::getMaxRequests));
    }

    private void addQueueRule(String rule, QueueController controller) {
        Tags tags = ruleTags(rule, controller);
        limit.add(row(tags, controller, QueueController::getPriority));
        running.add(row(tags, controller, QueueControllerState::totalQueued));
        activeQueues.add(row(tags, controller, QueueControllerState::activeQueues));
    }

    private String rateRuleKey(RateFlowController controller) {
        String prefix = controller.getKeyGenerator() instanceof CookieKeyGenerator ? "user.ows" : "ip.ows";
        if (controller.getMatcher() instanceof OWSRequestMatcher owsMatcher && owsMatcher.getService() != null) {
            String owsSuffix = owsRuleKey(owsMatcher).substring("ows".length());
            return prefix + owsSuffix;
        }
        return prefix;
    }

    private Tags ruleTags(String rule, FlowController controller) {
        return Tags.of("rule", rule, "controller", controller.getClass().getSimpleName());
    }

    private static <T> MultiGauge.Row<T> row(Tags tags, T sampled, ToDoubleFunction<T> value) {
        return MultiGauge.Row.of(tags, sampled, guarded(value));
    }

    private static <T> ToDoubleFunction<T> guarded(ToDoubleFunction<T> value) {
        return sampled -> {
            try {
                return value.applyAsDouble(sampled);
            } catch (RuntimeException e) {
                log.debug("control-flow rule gauge sampling failed, reporting NaN", e);
                return Double.NaN;
            }
        };
    }
}
