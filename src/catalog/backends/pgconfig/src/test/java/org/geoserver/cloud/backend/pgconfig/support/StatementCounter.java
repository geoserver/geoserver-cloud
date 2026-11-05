/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

/**
 * Wraps a {@link DataSource} to count the statements created on its connections, to assert how many queries an
 * operation runs.
 */
public class StatementCounter {

    private static final Set<String> STATEMENT_FACTORY_METHODS =
            Set.of("prepareStatement", "prepareCall", "createStatement");

    private final AtomicInteger statements = new AtomicInteger();

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

    public void reset() {
        statements.set(0);
    }

    private DataSource proxyDataSource(DataSource target) {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[] {DataSource.class}, (proxy, method, args) -> {
                    Object result = invoke(target, method, args);
                    if (result instanceof Connection connection) {
                        return proxyConnection(connection);
                    }
                    return result;
                });
    }

    private Connection proxyConnection(Connection target) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    if (STATEMENT_FACTORY_METHODS.contains(method.getName())) {
                        statements.incrementAndGet();
                    }
                    return invoke(target, method, args);
                });
    }

    private Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
