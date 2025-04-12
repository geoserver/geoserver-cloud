/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.mdc.config;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for controlling which Spring Environment information is included in the MDC.
 * <p>
 * These properties determine what application-specific information is added to the MDC (Mapped Diagnostic Context)
 * during request processing. Including this information in the MDC makes it available to all logging
 * statements, providing valuable context for distinguishing logs from different application instances
 * in a distributed environment.
 * <p>
 * The properties are configured using the prefix {@code logging.mdc.include.application} in the application
 * properties or YAML files.
 * <p>
 * Example configuration in YAML:
 * <pre>
 * logging:
 *   mdc:
 *     include:
 *       application:
 *         name: true
 *         version: true
 *         instance-id: true
 *         active-profiles: true
 *         instance-id-properties:
 *           - info.instance-id
 *           - spring.application.instance_id
 *           - pod.name
 * </pre>
 * <p>
 * This class provides methods to extract and add Spring Environment properties to the MDC based on the
 * configuration.
 *
 * @see org.geoserver.cloud.logging.mdc.servlet.SpringEnvironmentMdcFilter
 * @see org.geoserver.cloud.logging.mdc.webflux.MDCWebFilter
 */
@Data
@ConfigurationProperties(prefix = "logging.mdc.include.application")
public class SpringEnvironmentMdcConfigProperties {

    private boolean name = true;
    private boolean version = false;
    private boolean instanceId = false;

    /**
     * Application environment property names where to extract the instance-id from. Defaults to
     * [info.instance-id, spring.application.instance_id]
     */
    private List<String> instanceIdProperties = List.of("info.instance-id", "spring.application.instance_id");

    private boolean activeProfiles = false;

    /**
     * Adds Spring Environment properties to the MDC based on the configuration.
     * <p>
     * This method adds application-specific information from the Spring Environment to the MDC
     * based on the configuration in this class. The information can include:
     * <ul>
     *   <li>Application name</li>
     *   <li>Application version (from BuildProperties)</li>
     *   <li>Instance ID</li>
     *   <li>Active profiles</li>
     * </ul>
     *
     * @param env the Spring Environment from which to extract properties
     * @param buildProperties optional BuildProperties containing version information
     */
    public void addEnvironmentProperties(Environment env, Optional<BuildProperties> buildProperties) {
        if (isName()) {
            MDC.put("application.name", env.getProperty("spring.application.name"));
        }

        putVersion(buildProperties);
        putInstanceId(env);

        if (isActiveProfiles()) {
            MDC.put("spring.profiles.active", Stream.of(env.getActiveProfiles()).collect(Collectors.joining(",")));
        }
    }

    /**
     * Adds the application version to the MDC if enabled by configuration.
     * <p>
     * This method extracts the version from the BuildProperties and adds it to the MDC
     * with the key {@code application.version} if {@link #isVersion()} is true and
     * BuildProperties are available.
     *
     * @param buildProperties optional BuildProperties containing version information
     */
    private void putVersion(Optional<BuildProperties> buildProperties) {
        if (isVersion()) {
            buildProperties.map(BuildProperties::getVersion).ifPresent(v -> MDC.put("application.version", v));
        }
    }

    /**
     * Adds the instance ID to the MDC if enabled by configuration.
     * <p>
     * This method tries to extract an instance ID from the Spring Environment using the
     * property names defined in {@link #getInstanceIdProperties()}. It adds the first
     * non-empty value found to the MDC with the key {@code application.instance.id}
     * if {@link #isInstanceId()} is true.
     *
     * @param env the Spring Environment from which to extract the instance ID
     */
    private void putInstanceId(Environment env) {
        if (!isInstanceId() || null == getInstanceIdProperties()) {
            return;
        }

        for (String prop : getInstanceIdProperties()) {
            String value = env.getProperty(prop);
            if (StringUtils.hasText(value)) {
                MDC.put("application.instance.id", value);
                return;
            }
        }
    }
}
