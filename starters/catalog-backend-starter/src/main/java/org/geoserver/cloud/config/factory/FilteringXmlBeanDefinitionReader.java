/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.factory;

import static org.springframework.util.StringUtils.hasLength;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

import com.google.common.base.Splitter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;

/**
 * Spring xml bean definition reader that uses a regular expression to include or exclude beans by
 * name and alias.
 *
 * <p>It overloads the {@link ImportResource @ImportResource} {@code locations} attribute allowing
 * to append a {@code #name=<regex>}, for example {@code servlet-context.xml#name=<regex>}
 *
 * <p>Examples:
 *
 * <p>Load all beans from a specific xml file on a specific jar file, except those named {@code foo}
 * or {@code bar}:
 *
 * <pre>
 * <code>
 *  &#64;ImportResource(
 *  reader = FilteringXmlBeanDefinitionReader.class,
 *  // exclude beans named foo and bar:
 *  locations = "jar:gs-main-.*!/applicationContext.xml#name=^(foo|bar)$"
 *  )
 * </code>
 * </pre>
 */
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
                        log.debug("Loaded {} bean definitions from {}", c, uri);
                        if (actualResources != null) {
                            actualResources.add(root);
                        }
                        count += c;
                    } catch (BeanDefinitionStoreException fnf) {
                        if (fnf.getCause() instanceof FileNotFoundException) {
                            log.debug("No {} in {}, skipping.", resourcePattern, uri);
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
            if (!isEmpty(rawFilters)) {
                Splitter.on(",")
                        .omitEmptyStrings()
                        .splitToList(rawFilters)
                        .forEach(expr -> parseAndSetFilter(expr, location));
            }
        }
    }

    private void parseAndSetFilter(final String expression, final String resourceLocation) {
        String actualExpression = expression;
        String[] split = actualExpression.split("=");
        if (split.length != 2) {
            throw throwInvalidExpression(resourceLocation, actualExpression, null);
        }
        String filterType = split[0];
        if (!"name".equals(filterType)) {
            throw throwInvalidExpression(resourceLocation, actualExpression, null);
        }
        try {
            String regex = split[1];
            Predicate<String> filter = Pattern.compile(regex).asMatchPredicate();
            FilteringBeanDefinitionDocumentReader.addFitler(filter);
        } catch (RuntimeException e) {
            throw throwInvalidExpression(resourceLocation, actualExpression, e);
        }
    }

    private IllegalArgumentException throwInvalidExpression(
            final String resourceLocation, String regex, Throwable cause) {
        String msg =
                String.format(
                        "Invalid bean filter expression (%s), expected name=<regex>>, resource: %s",
                        regex, resourceLocation);
        throw new IllegalArgumentException(msg);
    }

    public static class FilteringBeanDefinitionDocumentReader
            extends DefaultBeanDefinitionDocumentReader {

        private static ThreadLocal<List<Predicate<String>>> FILTERS =
                ThreadLocal.withInitial(ArrayList::new);

        public static void addFitler(Predicate<String> beanDefFilter) {
            FILTERS.get().add(beanDefFilter);
        }

        public static void releaseFiltersThreadLocals() {
            FILTERS.remove();
        }

        /**
         * @return {@code true) if any of the filters apply to the bean definition
         */
        private boolean exclude(String name) {
            List<Predicate<String>> filters = FILTERS.get();
            for (Predicate<String> filter : filters) {
                if (filter.test(name)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isFiltering() {
            return !FILTERS.get().isEmpty();
        }

        private Set<String> blackListedBeanNames = new HashSet<String>();

        protected @Override void processBeanDefinition(
                Element ele, BeanDefinitionParserDelegate delegate) {
            if (isFiltering()) {
                String nameAtt = ele.getAttribute(NAME_ATTRIBUTE);
                if (!hasText(nameAtt)) {
                    nameAtt = ele.getAttribute("id"); // old style
                }
                if (blackListedBeanNames.contains(nameAtt)) {
                    // in case the <alias/> element came before the bean definition
                    return;
                }
                String aliasAtt = ele.getAttribute(ALIAS_ATTRIBUTE);
                if (!hasText(aliasAtt)) aliasAtt = nameAtt; // name can be a comma separated list

                if (hasText(nameAtt) && exclude(nameAtt)) {
                    logFilteredBeanMessage(nameAtt);
                    return;
                }
                if (hasLength(aliasAtt)) {
                    String[] aliases =
                            tokenizeToStringArray(
                                    nameAtt,
                                    BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
                    for (String alias : aliases) {
                        if (exclude(alias)) {
                            logFilteredBeanMessage(alias);
                            return;
                        }
                    }
                }
            }
            super.processBeanDefinition(ele, delegate);
        }

        protected @Override void processAliasRegistration(Element ele) {
            if (isFiltering()) {
                String name = ele.getAttribute(NAME_ATTRIBUTE);
                String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
                if (exclude(alias)) {
                    try {
                        getReaderContext().getRegistry().removeBeanDefinition(name);
                    } catch (NoSuchBeanDefinitionException ok) {
                        log.trace(
                                "Blacklisted bean {}, alias {} comes first in the xml file",
                                name,
                                alias);
                        blackListedBeanNames.add(name);
                    }
                    logFilteredBeanMessage(String.format("alias: %s, name: %s", alias, name));
                    return;
                }
            }
            super.processAliasRegistration(ele);
        }

        private void logFilteredBeanMessage(String beanName) {
            String msgFormat = "Excluded by one of the configured filter expressions: {}";
            log.info(msgFormat, beanName);
        }
    }
}
