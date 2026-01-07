/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Evaluates property placeholders and SpEL expressions in Control-Flow configuration values.
 *
 * <p>This class provides a two-stage resolution process for configuration values:
 *
 * <ol>
 *   <li><b>Placeholder resolution</b>: Resolves {@code ${property:default}} placeholders using the
 *       Spring {@link Environment}, including system properties like {@code ${cpu.cores}}
 *   <li><b>SpEL evaluation</b>: Evaluates the result as a SpEL expression to support arithmetic
 *       operations like {@code "4 * 2"} -> {@code "8"}
 * </ol>
 *
 * <p>Example resolution flow:
 *
 * <pre>{@code
 * Input:    "${cpu.cores} * 2"
 * Step 1:   "4 * 2"         (placeholder resolved, assuming 4 cores)
 * Step 2:   "8"             (SpEL evaluated)
 * }</pre>
 *
 * <p>This enables dynamic configuration based on container resources:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     control-flow:
 *       properties:
 *         '[ows.global]': "${cpu.cores} * 2"
 *         '[ows.wms.getmap]': "${cpu.cores}"
 * }</pre>
 *
 * @see ControlFlowConfigurationProperties#resolvedProperties()
 * @see ControlFlowAppContextInitializer
 * @since 2.28.1.1
 */
@RequiredArgsConstructor
class ExpressionEvaluator {

    private final @NonNull Environment environment;

    private final ExpressionParser parser = new SpelExpressionParser();

    // Use a standard context, we don't need access to Spring beans within the SpEL itself
    private final StandardEvaluationContext context = new StandardEvaluationContext();

    /**
     * Resolves property placeholders in the given string.
     *
     * <p>Placeholders follow the format {@code ${property:default}} and are resolved against the
     * Spring Environment, which includes system properties, environment variables, and application
     * configuration.
     *
     * @param expressionString the string containing placeholders to resolve
     * @return the string with all placeholders resolved
     */
    public String resolvePlaceholders(String expressionString) {
        return environment.resolvePlaceholders(expressionString);
    }

    /**
     * Evaluates the given string as a SpEL expression.
     *
     * <p>This is typically called after {@link #resolvePlaceholders(String)} to evaluate arithmetic
     * expressions like {@code "4 * 2"}.
     *
     * @param expressionString the SpEL expression to evaluate
     * @return the result of evaluating the expression as a String
     * @throws org.springframework.expression.EvaluationException if the expression cannot be
     *     evaluated
     */
    public String evaluateExpressions(String expressionString) {
        Expression exp = parser.parseExpression(expressionString);
        return exp.getValue(context, String.class);
    }
}
