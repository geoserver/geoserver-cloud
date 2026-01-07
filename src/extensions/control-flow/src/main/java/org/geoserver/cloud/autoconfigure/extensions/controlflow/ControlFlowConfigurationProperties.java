/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import java.util.Properties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 * Configuration properties for the GeoServer Control-Flow extension.
 *
 * <p>This class binds properties under the {@code geoserver.extension.control-flow} prefix and
 * supports dynamic property resolution using Spring Environment placeholders and SpEL expressions.
 * <p>
 * {@link #resolvedProperties()} is used to obtain the final property values.
 *
 * <p>Example configuration in {@code application.yml}:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     control-flow:
 *       enabled: true
 *       use-properties-file: false
 *       properties:
 *         ows.global: "${CONTROL_FLOW_GLOBAL_LIMIT:100}"
 *         user: "#{${MAX_USERS:10} * 2}"
 * }</pre>
 *
 * @see ControlFlowAutoConfiguration
 * @since 2.27.0
 */
@Data
@ConfigurationProperties(prefix = "geoserver.extension.control-flow")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.controlflow")
class ControlFlowConfigurationProperties {

    /** Property name for enabling/disabling the control-flow extension. */
    static final String ENABLED_PROPERTY = "geoserver.extension.control-flow.enabled";

    /** Property name for using the legacy properties file configuration. */
    static final String USE_PROPERTIES_FILE = "geoserver.extension.control-flow.use-properties-file";

    private ExpressionEvaluator evaluator;
    private Properties resolved;

    /**
     * Whether to enable the control-flow extension
     */
    private boolean enabled = true;

    /**
     * Whether to use the default control-flow.properties file in the data directory
     * for configuration
     */
    private boolean usePropertiesFile = false;

    /**
     * key/value pairs of control flow configuration properties. Unused if
     * geoserver.extension.control-flow.use-properties-file is true
     */
    private Properties properties = new Properties();

    /**
     * Initializes the expression evaluator with the Spring Environment.
     *
     * @param environment the Spring Environment for resolving placeholders and SpEL expressions
     */
    @Autowired
    void setEnvironment(Environment environment) {
        this.evaluator = new ExpressionEvaluator(environment);
    }

    /**
     * Returns the control-flow properties with all placeholders and SpEL expressions resolved.
     *
     * <p>Property values can contain:
     *
     * <ul>
     *   <li>Environment placeholders: {@code ${VAR_NAME:default}}
     *   <li>SpEL expressions: {@code #{expression}}
     * </ul>
     *
     * @return a new Properties instance with all values resolved
     */
    public Properties resolvedProperties() {
        if (resolved == null) {
            resolved = new Properties();
            for (String name : properties.stringPropertyNames()) {
                String value = properties.getProperty(name);
                String resolvedValue = resolve(value);
                resolved.setProperty(name, resolvedValue);
            }
        }
        return resolved;
    }

    /**
     * Resolves placeholders and evaluates SpEL expressions in the given value.
     *
     * @param value the property value potentially containing placeholders or expressions
     * @return the resolved value
     */
    private String resolve(final String value) {
        String resolvedValue = evaluator.resolvePlaceholders(value);
        try {
            resolvedValue = evaluator.evaluateExpressions(resolvedValue);
        } catch (Exception e) {
            log.warn(
                    """
                     Error evaluating SpEL expressions in '{}', returning '{}'. \
                     This is ok if you're not trying to use an SpEL expression to perform arithmetic operations. \
                     Error message: {}
                     """,
                    value,
                    resolvedValue,
                    e.getMessage());
        }
        return resolvedValue;
    }
}
