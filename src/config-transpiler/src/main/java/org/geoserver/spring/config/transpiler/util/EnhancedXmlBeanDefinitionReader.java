/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.spring.config.transpiler.context.ComponentScanInfo;
import org.geoserver.spring.config.transpiler.context.EnhancedBeanInfo;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * Enhanced XmlBeanDefinitionReader that provides access to our custom document
 * reader so we can extract the enhanced bean information after parsing.
 */
class EnhancedXmlBeanDefinitionReader extends XmlBeanDefinitionReader {

    private EnhancedBeanDefinitionDocumentReader enhancedDocumentReader;

    public EnhancedXmlBeanDefinitionReader() {
        super(new DefaultListableBeanFactory());
        // Create and set our enhanced document reader
        this.enhancedDocumentReader = new EnhancedBeanDefinitionDocumentReader();
        setDocumentReaderClass(EnhancedBeanDefinitionDocumentReader.class);
        // Configure for lenient parsing - disable namespace validation since we handle
        // context:component-scan separately and don't need Spring's namespace handlers
        setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        setNamespaceAware(false);
    }

    /**
     * Get access to the enhanced document reader that captured the XML and
     * BeanDefinition info
     */
    EnhancedBeanDefinitionDocumentReader getEnhancedDocumentReader() {
        return enhancedDocumentReader;
    }

    @Override
    protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
        // Return our instance so we can get the data back
        return enhancedDocumentReader;
    }

    /**
     * Extract bean definitions from the factory - only primary bean names, not
     * aliases. Aliases will be handled through BeanNameInfo and included
     * in {@code  @Bean(name={...})} arrays
     */
    public Map<String, BeanDefinition> getBeanDefinitions() {
        Map<String, BeanDefinition> beanDefinitions = new LinkedHashMap<>();
        for (String beanName : getBeanDefinitionNames()) {
            beanDefinitions.put(beanName, getBeanDefinition(beanName));
        }
        return beanDefinitions;
    }

    public Map<String, EnhancedBeanInfo> getEnhancedBeanInfos() {
        return getEnhancedDocumentReader().getEnhancedBeanInfos();
    }

    public List<ComponentScanInfo> getComponentScans() {
        return getEnhancedDocumentReader().getComponentScans();
    }

    public String[] getBeanDefinitionNames() {
        return getRegistry().getBeanDefinitionNames();
    }

    public BeanDefinition getBeanDefinition(String beanName) {
        return getRegistry().getBeanDefinition(beanName);
    }
}
