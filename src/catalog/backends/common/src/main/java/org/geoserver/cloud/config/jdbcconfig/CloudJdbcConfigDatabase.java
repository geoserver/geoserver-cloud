/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;
import org.geoserver.util.CacheProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Overrides {@link #save} and {@link #remove} to {@link #clearCache(Info) dispose the internal
 * cache} for both the {@link ModificationProxy} wrapped object (as it may contain identity
 * references to other objects) and the provided {@code info} (which can contain new references to
 * other objects like workspace).
 */
class CloudJdbcConfigDatabase extends ConfigDatabase {

    public CloudJdbcConfigDatabase(
            final DataSource dataSource,
            final XStreamInfoSerialBinding binding,
            final CacheProvider cacheProvider) {
        super(dataSource, binding, cacheProvider);
    }

    /**
     * Override to dispose the internal cache for both the {@link ModificationProxy} wrapped object
     * (as it may contain identity references to other objects) and the provided {@code info} (which
     * can contain new references to other objects like workspace)
     */
    @Override
    @Transactional(
            transactionManager = "jdbcConfigTransactionManager",
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class)
    public <T extends Info> T save(T info) {
        clearCache(ModificationProxy.unwrap(info));
        T saved = super.save(info);
        clearCache(saved);
        return saved;
    }

    /**
     * Override to dispose the internal cache for both the {@link ModificationProxy} wrapped object
     */
    @Override
    @Transactional(
            transactionManager = "jdbcConfigTransactionManager",
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class)
    public void remove(Info info) {
        clearCache(ModificationProxy.unwrap(info));
        super.remove(info);
    }

    /**
     * Overrides to remove the {@link CatalogClearingListener} added by {@code super.setCatalog()},
     * we don't do caching here and the {@link CatalogClearingListener} produces null pointer
     * exceptions
     */
    public @Override void setCatalog(CatalogImpl catalog) {
        super.setCatalog(catalog);
        catalog.removeListeners(CatalogClearingListener.class);
    }
}
