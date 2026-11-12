/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Predicate;
import javax.sql.DataSource;
import lombok.NonNull;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The rules followed by a repository using an {@link EntityCache}: when its reads may use the cache, and when the
 * evictions of its writes run. Both depend on the transaction run by the calling thread on the data source of the
 * repository.
 *
 * <p>A read uses the cache unless the cache is suspended, or a transaction runs on the data source. A write evicts its
 * keys once other connections see it: at once in auto-commit mode, and when the transaction completes otherwise.
 */
public final class CacheAccess {

    private final EntityCache cache;
    private final DataSource dataSource;

    public CacheAccess(@NonNull EntityCache cache, @NonNull DataSource dataSource) {
        this.cache = cache;
        this.dataSource = dataSource;
    }

    /** Whether reads go through the cache: it is not suspended, and no transaction runs on the data source. */
    public boolean inUse() {
        return !cache.isSuspended() && !inTransaction();
    }

    /** Evicts the keys made stale by a write, once the write commits. */
    public void evictAfterWrite(Collection<CacheKey> keys) {
        CacheKey[] evicted = keys.toArray(CacheKey[]::new);
        afterWrite(() -> cache.evict(evicted));
    }

    /**
     * Evicts the keys made stale by a write, and the keys of other objects made stale by it, once the write commits.
     */
    public void evictAfterWrite(Collection<CacheKey> keys, Predicate<CacheKey> dependents) {
        CacheKey[] evicted = keys.toArray(CacheKey[]::new);
        afterWrite(() -> {
            cache.evict(evicted);
            cache.evictWhere(dependents);
        });
    }

    /**
     * Whether the calling thread runs a transaction on the data source. A read within it may see changes not committed
     * yet, and those must not reach the cache shared by other threads. A write within it commits later.
     *
     * <p>Spring also binds a connection to the thread for the statements run within a transaction on another data
     * source. That connection stays in auto-commit mode: its statements commit at once, and are not part of a
     * transaction.
     */
    private boolean inTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return false;
        }
        Object resource = TransactionSynchronizationManager.getResource(dataSource);
        return resource instanceof ConnectionHolder holder
                && null != holder.getConnectionHandle()
                && !autoCommits(holder.getConnection());
    }

    // A connection with an unreadable state counts as transactional: reading the database directly is always safe
    private static boolean autoCommits(Connection connection) {
        try {
            return connection.getAutoCommit();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Runs an eviction once the write is visible to other connections: at once in auto-commit mode, and when the
     * transaction completes otherwise. An eviction during the transaction would let a query of another connection read
     * the previous row afterwards, and its put would pass the epoch check.
     */
    private void afterWrite(Runnable eviction) {
        if (inTransaction() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }
}
