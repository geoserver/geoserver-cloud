/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.junit.jupiter.api.Test;

class SynchronizedResourceNotificationDispatcherTest {

    @Test
    void changedRunsUnderDispatcherLock() {
        SynchronizedResourceNotificationDispatcher dispatcher = new SynchronizedResourceNotificationDispatcher();
        List<Boolean> lockHeld = new ArrayList<>();
        dispatcher.addListener("styles/a.sld", notification -> lockHeld.add(Thread.holdsLock(dispatcher)));

        dispatcher.changed(new ResourceNotification("styles/a.sld", Kind.ENTRY_MODIFY, 1000L, List.of()));

        assertThat(lockHeld).containsExactly(true);
    }
}
