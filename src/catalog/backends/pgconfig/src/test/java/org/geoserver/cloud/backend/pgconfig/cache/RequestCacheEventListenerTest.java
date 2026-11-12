/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.remote.resourcepool.RemoteEventResourcePoolProcessor;
import org.junit.jupiter.api.Test;

class RequestCacheEventListenerTest {

    private final RemoteChangeCounter remoteChanges = new RemoteChangeCounter();
    private final RequestCacheEventListener listener = new RequestCacheEventListener(remoteChanges);

    @Test
    void remoteEventCountsOnce() {
        CatalogInfoRemoved removed = removedWorkspace();
        removed.setRemote(true);

        listener.onUpdateSequence(removed);

        assertThat(remoteChanges.count()).isOne();
    }

    // the facades evict the changes made on this instance
    @Test
    void localEventNotCounted() {
        listener.onUpdateSequence(removedWorkspace());

        assertThat(remoteChanges.count()).isZero();
    }

    @Test
    void runsAfterEntityEvictions() {
        assertThat(RequestCacheEventListener.ORDER).isGreaterThan(EntityCacheEventListener.ORDER);
    }

    // a request looking a changed store up before the pool cleanup must read the changed store
    @Test
    void runsBeforePoolCleanup() {
        assertThat(RequestCacheEventListener.ORDER)
                .isLessThan(RemoteEventResourcePoolProcessor.CLEANUP_AFTER_CHANGE_ORDER);
    }

    private static CatalogInfoRemoved removedWorkspace() {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("ws-1");
        workspace.setName("topp");
        return CatalogInfoRemoved.createLocal(1L, workspace);
    }
}
