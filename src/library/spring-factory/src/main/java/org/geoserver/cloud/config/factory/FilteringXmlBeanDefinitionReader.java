/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.factory;

import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.factory.FilteringXmlBeanDefinitionReaderAutoConfiguration;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * A custom Spring XML bean definition reader that applies filtering based on a regular expression
 * to selectively include or exclude beans by their name.
 * <p>
 * This reader extends {@link XmlBeanDefinitionReader} to overload the {@code locations} attribute
 * of {@link ImportResource}, allowing you to append an <strong>inclusion</strong> filter in the form
 * {@code #name=<regex>}. For example, if you specify:
 * <pre class="code">
 *   "servlet-context.xml#name=^(?!foo|bar).*$"
 * </pre>
 * then only beans whose names do <em>not</em> match "foo" or "bar" will be registered from that XML.
 * <p>
 * <strong>How it works:</strong>
 * <ul>
 *   <li>
 *     The filter is evaluated against the bean <em>name</em> as defined in the XML, not against its alias.
 *     Beans whose names match the regular expression are registered; all others are discarded.
 *   </li>
 *   <li>
 *     Alias registration is deferred until after bean registration. If a bean is not registered
 *     (because its name did not match the filter), any aliases for that bean are also discarded.
 *   </li>
 * </ul>
 * <p>
 * <strong>Examples:</strong>
 * <p>
 * <em>Example 1:</em> Load all beans from a specific XML file on a specific JAR file, except those
 * named {@code foo} or {@code bar}:
 * <pre class="code">
 * &#64;ImportResource(
 *     reader = FilteringXmlBeanDefinitionReader.class,
 *     // Exclude beans named "foo" or "bar":
 *     locations = "jar:gs-main-.*!/applicationContext.xml#name=^(?!foo|bar).*$"
 * )
 * </pre>
 * <p>
 * <em>Example 2:</em> Load only beans named {@code foo}, {@code bar}, or those matching
 * {@code gml.*OutputFormat}:
 * <pre class="code">
 * &#64;ImportResource(
 *     reader = FilteringXmlBeanDefinitionReader.class,
 *     // Include only specific beans:
 *     locations = "jar:gs-main-.*!/applicationContext.xml#name=^(foo|bar|gml.*OutputFormat).*$"
 * )
 * </pre>
 * <p>
 * In addition to filtering functionality, this reader maintains internal caches to optimize resource
 * loading:
 * <ul>
 *   <li>
 *     <strong>XML Document Cache:</strong> Parsed XML documents are cached by their resource URI so that
 *     multiple configurations loading beans from the same XML file do not trigger redundant parsing.
 *   </li>
 *   <li>
 *     <strong>Classpath Resource Cache:</strong> A cache of classpath resources is maintained to avoid the
 *     overhead of reloading all resources for each location.
 *   </li>
 * </ul>
 * <p>
 * The caches can be cleared by invoking {@link #clearCaches()}, which is typically called after the
 * application context is refreshed.
 *
 * @see ImportResource
 * @see XmlBeanDefinitionReader
 * @see FilteringXmlBeanDefinitionReaderAutoConfiguration
 * @see FilteringXmlBeanDefinitionReader#clearCaches()
 * @see FilteringXmlBeanDefinitionReader#reader()
 * @see FilteringXmlBeanDefinitionReader#locations()
 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.config.factory")
public class FilteringXmlBeanDefinitionReader extends XmlBeanDefinitionReader {

    private static final String XML_SPLIT_TOKEN = ".xml#";

    /**
     * Cache parsed XML documents by Resource URI, since many configurations can try to load
     * different sets of beans from the same xml document
     *
     * @see FilteringXmlBeanDefinitionReaderAutoConfiguration
     * @see #clearCaches()
     */
    private static Map<String, Document> classpathDocuments = new HashMap<>();

    /**
     * To be used by {@link #getAllClasspathResources}, caches all classpath resources to avoid
     * loading them all for each location
     *
     * @see FilteringXmlBeanDefinitionReaderAutoConfiguration
     * @see #clearCaches()
     */
    private static Resource[] classpathBaseResources;

    public FilteringXmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    /**
     * Clears any cached resource, expected to be called after the application context is refreshed
     */
    public static synchronized void clearCaches() {
        if (!classpathDocuments.isEmpty() || null != classpathBaseResources) {
            log.debug(
                    "Clearing cache of {} parsed xml documents and {} classpath resources",
                    classpathDocuments.size(),
                    null == classpathBaseResources ? 0 : classpathBaseResources.length);
            classpathDocuments.clear();
            classpathBaseResources = null;
        }
    }

    protected @Override Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
        return classpathDocuments.computeIfAbsent(resource.getURI().toString(), r -> {
            try {
                log.trace("Loading document {}", r);
                return super.doLoadDocument(inputSource, resource);
            } catch (Exception e) {
                if (e instanceof RuntimeException rte) {
                    throw rte;
                }
                throw (RuntimeException) new BeanDefinitionStoreException(e.getMessage()).initCause(e);
            }
        });
    }

    @Override
    public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources)
            throws BeanDefinitionStoreException {

        super.setDocumentReaderClass(FilteringBeanDefinitionDocumentReader.class);
        parseAndSetBeanInclusionFilters(location);
        location = removeBeanFilterExpressions(location);
        try {
            return loadBeanDefinitionsApplyingFilters(location, actualResources);
        } finally {
            FilteringBeanDefinitionDocumentReader.releaseFiltersThreadLocals();
        }
    }

    private int loadBeanDefinitionsApplyingFilters(String location, Set<Resource> actualResources) {
        final ResourceLoader resourceLoader = getResourceLoader();
        final boolean filterByResourceLocation = (resourceLoader instanceof ResourcePatternResolver)
                && (location.startsWith("jar:") || location.startsWith("!jar:"));
        if (!filterByResourceLocation) {
            return super.loadBeanDefinitions(location, actualResources);
        }

        // Resource pattern matching available.
        try {
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
            final String resourcePattern = location.substring(2 + location.indexOf("!/"));
            final Resource[] allClasspathBaseResources =
                    getAllClasspathResources((ResourcePatternResolver) resourceLoader);
            log.trace("looking for {}", location);

            int count = 0;
            for (Resource root : allClasspathBaseResources) {
                count += loadBeanDefinitions(actualResources, jarNameExpression, jarNamePattern, resourcePattern, root);
            }
            return count;
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "Could not resolve bean definition resource pattern [%s]".formatted(location), ex);
        }
    }

    private int loadBeanDefinitions(
            Set<Resource> actualResources,
            String jarNameExpression,
            Pattern jarNamePattern,
            final String resourcePattern,
            Resource root)
            throws IOException {
        String uri = root.getURI().toString();
        int count = 0;
        if (jarNamePattern.matcher(uri).matches()) {
            String resourceURI = root.getURI().toString() + resourcePattern;
            log.debug("Loading bean definitions from {}, matches pattern {}", resourceURI, jarNameExpression);
            try {
                count = super.loadBeanDefinitions(resourceURI, actualResources);
                log.debug("Loaded {} bean definitions from {}", count, uri);
                if (actualResources != null) {
                    actualResources.add(root);
                }
            } catch (BeanDefinitionStoreException fnf) {
                if (fnf.getCause() instanceof FileNotFoundException) {
                    log.debug("No {} in {}, skipping.", resourcePattern, uri);
                } else {
                    throw fnf;
                }
            }
        }
        return count;
    }

    /**
     * @param resourceLoader
     * @return
     * @throws IOException
     */
    private static synchronized Resource[] getAllClasspathResources(ResourcePatternResolver patternResolver)
            throws IOException {
        if (null == classpathBaseResources) {
            StopWatch sw = new StopWatch();
            sw.start();
            classpathBaseResources = patternResolver.getResources("classpath*:");
            sw.stop();
            if (log.isTraceEnabled()) {
                log.trace("Loaded %,d classpath resources in %,dms"
                        .formatted(classpathBaseResources.length, sw.getTotalTimeMillis()));
            }
        }
        return classpathBaseResources;
    }

    private String removeBeanFilterExpressions(String location) {
        if (location.contains(XML_SPLIT_TOKEN)) {
            location = location.substring(0, location.indexOf(XML_SPLIT_TOKEN) + XML_SPLIT_TOKEN.length() - 1);
        }
        return location;
    }

    private void parseAndSetBeanInclusionFilters(String location) {
        if (location.contains(XML_SPLIT_TOKEN)) {
            String filterTypeAndRegularExpression =
                    location.substring(XML_SPLIT_TOKEN.length() + location.indexOf(XML_SPLIT_TOKEN));
            if (hasText(filterTypeAndRegularExpression)) {
                String[] split = filterTypeAndRegularExpression.split("=");
                if (split.length != 2) {
                    throw throwInvalidExpression(location, filterTypeAndRegularExpression, null);
                }
                String filterType = split[0];
                if (!"name".equals(filterType)) {
                    throw throwInvalidExpression(location, filterTypeAndRegularExpression, null);
                }
                String regex = split[1];
                addBeanNameIncludeFilter(regex, location);
            }
        }
    }

    private void addBeanNameIncludeFilter(final String regex, final String resourceLocation) {
        try {
            Predicate<String> matcher = Pattern.compile(regex).asMatchPredicate();
            FilteringBeanDefinitionDocumentReader.addMatcher(matcher);
        } catch (RuntimeException e) {
            throw throwInvalidExpression(resourceLocation, regex, e);
        }
    }

    private IllegalArgumentException throwInvalidExpression(
            final String resourceLocation, String regex, Throwable cause) {
        String msg = "Invalid bean filter expression (%s), expected name=<regex>>, resource: %s"
                .formatted(regex, resourceLocation);
        throw new IllegalArgumentException(msg, cause);
    }

    public static class FilteringBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

        private static final ThreadLocal<List<Predicate<String>>> MATCHERS = ThreadLocal.withInitial(ArrayList::new);

        public static void addMatcher(Predicate<String> beanDefFilter) {
            MATCHERS.get().add(beanDefFilter);
        }

        public static void releaseFiltersThreadLocals() {
            MATCHERS.remove();
        }

        /**
         * @return {@code true) if any of the filters apply to the bean definition, meaning the bean
         *         shall be registered; {@code false} if the bean shall be discarded
         */
        private boolean include(String name) {
            List<Predicate<String>> matchers = MATCHERS.get();
            for (Predicate<String> matcher : matchers) {
                if (matcher.test(name)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isFiltering() {
            return !MATCHERS.get().isEmpty();
        }

        private Set<String> blackListedBeanNames = new HashSet<>();
        private Map<String, String> deferredNameToAlias = new HashMap<>();

        protected @Override void doRegisterBeanDefinitions(Element root) {
            super.doRegisterBeanDefinitions(root);
            blackListedBeanNames.forEach(deferredNameToAlias::remove);
            for (Entry<String, String> e : deferredNameToAlias.entrySet()) {
                String name = e.getKey();
                String alias = e.getValue();
                registerDeferredAlias(name, alias);
            }
        }

        private void registerDeferredAlias(String name, String alias) {
            String msgFormat = "Registering   '{}' alias for '{}'";
            log.trace(msgFormat, alias, name);
            try {
                getReaderContext().getRegistry().registerAlias(name, alias);
            } catch (Exception ex) {
                getReaderContext()
                        .error(
                                "Failed to register alias '%s' for bean with name '%s'".formatted(alias, name),
                                null,
                                ex);
            }
            getReaderContext().fireAliasRegistered(name, alias, null);
        }

        protected @Override void processAliasRegistration(Element ele) {
            if (!isFiltering()) {
                super.processAliasRegistration(ele);
                return;
            }
            final String name = ele.getAttribute(NAME_ATTRIBUTE);
            final String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
            if (!StringUtils.hasText(name)) {
                getReaderContext().error("Name must not be empty", ele);
                return;
            }
            if (!StringUtils.hasText(alias)) {
                getReaderContext().error("Alias must not be empty", ele);
                return;
            }
            deferredNameToAlias.put(name, alias);
        }

        protected @Override void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
            final String beanNameOrId = getBeanNameOrId(ele);
            if (isFiltering()) {
                if (shallInclude(beanNameOrId)) {
                    logIncludingBeanMessage(beanNameOrId);
                    super.processBeanDefinition(ele, delegate);
                } else if (hasText(beanNameOrId)) {
                    blackListedBeanNames.add(beanNameOrId);
                    logExcludedBeanMessage(beanNameOrId);
                }
            } else {
                logUnfiltered(beanNameOrId);
                super.processBeanDefinition(ele, delegate);
            }
        }

        private boolean shallInclude(String nameAtt) {
            if (!hasText(nameAtt) || include(nameAtt)) {
                return true;
            }

            String[] aliases =
                    tokenizeToStringArray(nameAtt, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
            for (String alias : aliases) {
                if (include(alias)) {
                    return true;
                }
            }
            return false;
        }

        private String getBeanNameOrId(Element ele) {
            String nameAtt = ele.getAttribute(NAME_ATTRIBUTE);
            if (!hasText(nameAtt)) {
                nameAtt = ele.getAttribute("id"); // old style
            }
            return nameAtt;
        }

        private void logUnfiltered(String beanName) {
            if (hasText(beanName)) {
                String msgFormat = "Include bean '{}', no regular expression provided";
                log.trace(msgFormat, beanName);
            }
        }

        private void logIncludingBeanMessage(String beanName) {
            if (hasText(beanName)) {
                String msgFormat = "Include bean '{}', matches regular expression";
                log.trace(msgFormat, beanName);
            }
        }

        private void logExcludedBeanMessage(String beanName) {
            if (hasText(beanName)) {
                String msgFormat = "Exclude bean '{}', no regular expression matches";
                log.trace(msgFormat, beanName);
            }
        }
    }
}
