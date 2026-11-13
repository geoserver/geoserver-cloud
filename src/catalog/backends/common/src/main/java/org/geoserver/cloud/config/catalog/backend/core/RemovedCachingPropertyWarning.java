/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Warns at startup that {@code geoserver.catalog.caching.enabled} has no effect. The property switched on a catalog
 * cache that has been removed. The pgconfig backend caches catalog and configuration objects on its own, and
 * {@code geoserver.backend.pgconfig.cache.enabled} controls these caches.
 *
 * <p>The warning appears for any value of the property, {@code false} included: a deployment that turned the removed
 * cache off gets the pgconfig caches by default.
 */
@Conditional(RemovedCachingPropertyWarning.PropertyIsSet.class)
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.core")
public class RemovedCachingPropertyWarning implements InitializingBean {

    static final String REMOVED_PROPERTY = "geoserver.catalog.caching.enabled";

    static final String REPLACEMENT_PROPERTY = "geoserver.backend.pgconfig.cache.enabled";

    @Override
    public void afterPropertiesSet() {
        log.warn(
                "{} has no effect and is ignored. The pgconfig backend caches catalog and configuration objects,"
                        + " controlled by {}",
                REMOVED_PROPERTY,
                REPLACEMENT_PROPERTY);
    }

    static class PropertyIsSet implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return context.getEnvironment().containsProperty(REMOVED_PROPERTY);
        }
    }
}
