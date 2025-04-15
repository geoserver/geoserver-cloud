/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.importer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * {@link ConditionalOnProperty @ConditionalOnProperty} that checks if {@code
 * geoserver.extension.importer.enabled} is {@code true}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServer
@ConditionalOnClass(name = "org.geoserver.importer.Importer")
@ConditionalOnProperty(
        prefix = ImporterConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = ImporterConfigProperties.DEFAULT_ENABLED)
public @interface ConditionalOnImporter {}
