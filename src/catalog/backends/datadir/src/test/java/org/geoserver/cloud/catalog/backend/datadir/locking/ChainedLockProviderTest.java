/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource.Lock;
import org.junit.jupiter.api.Test;

class ChainedLockProviderTest {

    @Test
    void acquiresBothLocksInOrder() {
        List<String> order = new ArrayList<>();
        LockProvider first = key -> {
            order.add("first-acquire");
            return () -> order.add("first-release");
        };
        LockProvider second = key -> {
            order.add("second-acquire");
            return () -> order.add("second-release");
        };

        ChainedLockProvider chained = new ChainedLockProvider(first, second);
        Lock lock = chained.acquire("testKey");

        assertThat(order).containsExactly("first-acquire", "second-acquire");

        lock.release();
        assertThat(order).containsExactly("first-acquire", "second-acquire", "second-release", "first-release");
    }

    @Test
    void releasesFirstLockIfSecondFails() {
        List<String> order = new ArrayList<>();
        LockProvider first = key -> {
            order.add("first-acquire");
            return () -> order.add("first-release");
        };
        LockProvider second = key -> {
            throw new RuntimeException("forced second lock failure");
        };

        ChainedLockProvider chained = new ChainedLockProvider(first, second);
        assertThatThrownBy(() -> chained.acquire("testKey"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("forced second lock failure");

        assertThat(order).containsExactly("first-acquire", "first-release");
    }

    @Test
    void nullFirstProviderRejected() {
        assertThatThrownBy(() -> new ChainedLockProvider(null, mock(LockProvider.class)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullSecondProviderRejected() {
        assertThatThrownBy(() -> new ChainedLockProvider(mock(LockProvider.class), null))
                .isInstanceOf(NullPointerException.class);
    }
}
