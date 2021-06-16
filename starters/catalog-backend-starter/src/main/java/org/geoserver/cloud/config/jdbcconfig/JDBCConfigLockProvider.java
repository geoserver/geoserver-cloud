/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource.Lock;
import org.springframework.util.Assert;

/** */
@Slf4j
public class JDBCConfigLockProvider implements LockProvider {

    private @NonNull javax.sql.DataSource dataSource;

    private JDBCDistributedLockManager locksManager;

    JDBCConfigLockProvider(@NonNull DataSource pgsqlDataSource) {
        this.dataSource = pgsqlDataSource;
        this.locksManager = new JDBCDistributedLockManager(pgsqlDataSource);
    }

    @Override
    public Lock acquire(@NonNull String path) {
        return locksManager.acquire(path);
    }

    private static @Data class LockKey {
        private final String name;
        private final long key;

        static LockKey valueOf(@NonNull String path) {
            return new LockKey(path, advisoryLockKey(path));
        }

        private static long advisoryLockKey(@NonNull String path) {
            return Hashing.sha256().hashString(path, StandardCharsets.UTF_8).asLong();
        }
    }

    private static @RequiredArgsConstructor class JDBCDistributedLockManager {
        private final DataSource dataSource;
        private final AtomicInteger openSessionCounter = new AtomicInteger();
        private volatile Connection _openLocksSession;

        // private final ConcurrentMap<LockKey, AdvisoryLock> inProcessLocks = new
        // ConcurrentHashMap<>();

        public AdvisoryLock acquire(@NonNull String name) {
            final LockKey key = LockKey.valueOf(name);
            final Connection session = getConnection();
            openSessionCounter.incrementAndGet();
            try (PreparedStatement st = session.prepareStatement("select pg_advisory_lock(?)")) {
                st.setLong(1, key.getKey());
                st.execute(); // this blocks until the lock is acquired
            } catch (SQLException e) {
                openSessionCounter.decrementAndGet();
                throw new IllegalStateException("Error acquiring advisory lock for " + key, e);
            }
            return new AdvisoryLock(key, this);
        }

        public void release(@NonNull LockKey lockKey) {
            boolean unlocked = false;
            final Connection session = getConnection();
            try (PreparedStatement st = session.prepareStatement("select pg_advisory_unlock(?)")) {
                st.setLong(1, lockKey.getKey());
                try (ResultSet rs = st.executeQuery()) {
                    Assert.isTrue(
                            rs.next(),
                            "pg_advisory_lock shall return a single record with a boolean");
                    unlocked = rs.getBoolean(1);
                    if (!unlocked) {
                        String msg =
                                String.format(
                                        "Tried to unlock non-locked key %d for %s",
                                        lockKey.getKey(), lockKey.getName());
                        throw new IllegalStateException(msg);
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Error releasing " + lockKey, e);
            } finally {
                if (unlocked) {
                    final int openLocks = openSessionCounter.decrementAndGet();
                    if (0 == openLocks) {
                        this._openLocksSession = null;
                        try {
                            session.close();
                        } catch (SQLException e) {
                            log.warn("Error closing connection for advisory locks session", e);
                        }
                    }
                }
            }
        }

        private Connection getConnection() {
            if (_openLocksSession == null) {
                synchronized (this) {
                    if (_openLocksSession == null) {
                        try {
                            _openLocksSession = dataSource.getConnection();
                        } catch (SQLException e) {
                            throw new IllegalStateException(
                                    "Unable to obtain connection to act as single advisory locks session");
                        }
                    }
                }
            }
            return _openLocksSession;
        }
    }

    private static @Data class AdvisoryLock implements Lock {
        private final LockKey lockKey;
        private final transient JDBCDistributedLockManager manager;

        @Override
        public void release() {
            manager.release(lockKey);
        }
    }
}
