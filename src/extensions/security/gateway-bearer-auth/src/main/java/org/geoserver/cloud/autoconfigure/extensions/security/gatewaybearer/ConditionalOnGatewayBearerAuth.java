/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.gatewaybearer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** Activates gateway Bearer authentication only when explicitly enabled. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Inherited
@ConditionalOnProperty(
        prefix = GatewayBearerAuthConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = GatewayBearerAuthConfigProperties.DEFAULT)
public @interface ConditionalOnGatewayBearerAuth {}
