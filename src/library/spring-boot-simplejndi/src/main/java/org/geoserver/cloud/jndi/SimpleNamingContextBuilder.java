/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jndi;

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;

import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A simple implementation of a JNDI naming context builder that provides an
 * initial context factory for applications requiring JNDI support.
 *
 * <p>
 * This builder allows the configuration of an {@link InitialContextFactory}
 * dynamically based on environment properties. If no factory is explicitly
 * provided in the environment, a default {@link SimpleNamingContextFactory} is
 * used.
 *
 * <p>
 * Example usage:
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
 * NamingManager.setInitialContextFactoryBuilder(builder);
 * ...
 * Context initialContext = NamingManager.getInitialContext(new HashTable<>());
 * DataSource ds = new DriverManagerDataSource(...);
 * initialContext.bind("java:comp/env/jdbc/myds", ds);
 * </pre>
 *
 * @see SimpleNamingContext
 */
public class SimpleNamingContextBuilder implements InitialContextFactoryBuilder {

    /**
     * Default factory instance used when no custom factory is provided in the
     * environment.
     */
    private final SimpleNamingContextFactory factory = new SimpleNamingContextFactory();

    /**
     * Creates an {@link InitialContextFactory} instance based on the provided
     * environment properties.
     *
     * <p>
     * If the environment specifies an {@link InitialContextFactory} class name or
     * instance, through the {@code java.naming.factory.initial} parameter, this
     * method attempts to instantiate and return it. Otherwise, it falls back to the
     * default {@link SimpleNamingContextFactory}.
     *
     * @param environment a {@link Hashtable} containing JNDI environment
     *                    properties, or {@code null}
     * @return an instance of {@link InitialContextFactory}
     * @throws IllegalArgumentException if the specified factory class is invalid or
     *                                  does not implement
     *                                  {@link InitialContextFactory}
     * @throws IllegalStateException    if the specified factory class cannot be
     *                                  instantiated
     */
    @Override
    public InitialContextFactory createInitialContextFactory(@Nullable Hashtable<?, ?> environment) {
        // Return the default factory if no specific factory is provided in the
        // environment's java.naming.factory.initial key.
        return createFromEnvironment(environment).orElseGet(() -> this.factory);
    }

    private Optional<InitialContextFactory> createFromEnvironment(Map<?, ?> environment) {
        InitialContextFactory fac = null;

        Object factoryParam = (environment == null ? Map.of() : environment).get(INITIAL_CONTEXT_FACTORY);
        if (factoryParam != null) {
            Class<?> icfClass =
                    switch (factoryParam) {
                        case Class<?> c -> c;
                        case String className -> loadClass(className);
                        default -> throw invalidType(factoryParam);
                    };

            if (!InitialContextFactory.class.isAssignableFrom(icfClass)) {
                throw new IllegalArgumentException("Specified class does not implement [%s]: %s"
                        .formatted(InitialContextFactory.class.getName(), factoryParam));
            }
            fac = newInstance(icfClass);
        }
        return Optional.ofNullable(fac);
    }

    private InitialContextFactory newInstance(Class<?> icfClass) {
        try {
            return (InitialContextFactory)
                    ReflectionUtils.accessibleConstructor(icfClass).newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to instantiate specified InitialContextFactory: " + icfClass, ex);
        }
    }

    private IllegalArgumentException invalidType(Object factoryParam) {
        return new IllegalArgumentException("Invalid value type for environment key [%s]: %s"
                .formatted(INITIAL_CONTEXT_FACTORY, factoryParam.getClass().getName()));
    }

    private Class<?> loadClass(String className) {
        return ClassUtils.resolveClassName(className, getClass().getClassLoader());
    }
}
