/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.processor;

import com.palantir.javapoet.MethodSpec;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.generator.AbstractBeanInheritanceMethodGenerator;
import org.geoserver.spring.config.transpiler.generator.BeanMethodGenerator;
import org.geoserver.spring.config.transpiler.generator.ConstructorBasedBeanMethodGenerator;
import org.geoserver.spring.config.transpiler.generator.FactoryMethodBeanMethodGenerator;
import org.geoserver.spring.config.transpiler.generator.ProxyFactoryBeanMethodGenerator;
import org.geoserver.spring.config.transpiler.generator.SimpleBeanMethodGenerator;
import org.geoserver.spring.config.transpiler.xml.EnhancedBeanDefinition;
import org.geoserver.spring.config.transpiler.xml.XmlBeanDefinitionParser;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Utility class for testing bean method generators with simplified setup.
 *
 * <p>This utility provides clean methods for testing individual bean method generation without complex test
 * infrastructure setup.
 *
 * @since 3.0.0
 */
public class BeanMethodGeneratorTestUtils {

    /**
     * Generate a bean method from XML content using the same parsing pipeline as the real transpiler.
     *
     * @param beanName the name of the bean to generate method for
     * @param xmlContent the XML content containing the bean definition and any aliases
     * @return the generated method specification
     */
    public static MethodSpec generateBeanMethodFromXml(String beanName, String xmlContent) {
        // Wrap XML content properly
        String tempXmlContent = wrapInBeansRoot(xmlContent);

        // Parse XML using the same mechanism as the real transpiler
        XmlBeanDefinitionParser.ParsedXmlResult parsed = XmlBeanDefinitionParser.parseXmlContent(tempXmlContent);

        BeanDefinition beanDefinition = parsed.getBeanDefinitions().get(beanName);
        if (beanDefinition == null) {
            throw new IllegalArgumentException("Bean '" + beanName + "' not found in XML content");
        }

        // Get the enhanced bean info directly from the parser (this will contain the original DOM element)
        EnhancedBeanDefinition enhancedBeanInfo = parsed.getEnhancedBeanInfos().get(beanName);

        // Create TranspilationContext for testing
        TranspilationContext context =
                TranspilationContext.forTesting("org.geoserver.test.generated", "TestConfig", false, false);
        context.setAllBeanDefinitions(parsed.getBeanDefinitions());
        context.setEnhancedBeanInfos(parsed.getEnhancedBeanInfos());

        // Create BeanGenerationContext with enhanced bean info
        BeanGenerationContext beanContext = BeanGenerationContext.builder()
                .beanDefinition(beanDefinition)
                .enhancedBeanInfo(enhancedBeanInfo)
                .transpilationContext(context)
                .build();

        // Find appropriate generator and generate method
        BeanMethodGenerator generator = findGeneratorForBean(beanDefinition);
        if (generator == null) {
            throw new IllegalStateException("No generator found for bean: " + beanName);
        }

        return generator.generateBeanMethod(beanContext);
    }

    /**
     * Find the most appropriate generator for the given bean definition. Uses the same logic as
     * ConfigurationClassGenerator.
     */
    private static BeanMethodGenerator findGeneratorForBean(BeanDefinition beanDefinition) {
        List<BeanMethodGenerator> generators = Arrays.asList(
                new AbstractBeanInheritanceMethodGenerator(), // Priority 50
                new ProxyFactoryBeanMethodGenerator(), // Priority 40
                new ConstructorBasedBeanMethodGenerator(), // Priority 50
                new FactoryMethodBeanMethodGenerator(), // Priority 60
                new SimpleBeanMethodGenerator()); // Priority 200

        return generators.stream()
                .filter(generator -> generator.canHandle(beanDefinition))
                .min(Comparator.comparingInt(BeanMethodGenerator::getPriority))
                .orElse(null);
    }

    /** Wrap XML content in a proper beans root element if needed */
    static String wrapInBeansRoot(String xmlContent) {
        String trimmed = xmlContent.trim();
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<beans")) {
            return trimmed;
        }

        boolean hasContextNamespace = trimmed.contains("context:");

        if (hasContextNamespace) {
            return """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:context="http://www.springframework.org/schema/context"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans
                       http://www.springframework.org/schema/beans/spring-beans.xsd
                       http://www.springframework.org/schema/context
                       http://www.springframework.org/schema/context/spring-context.xsd">
                """
                    + trimmed + """
                </beans>
                """;
        }

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns="http://www.springframework.org/schema/beans"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.springframework.org/schema/beans
                   http://www.springframework.org/schema/beans/spring-beans.xsd">
            """
                + trimmed + """
            </beans>
            """;
    }
}
