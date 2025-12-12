/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.spring.config.transpiler.context.ComponentScanInfo;
import org.geoserver.spring.config.transpiler.context.EnhancedBeanInfo;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.io.InputStreamResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class for parsing XML bean definitions with proper alias resolution.
 *
 * <p>
 * This utility extracts the XML parsing logic from ConfigurationClassVisitor to
 * make it reusable for both transpiler and testing scenarios.
 *
 * @since 2.28.0
 */
public class XmlBeanDefinitionParser {

    /**
     * Result of parsing XML content containing bean definitions, enhanced bean
     * info, and component scans.
     */
    public static class ParsedXmlResult {
        private final Map<String, BeanDefinition> beanDefinitions;
        private final Map<String, EnhancedBeanInfo> enhancedBeanInfos;
        private final List<ComponentScanInfo> componentScans;

        public ParsedXmlResult(
                Map<String, BeanDefinition> beanDefinitions,
                Map<String, EnhancedBeanInfo> enhancedBeanInfos,
                List<ComponentScanInfo> componentScans) {
            this.beanDefinitions = beanDefinitions;
            this.enhancedBeanInfos = enhancedBeanInfos;
            this.componentScans = componentScans;
        }

        public Map<String, BeanDefinition> getBeanDefinitions() {
            return beanDefinitions;
        }

        public Map<String, EnhancedBeanInfo> getEnhancedBeanInfos() {
            return enhancedBeanInfos;
        }

        public List<ComponentScanInfo> getComponentScans() {
            return componentScans;
        }
    }

    /**
     * Parse XML content string into bean definitions with proper alias resolution.
     *
     * @param xmlContent the XML content as string
     * @return parsed result containing bean definitions and name resolution info
     */
    public static ParsedXmlResult parseXmlContent(String xmlContent) {
        try {
            // Add XML prolog if missing and wrap in beans element if needed
            String fullXml = xmlContent.trim();

            // Check if XML prolog is missing
            boolean needsXmlProlog = !fullXml.startsWith("<?xml");

            // Check if content needs to be wrapped in <beans> element
            // Look for <beans> tag anywhere in the content (could be after comments/DOCTYPE)
            boolean needsBeansWrapper = !fullXml.contains("<beans");

            if (needsXmlProlog && needsBeansWrapper) {
                // Missing both prolog and beans wrapper - add both
                fullXml =
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <beans xmlns="http://www.springframework.org/schema/beans"
                               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                               xsi:schemaLocation="http://www.springframework.org/schema/beans
                               http://www.springframework.org/schema/beans/spring-beans.xsd">
                        """
                                + fullXml + """
                        </beans>
                        """;
            } else if (needsXmlProlog) {
                // Missing only the XML prolog - add it without wrapping in beans
                fullXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + fullXml;
            }

            // Use the enhanced XML reader that already handles everything
            EnhancedXmlBeanDefinitionReader reader = new EnhancedXmlBeanDefinitionReader();
            reader.loadBeanDefinitions(new InputStreamResource(new ByteArrayInputStream(fullXml.getBytes("UTF-8"))));

            // Extract bean definitions from the factory - only primary bean names, not aliases
            // Aliases will be handled through BeanNameInfo and included in @Bean(name={...}) arrays
            Map<String, BeanDefinition> beanDefinitions = reader.getBeanDefinitions();

            // Get enhanced bean infos and component scans from the enhanced reader
            // The enhanced bean infos now contain all the name resolution information
            Map<String, EnhancedBeanInfo> enhancedBeanInfos = reader.getEnhancedBeanInfos();
            List<ComponentScanInfo> componentScans = reader.getComponentScans();

            return new ParsedXmlResult(beanDefinitions, enhancedBeanInfos, componentScans);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML content: " + e.getMessage(), e);
        }
    }

    /**
     * Parse component-scan elements from XML content.
     *
     * @param xmlContent the XML content
     * @return list of component scan configurations
     */
    private static List<ComponentScanInfo> parseComponentScanElements(String xmlContent) throws Exception {
        List<ComponentScanInfo> componentScans = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

        // Look for component-scan elements with context namespace
        org.w3c.dom.NodeList componentScanNodes =
                doc.getElementsByTagNameNS("http://www.springframework.org/schema/context", "component-scan");

        for (int i = 0; i < componentScanNodes.getLength(); i++) {
            org.w3c.dom.Node node = componentScanNodes.item(i);
            if (node instanceof Element element) {
                ComponentScanInfo scanInfo = new ComponentScanInfo(element);
                componentScans.add(scanInfo);
            }
        }

        // Also check for elements without namespace (in case of default namespace)
        org.w3c.dom.NodeList defaultNodes = doc.getElementsByTagName("context:component-scan");
        for (int i = 0; i < defaultNodes.getLength(); i++) {
            org.w3c.dom.Node node = defaultNodes.item(i);
            if (node instanceof Element element) {
                ComponentScanInfo scanInfo = new ComponentScanInfo(element);
                componentScans.add(scanInfo);
            }
        }

        return componentScans;
    }
}
