/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class PutBudgetTest {

    @Test
    void spentAtLimit() {
        PutBudget budget = new PutBudget(1_000);

        budget.charge(600);
        assertThat(budget.isSpent()).isFalse();

        budget.charge(400);
        assertThat(budget.isSpent()).isTrue();
    }

    @Test
    void boundToOneThread() {
        PutBudget budget = new PutBudget(1_000);

        Optional<PutBudget> here;
        try (PutBudget.Binding binding = budget.bind()) {
            here = PutBudget.current();
            Optional<PutBudget> elsewhere =
                    CompletableFuture.supplyAsync(PutBudget::current).join();
            assertThat(elsewhere).isEmpty();
        }

        assertThat(here).containsSame(budget);
        assertThat(PutBudget.current()).isEmpty();
    }

    @Test
    void nestedBindsUnwindInOrder() {
        PutBudget outer = new PutBudget(1_000);
        PutBudget inner = PutBudget.unlimited();

        try (PutBudget.Binding outerBinding = outer.bind()) {
            try (PutBudget.Binding innerBinding = inner.bind()) {
                assertThat(PutBudget.current()).containsSame(inner);
            }
            assertThat(PutBudget.current()).containsSame(outer);
        }

        assertThat(PutBudget.current()).isEmpty();
    }

    @Test
    void closeTwiceDoesNothing() {
        PutBudget outer = new PutBudget(1_000);
        PutBudget.Binding outerBinding = outer.bind();
        PutBudget.Binding innerBinding = PutBudget.unlimited().bind();
        innerBinding.close();

        innerBinding.close();

        assertThat(PutBudget.current()).containsSame(outer);
        outerBinding.close();
        assertThat(PutBudget.current()).isEmpty();
    }

    @Test
    void closedAfterException() {
        PutBudget budget = new PutBudget(1_000);

        assertThatIllegalStateException().isThrownBy(() -> {
            try (PutBudget.Binding binding = budget.bind()) {
                throw new IllegalStateException("failed");
            }
        });

        assertThat(PutBudget.current()).isEmpty();
    }

    @Test
    void unlimitedNeverSpent() {
        PutBudget budget = PutBudget.unlimited();

        budget.charge(Integer.MAX_VALUE);

        assertThat(budget.isSpent()).isFalse();
    }
}
