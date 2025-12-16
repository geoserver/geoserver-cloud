/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.visitor;

import com.squareup.javapoet.MethodSpec;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Core visitor interface for processing Spring bean definitions and generating Java code.
 *
 * <p>This interface defines the contract for visitors that can transform Spring
 * {@link BeanDefinition} objects into JavaPoet {@link MethodSpec} objects representing
 * {@code @Bean} methods in a {@code @Configuration} class.
 *
 * <p>The visitor pattern allows for different implementations to handle various
 * types of bean definitions (constructor-based, factory-method-based, etc.) while
 * maintaining a consistent interface.
 *
 * <p>Implementations should be stateless and thread-safe to allow for concurrent
 * processing of multiple bean definitions.
 *
 * @since 2.28.0
 * @see BeanGenerationContext
 * @see TranspilationContext
 */
public interface BeanDefinitionVisitor {

    /**
     * Check if this visitor can handle the given bean definition.
     *
     * <p>This method allows for visitor selection based on bean definition
     * characteristics such as:
     * <ul>
     *   <li>Bean class type</li>
     *   <li>Constructor arguments presence</li>
     *   <li>Factory method configuration</li>
     *   <li>Property values</li>
     * </ul>
     *
     * @param beanDefinition the bean definition to check
     * @param context the transpilation context
     * @return true if this visitor can process the bean definition
     */
    boolean canHandle(BeanDefinition beanDefinition, TranspilationContext context);

    /**
     * Generate a {@code @Bean} method for the given bean definition.
     *
     * <p>This method creates a complete {@link MethodSpec} representing a Spring
     * {@code @Bean} method that will instantiate and configure the bean as
     * specified in the original XML configuration.
     *
     * <p>The generated method should:
     * <ul>
     *   <li>Be annotated with {@code @Bean}</li>
     *   <li>Have the correct return type</li>
     *   <li>Include proper parameter injection for dependencies</li>
     *   <li>Handle constructor arguments and property values</li>
     *   <li>Respect visibility modifiers from the transpilation context</li>
     * </ul>
     *
     * @param beanContext the bean-specific generation context containing both
     *                   bean-specific data and a reference to the TranspilationContext
     * @return the generated {@code @Bean} method specification
     * @throws IllegalArgumentException if the bean definition cannot be processed
     */
    MethodSpec generateBeanMethod(BeanGenerationContext beanContext);

    /**
     * Get the priority order for this visitor.
     *
     * <p>When multiple visitors can handle the same bean definition, the one
     * with the highest priority (lowest number) will be selected.
     *
     * <p>Common priority ranges:
     * <ul>
     *   <li>0-99: High priority (specialized visitors)</li>
     *   <li>100-199: Medium priority (common cases)</li>
     *   <li>200+: Low priority (fallback visitors)</li>
     * </ul>
     *
     * @return the visitor priority (lower numbers = higher priority)
     */
    default int getPriority() {
        return 100; // Default medium priority
    }
}
