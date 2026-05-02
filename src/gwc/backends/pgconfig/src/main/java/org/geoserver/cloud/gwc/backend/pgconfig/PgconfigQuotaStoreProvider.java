/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import lombok.NonNull;
import org.geoserver.gwc.ConfigurableQuotaStoreProvider;
import org.geoserver.gwc.JDBCConfigurationStorage;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.storage.TilePageCalculator;

/**
 * {@link ConfigurableQuotaStoreProvider} that always returns a pre-built {@link QuotaStore} (a {@code JDBCQuotaStore}
 * wired to the pgconfig DataSource by {@code PgconfigDiskQuotaAutoConfiguration}).
 *
 * <p>Extends {@code ConfigurableQuotaStoreProvider} (rather than the bare {@code QuotaStoreProvider}) because the
 * GeoServer Wicket UI's {@code DiskQuotaWarningPanel} looks up the active provider by exact type via
 * {@code getBeanOfType(ConfigurableQuotaStoreProvider.class)}, and a miss there NPEs the home page.
 *
 * <p>{@link #afterPropertiesSet()} is a no-op (the parent's H2-to-HSQL config rewrite and the factory dispatch through
 * {@code geowebcache-diskquota.xml} aren't needed here - the catalog backend is fixed and the store is constructed
 * eagerly by the auto-configuration). {@link #getQuotaStore()} returns the pre-built store directly. {@link #destroy()}
 * closes it; the underlying DataSource is wrapped with a non-closing delegate so the shared pgconfig pool is preserved.
 *
 * @since 3.0.0
 */
public class PgconfigQuotaStoreProvider extends ConfigurableQuotaStoreProvider {

    private final QuotaStore preBuiltStore;

    public PgconfigQuotaStoreProvider(
            @NonNull ConfigLoader loader,
            @NonNull TilePageCalculator calculator,
            @NonNull JDBCConfigurationStorage jdbcConfigStorage,
            @NonNull QuotaStore store) {
        super(loader, calculator, jdbcConfigStorage);
        this.preBuiltStore = store;
        this.store = store;
    }

    @Override
    public synchronized QuotaStore getQuotaStore() {
        return preBuiltStore;
    }

    @Override
    public void afterPropertiesSet() {
        // intentional no-op: store is pre-built and injected via constructor
    }

    @Override
    public void destroy() throws Exception {
        if (preBuiltStore != null) {
            preBuiltStore.close();
        }
    }
}
