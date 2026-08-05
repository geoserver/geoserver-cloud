/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.ows.Request;
import org.junit.jupiter.api.Test;

class MonitoredPriorityThreadBlockerTest {

    @Test
    void tracksAdmittedRequestsSeparatelyFromWaitingQueue() throws InterruptedException {
        MonitoredPriorityThreadBlocker blocker = new MonitoredPriorityThreadBlocker(1, request -> 0);
        Request first = new Request();
        Request second = new Request();

        assertThat(blocker.requestIncoming(first, -1)).isTrue();
        assertThat(blocker.getAdmittedCount()).isEqualTo(1);
        assertThat(blocker.getRunningRequestsCount()).as("waiting queue").isZero();

        // over the limit with a timeout: the superclass still moves the request
        // into its running set after the wait expires, the mirror must match
        assertThat(blocker.requestIncoming(second, 20)).isFalse();
        assertThat(blocker.getAdmittedCount()).isEqualTo(2);
        assertThat(blocker.getRunningRequestsCount()).as("waiting queue").isZero();

        blocker.requestComplete(second);
        assertThat(blocker.getAdmittedCount()).isEqualTo(1);

        blocker.requestComplete(first);
        assertThat(blocker.getAdmittedCount()).isZero();
    }
}
