/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.factory;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Calls {@link FilteringXmlBeanDefinitionReader#clearCaches()} once the application context is
 * refreshed
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class FilteringXmlBeanDefinitionReaderAutoConfiguration {

    @EventListener
    void clearFilteringXmlBeanDefinitionReaderCaches(ContextRefreshedEvent event) {
        FilteringXmlBeanDefinitionReader.clearCaches();
    }
}
