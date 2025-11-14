/* (c) 2023  Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.autoconfigure.conditionals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.acl.plugin.config.condition.AclEnabledCondition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot {@link AutoConfiguration @AutoConfiguration} conditional to enable/disable the plugin
 *
 * <p>For plain Spring (without spring boot auto configuration support),
 * {@link AclEnabledCondition @Conditional(AclEnabledCondition.class)} is to be used on plain
 * {@link Configuration @Configuration} classes
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "geoserver.acl", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnAclEnabled {}
