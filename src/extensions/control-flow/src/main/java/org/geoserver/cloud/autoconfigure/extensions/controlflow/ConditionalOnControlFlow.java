/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite conditional annotation for the GeoServer Control-Flow extension.
 *
 * <p>Beans annotated with this condition are only created when both conditions are met:
 *
 * <ul>
 *   <li>The {@code org.geoserver.flow.ControlFlowConfigurator} class is present on the classpath
 *       (i.e., the {@code gs-control-flow} dependency is available)
 *   <li>The property {@code geoserver.extension.control-flow.enabled} is {@code true} (which is
 *       the default if not specified)
 * </ul>
 *
 * <p>The extension can be disabled by setting:
 *
 * <pre>{@code
 * geoserver.extension.control-flow.enabled=false
 * }</pre>
 *
 * <p>Or using the shorthand environment variable:
 *
 * <pre>{@code
 * CONTROL_FLOW=false
 * }</pre>
 *
 * @see ControlFlowAutoConfiguration
 * @see ControlFlowConfigurationProperties#ENABLED_PROPERTY
 * @since 2.28.1.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnClass(name = "org.geoserver.flow.ControlFlowConfigurator")
@ConditionalOnProperty(
        name = ControlFlowConfigurationProperties.ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = true)
@interface ConditionalOnControlFlow {}
