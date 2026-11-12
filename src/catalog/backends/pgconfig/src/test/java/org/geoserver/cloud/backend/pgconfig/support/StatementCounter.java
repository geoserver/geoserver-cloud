/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

/**
 * Wraps a {@link DataSource} to count the statements created on its connections, to assert how many queries an
 * operation runs. It also tracks the most connections open at once, to assert how many connections an operation holds.
 */
public class StatementCounter {

    private static final Set<String> STATEMENT_FACTORY_METHODS =
            Set.of("prepareStatement", "prepareCall", "createStatement");

    private final AtomicInteger statements = new AtomicInteger();

    private final AtomicInteger openConnections = new AtomicInteger();

    private final AtomicInteger mostOpenConnections = new AtomicInteger();

    private final DataSource counting;

    public StatementCounter(DataSource target) {
        this.counting = proxyDataSource(target);
    }

    public DataSource dataSource() {
        return counting;
    }

    public int count() {
        return statements.get();
    }

    /** The most connections open at once since the creation of this counter or its last reset. */
    public int mostOpenConnections() {
        return mostOpenConnections.get();
    }

    /** Resets the count of statements, and the most connections open at once to the connections open at the time. */
    public void reset() {
        statements.set(0);
        mostOpenConnections.set(openConnections.get());
    }

    private DataSource proxyDataSource(DataSource target) {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[] {DataSource.class}, (proxy, method, args) -> {
                    Object result = invoke(target, method, args);
                    if (result instanceof Connection connection) {
                        countOpened();
                        return proxyConnection(connection);
                    }
                    return result;
                });
    }

    private Connection proxyConnection(Connection target) {
        AtomicBoolean closed = new AtomicBoolean();
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    if ("close".equals(method.getName()) && closed.compareAndSet(false, true)) {
                        openConnections.decrementAndGet();
                    }
                    if (STATEMENT_FACTORY_METHODS.contains(method.getName())) {
                        statements.incrementAndGet();
                        Statement statement = (Statement) invoke(target, method, args);
                        return proxyStatement(statement, (Connection) proxy, method.getReturnType());
                    }
                    return invoke(target, method, args);
                });
    }

    /**
     * Wraps a statement to answer its connection with the counting one. A stream of {@code JdbcTemplate} releases the
     * connection of its statement when it closes, and the counter must see that release.
     */
    private Statement proxyStatement(Statement target, Connection connection, Class<?> statementType) {
        return (Statement) Proxy.newProxyInstance(
                statementType.getClassLoader(), new Class<?>[] {statementType}, (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        return connection;
                    }
                    return invoke(target, method, args);
                });
    }

    private void countOpened() {
        int open = openConnections.incrementAndGet();
        mostOpenConnections.accumulateAndGet(open, Math::max);
    }

    private Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
