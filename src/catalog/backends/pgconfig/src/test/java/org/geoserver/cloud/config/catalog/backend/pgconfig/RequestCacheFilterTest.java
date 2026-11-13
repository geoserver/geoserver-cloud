/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.catalog.plugin.requestcache.RequestCacheThreadLocalTransfer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCacheFilterTest {

    private final RequestCacheFilter filter = new RequestCacheFilter(10);

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void closeScope() {
        RequestCacheScope.close();
    }

    @Test
    void filterOpensAndClosesScope() throws Exception {
        ScopeRecorder servlet = new ScopeRecorder();
        MockFilterChain chain = new MockFilterChain(servlet, filter);

        chain.doFilter(request, response);

        RequestCache seen = servlet.seen;
        assertThat(seen).isNotNull();
        assertThat(servlet.seenLive).isTrue();
        assertThat(seen.isDead()).isTrue();
        assertThat(RequestCacheScope.current()).isNull();
    }

    // if the filter also runs for forwards or includes, the nested call finds the scope open: it opens nothing and
    // closes nothing
    @Test
    void forwardKeepsOuterScope() throws Exception {
        ScopeRecorder forwarded = new ScopeRecorder();
        Forwarder servlet = new Forwarder(forwarded);
        MockFilterChain chain = new MockFilterChain(servlet, filter);

        chain.doFilter(request, response);

        RequestCache requestCache = servlet.beforeForward;
        assertThat(forwarded.seen).isSameAs(requestCache);
        assertThat(servlet.afterForward).isSameAs(requestCache);
        assertThat(servlet.liveAfterForward).isTrue();
        assertThat(requestCache.isDead()).isTrue();
    }

    // a thread-local cleanup on the thread of the request removed the cache before the request ended
    @Test
    void filterClosesItsOwnCache() throws Exception {
        ScopeRecorder servlet = new ScopeRecorder(new RequestCacheThreadLocalTransfer()::cleanup);
        MockFilterChain chain = new MockFilterChain(servlet, filter);

        chain.doFilter(request, response);

        assertThat(servlet.seen.isDead()).isTrue();
    }

    @Test
    void failedRequestClosesScope() {
        Failing servlet = new Failing();
        MockFilterChain chain = new MockFilterChain(servlet, filter);

        assertThatThrownBy(() -> chain.doFilter(request, response)).isInstanceOf(ServletException.class);

        assertThat(servlet.seen.isDead()).isTrue();
        assertThat(RequestCacheScope.current()).isNull();
    }

    /** Records the cache bound to the thread of the request, then runs an action on the thread. */
    private static class ScopeRecorder extends GenericServlet {
        private final transient Runnable afterRecording;
        private transient @Nullable RequestCache seen;
        private boolean seenLive;

        ScopeRecorder() {
            this(() -> {});
        }

        ScopeRecorder(Runnable afterRecording) {
            this.afterRecording = afterRecording;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) {
            seen = RequestCacheScope.current();
            seenLive = seen != null && !seen.isDead();
            afterRecording.run();
        }
    }

    /** Forwards the request through the filter again, and records the cache of the scope before and after. */
    private class Forwarder extends GenericServlet {
        private final transient ScopeRecorder target;
        private transient @Nullable RequestCache beforeForward;
        private transient @Nullable RequestCache afterForward;
        private boolean liveAfterForward;

        Forwarder(ScopeRecorder target) {
            this.target = target;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            beforeForward = RequestCacheScope.current();
            MockFilterChain forward = new MockFilterChain(target, filter);
            forward.doFilter(req, res);
            afterForward = RequestCacheScope.current();
            liveAfterForward = afterForward != null && !afterForward.isDead();
        }
    }

    /** Fails the request, after recording the cache of the scope. */
    private static class Failing extends GenericServlet {
        private transient @Nullable RequestCache seen;

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException {
            seen = RequestCacheScope.current();
            throw new ServletException("request failed");
        }
    }
}
