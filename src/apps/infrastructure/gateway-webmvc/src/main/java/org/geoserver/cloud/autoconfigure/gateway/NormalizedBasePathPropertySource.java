/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * Resolves the {@code basepath} property to its canonical form before route definitions consume it.
 *
 * <p>The shipped gateway config splices {@code ${basepath}} textually into route predicates and redirect targets (e.g.
 * {@code Path=${basepath}/wms}, {@code RedirectTo=302, ${basepath}/web/}). A base path of {@code /} or one with a
 * trailing slash silently breaks all routes: {@code //wms} never matches a request path and a {@code Location: //web/}
 * header is scheme-relative, sending browsers to {@code http://web/}. The canonical form has no trailing slash, and the
 * root base path is the empty string.
 *
 * <p>This property source shadows {@code basepath} at the top of the environment: it finds the raw value the other
 * sources define (typically the {@code basepath: ${geoserver.base-path}} alias in the shipped config), resolves its
 * placeholders and normalizes the result. Values with unresolvable placeholders pass through untouched, and every other
 * property name is left to the regular sources.
 *
 * @since 3.1.0
 */
class NormalizedBasePathPropertySource extends PropertySource<Object> {

    static final String NAME = "geoserverNormalizedBasePath";

    static final String BASE_PATH_PROPERTY = "basepath";

    /**
     * Guards against placeholder cycles (e.g. {@code basepath} and {@code geoserver.base-path} aliasing each other): a
     * reentrant lookup falls through to the regular property sources instead of recursing.
     */
    private final ThreadLocal<Boolean> resolving = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Kept out of {@link #getSource()} on purpose: Spring Boot's {@code SpringConfigurationPropertySources} recurses
     * into any property source whose source object is a {@link ConfigurableEnvironment}, and this source lives inside
     * the environment it reads from.
     */
    private final ConfigurableEnvironment environment;

    private NormalizedBasePathPropertySource(ConfigurableEnvironment environment) {
        super(NAME);
        this.environment = environment;
    }

    /** Puts a normalizing source at the top of the environment's property sources; a no-op if already registered. */
    static void register(ConfigurableEnvironment environment) {
        MutablePropertySources sources = environment.getPropertySources();
        if (!sources.contains(NAME)) {
            sources.addFirst(new NormalizedBasePathPropertySource(environment));
        }
    }

    @Override
    public Object getProperty(String name) {
        if (!BASE_PATH_PROPERTY.equals(name) || resolving.get().booleanValue()) {
            return null;
        }
        resolving.set(Boolean.TRUE);
        try {
            return resolveNormalizedBasePath();
        } finally {
            resolving.set(Boolean.FALSE);
        }
    }

    private String resolveNormalizedBasePath() {
        String rawValue = findRawBasePath();
        if (rawValue == null) {
            return null;
        }
        String resolved = environment.resolvePlaceholders(rawValue);
        if (containsUnresolvedPlaceholder(resolved)) {
            return resolved;
        }
        return normalize(resolved);
    }

    /** Finds the {@code basepath} value the other property sources define, placeholders unresolved. */
    private String findRawBasePath() {
        for (PropertySource<?> candidate : environment.getPropertySources()) {
            if (candidate == this) {
                continue;
            }
            Object rawValue = candidate.getProperty(BASE_PATH_PROPERTY);
            if (rawValue != null) {
                return rawValue.toString();
            }
        }
        return null;
    }

    private static boolean containsUnresolvedPlaceholder(String value) {
        return value.contains("${");
    }

    /**
     * Reduces the value to the canonical base path form: no trailing slashes, a leading slash when not empty. The root
     * spellings ({@code ""}, {@code "/"}, whitespace) all reduce to the empty string.
     */
    private static String normalize(String rawValue) {
        String value = rawValue.strip();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.isEmpty() && !value.startsWith("/")) {
            value = "/" + value;
        }
        return value;
    }
}
