/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.processor;

import com.squareup.javapoet.MethodSpec;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.EnhancedBeanInfo;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.util.XmlBeanDefinitionParser;
import org.geoserver.spring.config.transpiler.visitor.AbstractBeanInheritanceVisitor;
import org.geoserver.spring.config.transpiler.visitor.BeanDefinitionVisitor;
import org.geoserver.spring.config.transpiler.visitor.ConstructorBasedBeanVisitor;
import org.geoserver.spring.config.transpiler.visitor.FactoryMethodBeanVisitor;
import org.geoserver.spring.config.transpiler.visitor.ProxyFactoryBeanVisitor;
import org.geoserver.spring.config.transpiler.visitor.SimpleBeanVisitor;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Utility class for testing bean visitors with simplified setup.
 *
 * <p>This utility provides clean methods for testing individual bean method generation
 * without complex test infrastructure setup.
 *
 * @since 2.28.0
 */
public class BeanVisitorTestUtils {

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
        EnhancedBeanInfo enhancedBeanInfo = parsed.getEnhancedBeanInfos().get(beanName);

        // Create TranspilationContext for testing
        TranspilationContext context =
                TranspilationContext.forTesting("org.geoserver.test.generated", "TestConfig", false);
        context.setAllBeanDefinitions(parsed.getBeanDefinitions());
        context.setEnhancedBeanInfos(parsed.getEnhancedBeanInfos());

        // Create BeanGenerationContext with enhanced bean info
        BeanGenerationContext beanContext = BeanGenerationContext.builder()
                .beanName(beanName)
                .beanDefinition(beanDefinition)
                .enhancedBeanInfo(enhancedBeanInfo)
                .transpilationContext(context)
                .build();

        // Find appropriate visitor and generate method
        BeanDefinitionVisitor visitor = findVisitorForBean(beanDefinition, context);
        if (visitor == null) {
            throw new IllegalStateException("No visitor found for bean: " + beanName);
        }

        return visitor.generateBeanMethod(beanContext);
    }

    /**
     * Find the most appropriate visitor for the given bean definition.
     * Uses the same logic as ConfigurationClassVisitor.
     */
    private static BeanDefinitionVisitor findVisitorForBean(
            BeanDefinition beanDefinition, TranspilationContext context) {
        List<BeanDefinitionVisitor> visitors = Arrays.asList(
                new AbstractBeanInheritanceVisitor(), // Priority 50
                new ProxyFactoryBeanVisitor(), // Priority 40
                new ConstructorBasedBeanVisitor(), // Priority 100
                new FactoryMethodBeanVisitor(), // Priority 150
                new SimpleBeanVisitor()); // Priority 200

        return visitors.stream()
                .filter(visitor -> visitor.canHandle(beanDefinition, context))
                .min(Comparator.comparingInt(BeanDefinitionVisitor::getPriority))
                .orElse(null);
    }

    /**
     * Wrap XML content in a proper beans root element if needed
     */
    private static String wrapInBeansRoot(String xmlContent) {
        String trimmed = xmlContent.trim();
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<beans")) {
            return trimmed;
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

    /**
     * Sanitize a bean name to be a valid Java method identifier (same as EnhancedBeanInfo logic)
     */
    private static String sanitizeBeanName(String beanName) {
        if (beanName == null || beanName.isEmpty()) {
            return "unknownBean";
        }

        // Replace non-alphanumeric characters with underscores
        String sanitized = beanName.replaceAll("[^a-zA-Z0-9_]", "_");

        // Ensure it starts with a letter or underscore (Java identifier rules)
        if (!Character.isJavaIdentifierStart(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }

        return sanitized;
    }
}
