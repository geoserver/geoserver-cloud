/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.authzn;

import org.geoserver.security.GeoServerAuthenticationKeyProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditionals:
 *
 * <ul>
 *   <li>Authkey extension is in the classpath
 *   <li>{@literal geoserver.security.authkey=true}: Authkey module enabled. Defaults to {@code
 *       false}
 * </ul>
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(GeoServerAuthenticationKeyProvider.class)
@ConditionalOnProperty(
        name = AuthKeyAutoConfiguration.GEOSERVER_SECURITY_AUTHKEY,
        havingValue = "true",
        matchIfMissing = ConditionalOnAuthKeyEnabled.ENABLED_BY_DEFAULT)
public @interface ConditionalOnAuthKeyEnabled {
    boolean ENABLED_BY_DEFAULT = false;
}
