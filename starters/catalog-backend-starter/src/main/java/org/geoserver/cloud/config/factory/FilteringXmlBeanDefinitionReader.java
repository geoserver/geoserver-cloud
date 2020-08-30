/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.factory;

import com.google.common.base.Splitter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

@Slf4j
public class FilteringXmlBeanDefinitionReader extends XmlBeanDefinitionReader {

    public FilteringXmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    public @Override int loadBeanDefinitions(
            String location, @Nullable Set<Resource> actualResources)
            throws BeanDefinitionStoreException {

        super.setDocumentReaderClass(FilteringBeanDefinitionDocumentReader.class);
        parseAndSetBeanFilters(location);
        location = removeBeanFilterExpressions(location);
        try {
            return loadBeanDefinitionsApplyingFilters(location, actualResources);
        } finally {
            FilteringBeanDefinitionDocumentReader.releaseFiltersThreadLocals();
        }
    }

    private int loadBeanDefinitionsApplyingFilters(String location, Set<Resource> actualResources) {
        final ResourceLoader resourceLoader = getResourceLoader();
        final boolean filterByResourceLocation =
                (resourceLoader instanceof ResourcePatternResolver)
                        && (location.startsWith("jar:") || location.startsWith("!jar:"));
        if (!filterByResourceLocation) {
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
                    try {
                        int c = super.loadBeanDefinitions(resourceURI, actualResources);
                        log.info("Loaded {} bean definitions from {}", c, uri);
                        if (actualResources != null) {
                            actualResources.add(root);
                        }
                        count += c;
                    } catch (BeanDefinitionStoreException fnf) {
                        if (fnf.getCause() instanceof FileNotFoundException) {
                            log.info("No {} in {}, skipping.", resourcePattern, uri);
                        } else {
                            throw fnf;
                        }
                    }
                }
            }
            return count;
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "Could not resolve bean definition resource pattern [" + location + "]", ex);
        }
    }

    private String removeBeanFilterExpressions(String location) {
        if (location.contains(".xml#")) {
            location = location.substring(0, location.indexOf(".xml#") + ".xml#".length() - 1);
        }
        return location;
    }

    private void parseAndSetBeanFilters(String location) {
        if (location.contains(".xml#")) {
            String rawFilters = location.substring(".xml#".length() + location.indexOf(".xml#"));
            if (!StringUtils.isEmpty(rawFilters)) {
                Splitter.on(",")
                        .omitEmptyStrings()
                        .splitToList(rawFilters)
                        .forEach(expr -> parseAndSetFilter(expr, location));
            }
        }
    }

    private void parseAndSetFilter(final String expression, final String resourceLocation) {
        String actualExpression = expression;
        final boolean exclude = expression.startsWith("!");
        if (exclude) actualExpression = expression.substring(1);
        String[] split = actualExpression.split("=");
        if (split.length != 2) {
            throw throwInvalidExpression(resourceLocation, actualExpression, null);
        }
        String filterType = split[0];
        Predicate<String> matchPredicate;
        try {
            String regex = split[1];
            matchPredicate = Pattern.compile(regex).asMatchPredicate();
        } catch (RuntimeException e) {
            throw throwInvalidExpression(resourceLocation, actualExpression, e);
        }
        if (exclude) {
            matchPredicate = Predicate.not(matchPredicate);
        }
        Predicate<BeanDefinitionHolder> beanDefFilter;
        if ("name".equals(filterType)) {
            beanDefFilter = createBeanNameFilter(matchPredicate);
        } else if ("type".equals(filterType)) {
            beanDefFilter = createBeanTypeFilter(matchPredicate);
        } else {
            throw throwInvalidExpression(resourceLocation, actualExpression, null);
        }
        FilteringBeanDefinitionDocumentReader.addFitler(beanDefFilter);
    }

    private Predicate<BeanDefinitionHolder> createBeanNameFilter(Predicate<String> nameMatcher) {
        return beanDef -> {
            String name = beanDef.getBeanName();
            if (nameMatcher.test(name)) {
                return false; // register
            }
            for (int i = 0; beanDef.getAliases() != null && i < beanDef.getAliases().length; i++) {
                String alias = beanDef.getAliases()[i];
                if (nameMatcher.test(alias)) {
                    return false; // register
                }
            }
            return true; // do not register bean
        };
    }

    private Predicate<BeanDefinitionHolder> createBeanTypeFilter(
            Predicate<String> typeNamePredicate) {
        return beanDef -> {
            String beanClassName = beanDef.getBeanDefinition().getBeanClassName();
            if (typeNamePredicate.test(beanClassName)) {
                return false; // register, do not filter
            }
            return true; // filter out this bean
        };
    }

    private IllegalArgumentException throwInvalidExpression(
            final String resourceLocation, String regex, Throwable cause) {
        String msg =
                String.format(
                        "Invalid bean filter expression (%s), expected <[!][name|type]=<regex>>, resource: %s",
                        regex, resourceLocation);
        throw new IllegalArgumentException(msg);
    }

    public static class FilteringBeanDefinitionDocumentReader
            extends DefaultBeanDefinitionDocumentReader {

        private static ThreadLocal<List<Predicate<BeanDefinitionHolder>>> FILTERS =
                ThreadLocal.withInitial(ArrayList::new);

        public static void addFitler(Predicate<BeanDefinitionHolder> beanDefFilter) {
            FILTERS.get().add(beanDefFilter);
        }

        public static void releaseFiltersThreadLocals() {
            FILTERS.remove();
        }

        /**
         * @return {@code true) if any of the filters apply to the bean definition
         */
        private boolean exclude(BeanDefinitionHolder bdHolder) {
            for (Predicate<BeanDefinitionHolder> filter : FILTERS.get()) {
                if (filter.test(bdHolder)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isFiltering() {
            List<Predicate<BeanDefinitionHolder>> filters = FILTERS.get();
            return !filters.isEmpty();
        }

        protected @Override void processBeanDefinition(
                Element ele, BeanDefinitionParserDelegate delegate) {
            if (isFiltering()) {
                BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
                if (bdHolder == null) return;
                if (exclude(bdHolder)) {
                    logFilteredBeanMessage(bdHolder);
                } else {
                    proceedWithBeanRegistration(ele, delegate, bdHolder);
                }
            } else {
                super.processBeanDefinition(ele, delegate);
            }
        }

        private void logFilteredBeanMessage(BeanDefinitionHolder bdHolder) {
            String msgFormat = "Excluded by one of the configured filter expressions: {}";
            if (log.isTraceEnabled()) {
                log.trace(msgFormat, bdHolder.getLongDescription());
            } else {
                log.info(msgFormat, bdHolder.getShortDescription());
            }
        }

        /**
         * This method is a verbatim copy of {@link
         * DefaultBeanDefinitionDocumentReader#processBeanDefinition} from {@code
         * spring-beans-5.2.8.RELEASE.jar}, and is reproduced here because calling {@code
         * super.processBeanDefinition(Element, BeanDefinitionParserDelegate)} would result in a
         * "BeanDefinitionParsingException: Configuration problem: Bean name '{bean name here}' is
         * already used in this <beans> element" error, as {@code
         * delegate.parseBeanDefinitionElement(ele)} has already been called to apply the bean
         * filters.
         *
         * <p>Original license holds, copied bellow:
         *
         * <pre>
         * <code>
         * Copyright 2002-2018 the original author or authors.
         *
         * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
         * except in compliance with the License. You may obtain a copy of the License at
         *
         * https://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software distributed under the
         * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
         * either express or implied. See the License for the specific language governing
         * permissions and limitations under the License.
         * </code>
         * </pre>
         */
        private void proceedWithBeanRegistration(
                Element ele, BeanDefinitionParserDelegate delegate, BeanDefinitionHolder bdHolder) {
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
            try {
                // Register the final decorated instance.
                BeanDefinitionReaderUtils.registerBeanDefinition(
                        bdHolder, getReaderContext().getRegistry());
            } catch (BeanDefinitionStoreException ex) {
                getReaderContext()
                        .error(
                                "Failed to register bean definition with name '"
                                        + bdHolder.getBeanName()
                                        + "'",
                                ele,
                                ex);
            }
            // Send registration event.
            getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
        }
    }
}
