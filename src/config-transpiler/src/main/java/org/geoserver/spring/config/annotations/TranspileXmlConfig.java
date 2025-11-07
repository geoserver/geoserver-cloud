/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks classes for build-time XML import processing.
 *
 * <p>When applied to a {@code @Configuration} class, this annotation triggers a compile-time annotation processor that:
 * <ul>
 *   <li>Parses the specified XML configuration files</li>
 *   <li>Applies include/exclude filters to bean definitions</li>
 *   <li>Generates {@code @Configuration} classes with {@code @Bean} methods</li>
 *   <li>Eliminates runtime XML parsing overhead for improved startup performance</li>
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * @Configuration
 * @BuildTimeXmlImport(
 *     locations = "classpath:applicationContext.xml",
 *     targetPackage = "org.example.generated",
 *     targetClass = "MyGeneratedConfig",
 *     excludes = {
 *         "rawCatalog",      // provided by GeoServerBackendConfigurer
 *         "secureCatalog",   // provided by GeoServerBackendConfigurer
 *         "catalog",         // overridden by cloud-specific implementation
 *         "proxyfier"        // unused - Spring Boot handles forwarded headers
 *     })
 * @Import(org.example.generated.MyGeneratedConfig.class)
 * public class MyConfiguration {
 * }
 * }</pre>
 *
 * <p>The annotation processor will generate a {@code @Configuration} class
 * at the specified target package and class name containing {@code @Bean} methods
 * for all filtered bean definitions from the XML files.
 *
 * <p><strong>Generated class naming:</strong>
 * <ul>
 *   <li>If {@code targetClass} is specified, uses that name</li>
 *   <li>If {@code targetPackage} is specified, places class in that package</li>
 *   <li>Otherwise, uses the annotated class's package and appends "_Generated"</li>
 * </ul>
 *
 * <p><strong>Filtering behavior:</strong>
 * <ul>
 *   <li>If {@code includes} is specified, only beans matching the patterns are processed</li>
 *   <li>If {@code excludes} is specified, matching beans are filtered out</li>
 *   <li>Both include and exclude patterns support regular expressions</li>
 *   <li>Exclude patterns take precedence over include patterns</li>
 * </ul>
 *
 * <p><strong>Advantages over runtime XML processing:</strong>
 * <ul>
 *   <li>Faster application startup (no runtime XML parsing)</li>
 *   <li>Compile-time validation of bean definitions</li>
 *   <li>Better IDE support and refactoring capabilities</li>
 *   <li>Reduced memory footprint (no XML document caching)</li>
 *   <li>Predictable generated class names for {@code @Import} usage</li>
 * </ul>
 *
 * @since 2.28.0
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Import
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // Only needed at compile time
@Documented
@Repeatable(TranspileXmlConfig.List.class)
public @interface TranspileXmlConfig {

    /**
     * Alias for {@link #locations()}.
     * @return the XML resource locations to process
     */
    String[] value() default {};

    /**
     * XML configuration file locations to process. Supports Spring resource patterns including:
     * <ul>
     *   <li>classpath: resources (e.g., "classpath:applicationContext.xml")</li>
     *   <li>jar: patterns (e.g., "jar:gs-main-.*!/applicationContext.xml")</li>
     *   <li>file: system paths (e.g., "file:/path/to/config.xml")</li>
     * </ul>
     *
     * @return the XML resource locations to process
     */
    String[] locations() default {};

    /**
     * Target package name for the generated {@code @Configuration} class.
     * If not specified, uses the same package as the annotated class.
     *
     * <p>Example: {@code targetPackage = "org.example.generated"}
     *
     * @return the target package for the generated class
     */
    String targetPackage() default "";

    /**
     * Target class name for the generated {@code @Configuration} class.
     * If not specified, uses the annotated class name with "_Generated" suffix.
     *
     * <p>Example: {@code targetClass = "MyGeneratedConfig"}
     *
     * @return the target class name for the generated class
     */
    String targetClass() default "";

    /**
     * Bean name patterns to include. If specified, only beans whose names match
     * these patterns will be processed. Patterns support regular expressions.
     *
     * <p>Example: {@code includes = {"catalog.*", "geoServer.*"}}
     *
     * @return bean name include patterns
     */
    String[] includes() default {".*"}; // Include all by default

    /**
     * Bean name patterns to exclude. Beans whose names match these patterns
     * will be filtered out. Patterns support regular expressions.
     * Exclude patterns take precedence over include patterns.
     *
     * <p>Example: {@code excludes = {".*Test.*", ".*Mock.*", "proxyfier"}}
     *
     * @return bean name exclude patterns
     */
    String[] excludes() default {};

    /**
     * Whether to generate public classes and methods. By default, follows Spring conventions:
     * <ul>
     *   <li>{@code false} (default): Package-private {@code @Configuration} class and {@code @Bean} methods for better performance</li>
     *   <li>{@code true}: Public {@code @Configuration} class and {@code @Bean} methods for cross-package access</li>
     * </ul>
     *
     * <p><strong>Performance implications:</strong>
     * <ul>
     *   <li>Package-private: Spring uses raw classes without CGLIB proxies (faster)</li>
     *   <li>Public: Spring may use CGLIB proxies for {@code @Configuration} classes (slower)</li>
     * </ul>
     *
     * <p>Set to {@code true} when the generated class is in a different package than the importing code.
     *
     * @return {@code true} for public access, {@code false} for package-private (default)
     */
    boolean publicAccess() default false;

    /**
     * Whether to enable bean method proxying in the generated {@code @Configuration} class.
     * Controls the {@code proxyBeanMethods} attribute of the {@code @Configuration} annotation.
     * <ul>
     *   <li>{@code false} (default): Disables CGLIB proxying for better performance and startup time</li>
     *   <li>{@code true}: Enables CGLIB proxying for inter-bean dependencies and singleton enforcement</li>
     * </ul>
     *
     * <p><strong>Performance implications:</strong>
     * <ul>
     *   <li>{@code false}: Faster startup, reduced memory usage, no CGLIB proxies</li>
     *   <li>{@code true}: Slower startup, higher memory usage, but enforces Spring singleton semantics</li>
     * </ul>
     *
     * <p><strong>Safety for generated classes:</strong>
     * Setting this to {@code false} is generally safe for generated classes since the transpiler
     * does not generate inter-bean method calls within {@code @Bean} methods. All dependencies
     * are handled through method parameters and Spring's dependency injection system.
     *
     * <p>This defaults to {@code false} (opposite of Spring's default) for better performance in GeoServer Cloud.
     *
     * @return {@code false} for no proxying (default), {@code true} for CGLIB proxying
     */
    boolean proxyBeanMethods() default false;

    /**
     * Container annotation for multiple {@code @BuildTimeXmlImport} declarations.
     * This allows using multiple {@code @BuildTimeXmlImport} annotations on the same class.
     *
     * <p>Example:
     * <pre>{@code
     * @BuildTimeXmlImport(locations = "classpath:beans1.xml", excludes = {"bean1"})
     * @BuildTimeXmlImport(locations = "classpath:beans2.xml", includes = {"bean.*"})
     * @Import({GeneratedClass1.class, GeneratedClass2.class})
     * public class MyConfiguration {
     * }
     * }</pre>
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface List {
        TranspileXmlConfig[] value();
    }
}
