/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.flow.controller.PriorityProvider;
import org.geoserver.flow.controller.PriorityThreadBlocker;
import org.geoserver.ows.Request;

/**
 * A {@link PriorityThreadBlocker} that also tracks how many requests it has admitted.
 *
 * <p>The superclass keeps its running set private, and its {@link #getRunningRequestsCount()} returns the waiting-queue
 * size, not the running count. This subclass mirrors the superclass bookkeeping with a set keyed by {@link Request}
 * equality (a per-instance unique identifier, effectively identity): a request is added whenever
 * {@code super.requestIncoming} returns normally (the superclass does this even for a timed-out request, until
 * {@code requestComplete} removes it) and never when the wait is interrupted.
 *
 * @since 3.1
 */
class MonitoredPriorityThreadBlocker extends PriorityThreadBlocker {

    private final Set<Request> admitted = ConcurrentHashMap.newKeySet();

    public MonitoredPriorityThreadBlocker(int queueSize, PriorityProvider priorityProvider) {
        super(queueSize, priorityProvider);
    }

    @Override
    public boolean requestIncoming(Request request, long timeout) throws InterruptedException {
        boolean proceed = super.requestIncoming(request, timeout);
        admitted.add(request);
        return proceed;
    }

    @Override
    public void requestComplete(Request request) {
        super.requestComplete(request);
        admitted.remove(request);
    }

    /** Requests currently admitted under this blocker, feeding the {@literal rule.running} gauge. */
    public int getAdmittedCount() {
        return admitted.size();
    }
}
