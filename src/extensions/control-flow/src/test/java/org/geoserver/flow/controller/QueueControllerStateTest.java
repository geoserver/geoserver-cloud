/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.ows.Request;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class QueueControllerStateTest {

    @Test
    void reportsActiveQueuesAndTotalQueuedRequests() {
        IpFlowController controller = new IpFlowController(2);
        assertThat(QueueControllerState.activeQueues(controller)).isZero();
        assertThat(QueueControllerState.totalQueued(controller)).isZero();

        Request fromFirstIp = request("10.0.0.1");
        Request fromSecondIp = request("10.0.0.2");
        controller.requestIncoming(fromFirstIp, -1);
        controller.requestIncoming(fromSecondIp, -1);

        assertThat(QueueControllerState.activeQueues(controller)).isEqualTo(2);
        assertThat(QueueControllerState.totalQueued(controller)).isEqualTo(2);

        // requestComplete resolves the queue from a thread-local set by the last
        // requestIncoming on this thread, completing in reverse order works
        controller.requestComplete(fromSecondIp);
        assertThat(QueueControllerState.totalQueued(controller)).isEqualTo(1);
        assertThat(QueueControllerState.activeQueues(controller)).isEqualTo(2);
    }

    private Request request(String remoteAddr) {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setRemoteAddr(remoteAddr);
        Request request = new Request();
        request.setHttpRequest(http);
        request.setHttpResponse(new MockHttpServletResponse());
        return request;
    }
}
