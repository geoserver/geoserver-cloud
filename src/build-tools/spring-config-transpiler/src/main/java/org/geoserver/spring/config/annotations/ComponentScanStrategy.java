/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.annotations;

/**
 * Strategy for handling {@code <context:component-scan>} elements during XML-to-Java transpilation.
 *
 * @since 3.0.0
 * @see TranspileXmlConfig#componentScanStrategy()
 */
public enum ComponentScanStrategy {
    /**
     * Ignore component-scan elements entirely. No {@code @ComponentScan} annotations or {@code @Bean} methods are
     * generated for component-scanned packages. Use this when the component scan packages from the original XML
     * configuration are not applicable in the target context.
     */
    IGNORE,

    /**
     * Transpile component-scan elements to {@code @ComponentScan} annotations on the generated configuration class.
     * This is the default behavior, preserving runtime classpath scanning.
     */
    INCLUDE,

    /**
     * Perform classpath scanning at build time and generate {@code @Bean} methods for each discovered component. This
     * eliminates runtime component scanning for faster application startup. Discovered components are placed in a
     * static inner {@code @Configuration} class named {@code ComponentScannedBeans}.
     *
     * <p>The scanned packages' classes must be available on the annotation processor classpath. The {@code excludes}
     * patterns from {@link TranspileXmlConfig} apply to component-scanned beans, matching against both the fully
     * qualified class name and the default bean name.
     */
    GENERATE
}
