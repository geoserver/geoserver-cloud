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

@RequiredArgsConstructor
class ExpressionEvaluator {

    private final @NonNull Environment environment;

    private final ExpressionParser parser = new SpelExpressionParser();

    // Use a standard context, we don't need access to Spring beans within the SpEL
    // itself
    private final StandardEvaluationContext context = new StandardEvaluationContext();

    public String resolvePlaceholders(String expressionString) {
        return environment.resolvePlaceholders(expressionString);
    }

    public String evaluateExpressions(String expressionString) {
        Expression exp = parser.parseExpression(expressionString);
        return exp.getValue(context, String.class);
    }
}
