/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.secrets;

import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

/**
 * Registers the {@code pgconfig.store.secrets.decrypt} timer: how many store secrets were decrypted, and the time
 * spent. The entity cache keeps the stores decrypted: the count grows with the stores read from the database, not with
 * the requests.
 */
public class StoreSecretsMetrics implements MeterBinder {

    private final StoreSecrets storeSecrets;

    public StoreSecretsMetrics(@NonNull StoreSecrets storeSecrets) {
        this.storeSecrets = storeSecrets;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        FunctionTimer.builder(
                        "pgconfig.store.secrets.decrypt",
                        storeSecrets,
                        StoreSecrets::decryptedValues,
                        StoreSecrets::decryptNanos,
                        TimeUnit.NANOSECONDS)
                .description("Store secrets decrypted when reading store rows")
                .register(registry);
    }
}
