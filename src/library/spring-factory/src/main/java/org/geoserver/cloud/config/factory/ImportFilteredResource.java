package org.geoserver.cloud.config.factory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.annotation.AliasFor;

/**
 * A composed annotation for importing Spring XML bean definitions using a filtered reader.
 * <p>
 * This annotation is meta-annotated with {@link ImportResource} and is intended to be used in place
 * of the standard {@code @ImportResource} when you wish to apply filtering logic to the XML bean
 * definitions. It customizes the default behavior by specifying
 * {@link FilteringXmlBeanDefinitionReader} as the default reader. This reader allows you to append
 * filtering criteria to the resource location, such as including or excluding beans based on a
 * regular expression applied to the bean names.
 * <p>
 * For example, you can specify a location like:
 * <pre class="code">
 *   "jar:gs-main-.*!/applicationContext.xml#name=^(?!foo|bar).*$"
 * </pre>
 * to import only those beans whose names do not match "foo" or "bar". The filtering mechanism is
 * implemented by {@link FilteringXmlBeanDefinitionReader}, which caches parsed XML documents and
 * classpath resources to optimize resource loading.
 * <p>
 * Both the {@code reader} and {@code locations} attributes of this annotation are aliased to the
 * corresponding attributes of {@link ImportResource} using {@link AliasFor}. This enables seamless
 * integration with Spring's XML bean definition processing while adding the filtering capabilities.
 * <p>
 * <strong>Example usage:</strong>
 * <pre class="code">
 * &#64;Configuration
 * &#64;ImportFilteredResource(
 *     // Exclude beans named "foo" or "bar"
 *     locations = "jar:gs-main-.*!/applicationContext.xml#name=^(?!foo|bar).*$"
 * )
 * public class AppConfig {
 *     // Your bean definitions go here
 * }
 * </pre>
 * <p>
 * Refer to {@link FilteringXmlBeanDefinitionReader} for details on how the filtering is applied,
 * including support for regular expression filters appended to the resource location. Note that the
 * filtering logic currently applies to bean names (not aliases) and defers alias registration until
 * after bean filtering.
 *
 * @see ImportResource
 * @see FilteringXmlBeanDefinitionReader
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@ImportResource
public @interface ImportFilteredResource {

    /**
     * Alias for the {@code reader} attribute of {@link ImportResource}.
     * <p>
     * Defaults to {@link FilteringXmlBeanDefinitionReader}, which is a custom XML bean definition reader
     * that applies filtering based on a regular expression. This reader allows you to append filtering
     * criteria to the resource location (e.g., <code>#name=&lt;regex&gt;</code>) so that only beans
     * matching the given pattern are registered.
     *
     * @return the class to use for reading and filtering XML bean definitions
     */
    @AliasFor(annotation = ImportResource.class, attribute = "reader")
    Class<?> reader() default FilteringXmlBeanDefinitionReader.class;

    /**
     * Alias for the {@code locations} attribute of {@link ImportResource}.
     * <p>
     * This attribute serves as a shortcut so you don’t have to specify the attribute name
     * explicitly. It is equivalent to {@link #locations()}.
     *
     * @return an array of resource location strings, optionally appended with filtering criteria
     */
    @AliasFor(annotation = ImportResource.class, attribute = "locations")
    String[] value() default {};

    /**
     * Alias for the {@code locations} attribute of {@link ImportResource}.
     * <p>
     * Specifies one or more resource locations from which to import Spring bean definitions. When using
     * {@link FilteringXmlBeanDefinitionReader}, the resource location may include a filtering fragment,
     * for example, <code>applicationContext.xml#name=^(foo|bar|gml.*OutputFormat).*$</code>, to include or
     * exclude beans based on their names.
     *
     * @return an array of resource location strings, optionally appended with filtering criteria
     */
    @AliasFor(annotation = ImportResource.class, attribute = "locations")
    String[] locations() default {};
}
