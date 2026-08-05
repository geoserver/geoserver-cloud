/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

/**
 * Read-only access to the package-private queue map of {@link QueueController} subclasses
 * ({@link UserConcurrentFlowController}, {@link IpFlowController}) for metrics sampling.
 *
 * <p>Lives in the {@code org.geoserver.flow.controller} package (a split package with the upstream
 * {@code gs-control-flow} jar) on purpose: the alternative is reflection, which would break at runtime instead of at
 * compile time when upstream renames a member.
 *
 * @since 3.1
 */
public final class QueueControllerState {

    private QueueControllerState() {
        // static utility
    }

    /** Live per-user or per-IP queues currently tracked by the controller. */
    public static int activeQueues(QueueController controller) {
        return controller.queues.size();
    }

    /** Requests currently admitted under the controller, summed across its queues. */
    public static int totalQueued(QueueController controller) {
        int total = 0;
        for (QueueController.TimedBlockingQueue queue : controller.queues.values()) {
            total += queue.size();
        }
        return total;
    }
}
