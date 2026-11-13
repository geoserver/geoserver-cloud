/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Decides the beans of the cache of the request from {@code geoserver.backend.pgconfig.cache.request.enabled}: the
 * filter, the thread-local transfer, the metrics, and the facades with or without the cache. The value is bound as
 * {@link EntityCacheProperties} binds it: {@code 1}, {@code on} and {@code yes} are true, and a missing value is true.
 * The property conditions of Spring Boot compare the value with the string {@code true}, and would read {@code 1} as
 * false.
 */
abstract class PgconfigRequestCacheCondition extends SpringBootCondition {

    private static final String PREFIX = "geoserver.backend.pgconfig.cache.request";

    private final boolean matchesWhenEnabled;

    PgconfigRequestCacheCondition(boolean matchesWhenEnabled) {
        this.matchesWhenEnabled = matchesWhenEnabled;
    }

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean enabled = isEnabled(context.getEnvironment());
        ConditionMessage message =
                ConditionMessage.forCondition("pgconfig request cache").because(PREFIX + ".enabled is " + enabled);
        return new ConditionOutcome(enabled == matchesWhenEnabled, message);
    }

    /** Whether the cache of the request is on, with the default value of {@link EntityCacheProperties}. */
    private static boolean isEnabled(Environment environment) {
        Binder binder = Binder.get(environment);
        EntityCacheProperties.Request request = binder.bindOrCreate(PREFIX, EntityCacheProperties.Request.class);
        return request.isEnabled();
    }

    /** Matches when the cache of the request is on. */
    static final class Enabled extends PgconfigRequestCacheCondition {
        Enabled() {
            super(true);
        }
    }

    /** Matches when the cache of the request is off. */
    static final class Disabled extends PgconfigRequestCacheCondition {
        Disabled() {
            super(false);
        }
    }
}
