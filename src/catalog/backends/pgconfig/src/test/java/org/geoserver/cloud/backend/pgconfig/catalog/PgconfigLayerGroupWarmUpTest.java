/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BooleanSupplier;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigLayerGroupRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PgconfigLayerGroupWarmUpTest {

    private static final long ONE_MIB = 1024 * 1024;

    private final PgconfigLayerGroupRepository groups = mock(PgconfigLayerGroupRepository.class);
    private final EntityCache cache = new EntityCache("gs-catalog", ONE_MIB, Duration.ofSeconds(30), new NoopCodec());
    private final PgconfigLayerGroupWarmUp warmUp = new PgconfigLayerGroupWarmUp(groups, cache);

    // a reset while the warm-up of a reload runs: that run stops at its next page, and the new run goes on
    @Test
    void newRunCancelsPrevious() throws InterruptedException {
        BlockingQueue<BooleanSupplier> runs = new LinkedBlockingQueue<>();
        CountDownLatch release = new CountDownLatch(1);
        when(groups.warmUp(any(), any())).thenAnswer(invocation -> {
            runs.add(invocation.getArgument(0));
            release.await();
            return 0;
        });
        try {
            warmUp.warmUpInBackground();
            BooleanSupplier first = nextRun(runs);
            boolean cancelledBeforeSecondRun = first.getAsBoolean();
            warmUp.warmUpInBackground();
            release.countDown();
            BooleanSupplier second = nextRun(runs);

            assertThat(cancelledBeforeSecondRun).isFalse();
            assertThat(first.getAsBoolean()).isTrue();
            assertThat(second.getAsBoolean()).isFalse();
        } finally {
            release.countDown();
        }
    }

    /** The cancellation of the next run to start, waiting for it on the common pool. */
    private static BooleanSupplier nextRun(BlockingQueue<BooleanSupplier> runs) throws InterruptedException {
        BooleanSupplier run = runs.poll(10, SECONDS);
        assertThat(run).as("a run started within 10 s").isNotNull();
        return run;
    }

    @Test
    void suspendedCacheSkipsRun() {
        cache.setSuspended(true);

        warmUp.warmUp(() -> false);

        verifyNoInteractions(groups);
    }

    // the other half stays for the objects read by requests
    @Test
    void budgetIsHalfOfCache() {
        ArgumentCaptor<PutBudget> budgets = ArgumentCaptor.forClass(PutBudget.class);

        warmUp.warmUp(() -> false);

        verify(groups).warmUp(any(), budgets.capture());
        PutBudget budget = budgets.getValue();
        budget.charge(ONE_MIB / 2 - 1);
        boolean spentBelowHalf = budget.isSpent();
        budget.charge(1);
        assertThat(spentBelowHalf).isFalse();
        assertThat(budget.isSpent()).isTrue();
    }
}
