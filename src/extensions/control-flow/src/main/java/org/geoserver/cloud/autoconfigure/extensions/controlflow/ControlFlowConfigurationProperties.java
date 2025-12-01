/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import java.util.Properties;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

@Data
@ConfigurationProperties(prefix = "geoserver.extension.control-flow")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.controlflow")
class ControlFlowConfigurationProperties {

    static final String ENABLED = "geoserver.extension.control-flow.enabled";
    static final String USE_PROPERTIES_FILE = "geoserver.extension.control-flow.use-properties-file";

    private final transient @NonNull ExpressionEvaluator evaluator;
    private transient Properties resolved;

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

    ControlFlowConfigurationProperties(Environment environment) {
        this.evaluator = new ExpressionEvaluator(environment);
    }

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

    private String resolve(final String value) {
        String resolved = evaluator.resolvePlaceholders(value);
        try {
            resolved = evaluator.evaluateExpressions(resolved);
        } catch (Exception e) {
            log.warn(
                    """
                     Error evaluating SpEL expressions in '{}', returning '{}'. \
                     This is ok if you're not trying to use an SpEL expression to perform arithmetic operations. \
                     Error message: {}
                     """,
                    value,
                    resolved,
                    e.getMessage());
        }
        return resolved;
    }
}
