/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.spring.config.transpiler.context.ComponentScanInfo;
import org.geoserver.spring.config.transpiler.context.EnhancedBeanInfo;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Enhanced version of Spring's DefaultBeanDefinitionDocumentReader that captures
 * both the original XML Element and the processed BeanDefinition for each bean.
 *
 * <p>This allows us to:
 * <ul>
 *   <li>Leverage Spring's robust XML parsing logic</li>
 *   <li>Access the complete DOM Document for XML extraction</li>
 *   <li>Preserve original XML structure for Javadoc generation</li>
 *   <li>Get rich BeanDefinition information for code generation</li>
 *   <li>Handle all Spring namespaces and features automatically</li>
 * </ul>
 *
 * @since 2.28.0
 */
class EnhancedBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

    /**
     * Map of bean name -> enhanced bean info containing both XML and BeanDefinition
     */
    private final Map<String, EnhancedBeanInfo> enhancedBeanInfos = new LinkedHashMap<>();

    /**
     * Map of alias -> bean name for resolving aliases
     */
    private final Map<String, String> aliasMap = new LinkedHashMap<>();

    /**
     * List of component-scan configurations found in the XML
     */
    private final List<ComponentScanInfo> componentScans = new ArrayList<>();

    /**
     * Store reference to the complete DOM document for XML extraction
     */
    @SuppressWarnings("unused")
    private Document xmlDocument;

    /**
     * Enhanced version of Spring's registerBeanDefinitions that captures original XML structure
     * before and after Spring's processing.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Stores the complete DOM document for later XML extraction</li>
     *   <li>Pre-captures component-scan and alias elements before Spring processes them</li>
     *   <li>Delegates to Spring's default processing logic</li>
     *   <li>Post-processes to associate captured aliases with their bean definitions</li>
     * </ol>
     *
     * <p>The important insight is that Spring's XmlBeanDefinitionReader already parses XML into
     * a DOM Document, so we have full access to the original structure while still leveraging
     * Spring's robust parsing logic for extracting BeanDefinition objects.
     *
     * @param doc the parsed DOM document containing bean definitions
     * @param readerContext Spring's XML reader context for registration
     */
    @Override
    public void registerBeanDefinitions(
            Document doc, org.springframework.beans.factory.xml.XmlReaderContext readerContext) {
        // Store the complete DOM document - this is the key insight!
        this.xmlDocument = doc;

        // Capture component-scan elements before Spring processes them
        captureComponentScanElements(doc);

        // Capture alias elements before Spring processes them
        captureAliasElements(doc);

        // Let Spring proceed with normal processing
        super.registerBeanDefinitions(doc, readerContext);

        // After Spring processing, associate aliases with bean definitions
        associateAliasesWithBeans();
    }

    @Override
    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        // Let Spring do the heavy lifting of parsing
        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);

        if (bdHolder != null) {
            // Capture both the original XML element and the processed BeanDefinition
            String beanName = bdHolder.getBeanName();
            BeanDefinition beanDefinition = bdHolder.getBeanDefinition();
            String[] aliases = bdHolder.getAliases();

            // Store enhanced information with original DOM element
            EnhancedBeanInfo info = new EnhancedBeanInfo(beanName, beanDefinition, ele, aliases);
            enhancedBeanInfos.put(beanName, info);

            // Continue with Spring's normal processing
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);

            // Register the bean definition with Spring
            BeanDefinitionReaderUtils.registerBeanDefinition(
                    bdHolder, getReaderContext().getRegistry());
        }
    }

    /**
     * Get all enhanced bean information collected during parsing
     */
    public Map<String, EnhancedBeanInfo> getEnhancedBeanInfos() {
        return enhancedBeanInfos;
    }

    /**
     * Get all component-scan configurations found during parsing
     */
    public List<ComponentScanInfo> getComponentScans() {
        return new ArrayList<>(componentScans);
    }

    /**
     * Capture component-scan elements from the DOM document
     */
    private void captureComponentScanElements(Document doc) {
        try {
            // Look for component-scan elements with context namespace
            org.w3c.dom.NodeList componentScanNodes =
                    doc.getElementsByTagNameNS("http://www.springframework.org/schema/context", "component-scan");

            for (int i = 0; i < componentScanNodes.getLength(); i++) {
                org.w3c.dom.Node node = componentScanNodes.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    ComponentScanInfo scanInfo = new ComponentScanInfo(element);
                    componentScans.add(scanInfo);
                }
            }

            // Also check for elements without namespace (in case of default namespace)
            org.w3c.dom.NodeList defaultNodes = doc.getElementsByTagName("context:component-scan");
            for (int i = 0; i < defaultNodes.getLength(); i++) {
                org.w3c.dom.Node node = defaultNodes.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    ComponentScanInfo scanInfo = new ComponentScanInfo(element);
                    componentScans.add(scanInfo);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail processing
            System.err.println("Error capturing component-scan elements: " + e.getMessage());
        }
    }

    /**
     * Capture alias elements from the DOM document
     */
    private void captureAliasElements(Document doc) {
        try {
            // Look for alias elements
            org.w3c.dom.NodeList aliasNodes = doc.getElementsByTagName("alias");

            for (int i = 0; i < aliasNodes.getLength(); i++) {
                org.w3c.dom.Node node = aliasNodes.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    String name = element.getAttribute("name");
                    String alias = element.getAttribute("alias");

                    if (!name.isEmpty() && !alias.isEmpty()) {
                        aliasMap.put(alias, name);
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't fail processing
            System.err.println("Error capturing alias elements: " + e.getMessage());
        }
    }

    /**
     * Associate captured aliases with their corresponding bean definitions
     */
    private void associateAliasesWithBeans() {
        for (Map.Entry<String, String> aliasEntry : aliasMap.entrySet()) {
            String alias = aliasEntry.getKey();
            String beanName = aliasEntry.getValue();

            EnhancedBeanInfo beanInfo = enhancedBeanInfos.get(beanName);
            if (beanInfo != null) {
                // Add the alias to the existing bean info
                beanInfo.addAlias(alias);
            }
        }
    }
}
