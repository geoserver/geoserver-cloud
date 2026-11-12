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
 * Matches when the resource store cache is on, from {@code geoserver.backend.pgconfig.cache.resources.enabled}. The
 * value is bound as {@link EntityCacheProperties} binds it, as for {@link PgconfigRequestCacheCondition}: {@code 1},
 * {@code on} and {@code yes} are true, and a missing value is true.
 */
class PgconfigResourceCacheCondition extends SpringBootCondition {

    private static final String PREFIX = "geoserver.backend.pgconfig.cache.resources";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean enabled = isEnabled(context.getEnvironment());
        ConditionMessage message = ConditionMessage.forCondition("pgconfig resource store cache")
                .because(PREFIX + ".enabled is " + enabled);
        return new ConditionOutcome(enabled, message);
    }

    /** Whether the resource store cache is on, with the default value of {@link EntityCacheProperties}. */
    private static boolean isEnabled(Environment environment) {
        Binder binder = Binder.get(environment);
        EntityCacheProperties.Resources resources = binder.bindOrCreate(PREFIX, EntityCacheProperties.Resources.class);
        return resources.isEnabled();
    }
}
