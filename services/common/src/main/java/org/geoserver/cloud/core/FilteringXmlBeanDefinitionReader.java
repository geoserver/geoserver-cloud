/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.core;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

@Slf4j
public class FilteringXmlBeanDefinitionReader extends XmlBeanDefinitionReader {

    public FilteringXmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    public @Override int loadBeanDefinitions(
            String location, @Nullable Set<Resource> actualResources)
            throws BeanDefinitionStoreException {
        final ResourceLoader resourceLoader = getResourceLoader();
        if (!(resourceLoader instanceof ResourcePatternResolver)
                || !(location.startsWith("jar:") || location.startsWith("!jar:"))) {
            return super.loadBeanDefinitions(location, actualResources);
        }

        // Resource pattern matching available.
        try {
            ResourcePatternResolver patternResolver = (ResourcePatternResolver) resourceLoader;
            final boolean excludeJar = location.startsWith("!");
            if (excludeJar) {
                location = location.substring(1);
            }
            // .*/gs-wfs-.*\.jar\!/
            final String jarName = location.substring("jar:".length(), location.indexOf("!"));
            String jarNameExpression = ".*/" + jarName;
            if (!jarName.endsWith(".jar")) {
                jarNameExpression += "\\.jar";
            }
            jarNameExpression += "\\!/";

            Pattern jarNamePattern = Pattern.compile(jarNameExpression);
            // the resource to load, e.g. applicationContext.xml
            String resourcePattern = location.substring(2 + location.indexOf("!/"));
            Resource[] allClasspathBaseResources = patternResolver.getResources("classpath*:");
            int count = 0;
            for (Resource root : allClasspathBaseResources) {
                String uri = root.getURI().toString();
                if (jarNamePattern.matcher(uri).matches()) {
                    String resourceURI = root.getURI().toString() + resourcePattern;
                    log.debug(
                            "Loading bean definitions from {}, matches pattern {}",
                            resourceURI,
                            jarNameExpression);
                    int c = super.loadBeanDefinitions(resourceURI, actualResources);
                    log.info("Loaded {} bean definitions from {}", c, uri);
                    if (actualResources != null) {
                        actualResources.add(root);
                    }
                    count += c;
                }
            }
            return count;
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "Could not resolve bean definition resource pattern [" + location + "]", ex);
        }
    }
}
