/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.junit.jupiter.api.Test;

class RequestCacheTest {

    private final RequestCache cache = new RequestCache(10);

    @Test
    void markResolvedOncePerObject() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();

        assertThat(cache.markResolved(ws)).isTrue();
        assertThat(cache.markResolved(ws)).isFalse();
    }

    // two equal objects are two instances, and each one holds its own references
    @Test
    void markResolvedByIdentity() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        WorkspaceInfoImpl copy = new WorkspaceInfoImpl();
        ws.setId("ws");
        copy.setId("ws");

        assertThat(cache.markResolved(ws)).isTrue();
        assertThat(cache.markResolved(copy)).isTrue();
    }

    @Test
    void clearForgetsResolved() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        cache.markResolved(ws);

        cache.clear();

        assertThat(cache.markResolved(ws)).isTrue();
    }

    @Test
    void deadCacheMarksNothing() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        cache.kill();

        assertThat(cache.markResolved(ws)).isTrue();
        assertThat(cache.markResolved(ws)).isTrue();
    }

    @Test
    void fullCacheMarksNothing() {
        RequestCache small = new RequestCache(1);
        WorkspaceInfoImpl first = new WorkspaceInfoImpl();
        WorkspaceInfoImpl second = new WorkspaceInfoImpl();

        assertThat(small.markResolved(first)).isTrue();
        assertThat(small.markResolved(second)).isTrue();
        assertThat(small.markResolved(second)).isTrue();
        assertThat(small.markResolved(first)).isFalse();
    }
}
