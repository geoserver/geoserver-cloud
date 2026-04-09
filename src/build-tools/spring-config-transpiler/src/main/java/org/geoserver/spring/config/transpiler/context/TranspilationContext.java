/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.geoserver.spring.config.transpiler.xml.EnhancedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Context object that holds all the information needed for transpilation.
 *
 * <p>This context object is passed through the generator hierarchy and contains:
 *
 * <ul>
 *   <li>The original annotation and annotated element
 *   <li>Derived configuration like target package/class names
 *   <li>Compiled regex patterns for filtering
 *   <li>The processing environment for file operations
 *   <li>Mutable state including bean definitions and processing tracking
 * </ul>
 *
 * <p>Each {@code @TranspileXmlConfig} annotation gets its own context instance, allowing for isolated processing of
 * different XML configurations.
 *
 * @since 3.0.0
 */
public class TranspilationContext {

    // -- Immutable configuration derived from the @TranspileXmlConfig annotation --

    /** The class that carries the {@code @TranspileXmlConfig} annotation. {@code null} in tests. */
    private final TypeElement annotatedElement;

    /** The annotation instance this context was created from. {@code null} in tests. */
    private final TranspileXmlConfig annotation;

    /**
     * Compile-time processing environment providing {@link javax.annotation.processing.Filer} and
     * {@link javax.annotation.processing.Messager} access. {@code null} in tests.
     */
    private final ProcessingEnvironment processingEnvironment;

    /**
     * Package name for the generated {@code @Configuration} class. Derived from
     * {@link TranspileXmlConfig#targetPackage()} or the annotated class's package.
     */
    private final String targetPackage;

    /**
     * Simple class name for the generated {@code @Configuration} class. Derived from
     * {@link TranspileXmlConfig#targetClass()} or the annotated class name + {@code "_Generated"}.
     */
    private final String targetClassName;

    /**
     * XML resource locations to parse (classpath: or jar: patterns), from {@link TranspileXmlConfig#value()} /
     * {@link TranspileXmlConfig#locations()}.
     */
    private final String[] xmlLocations;

    /**
     * Compiled regex patterns from {@link TranspileXmlConfig#includes()} — a bean is included when any of its names (or
     * aliases) matches at least one pattern. Default: {@code {".*"}}.
     */
    private final Pattern[] includePatterns;

    /**
     * Compiled regex patterns from {@link TranspileXmlConfig#excludes()} — a bean is excluded when any of its names (or
     * aliases) matches at least one pattern. Excludes take precedence over includes.
     */
    private final Pattern[] excludePatterns;

    /**
     * Whether the generated class and its {@code @Bean} methods should be {@code public}. When {@code false} (default),
     * package-private visibility is used to avoid CGLIB proxying.
     */
    private final boolean publicAccess;

    /**
     * Value for {@code @Configuration(proxyBeanMethods = ...)} on the generated class. Default {@code false} — safe
     * because the transpiler never generates inter-bean method calls.
     */
    private final boolean proxyBeanMethods;

    /**
     * Controls how {@code <context:component-scan>} XML elements are handled: generate {@code @ComponentScan}
     * annotations ({@code INCLUDE}), skip them ({@code IGNORE}), or perform build-time classpath scanning and emit
     * {@code @Bean} methods ({@code GENERATE}).
     */
    private final ComponentScanStrategy componentScanStrategy;

    // -- Mutable state populated during transpilation --

    /** Bean names already processed — used to prevent duplicate {@code @Bean} methods. */
    private Set<String> processedBeans;

    /**
     * All bean definitions loaded from the XML resources, keyed by bean name. Populated by
     * {@link org.geoserver.spring.config.transpiler.generator.ConfigurationClassGenerator#loadBeanDefinitions}.
     */
    private Map<String, BeanDefinition> allBeanDefinitions;

    /**
     * Per-bean metadata consolidating the Spring {@link BeanDefinition} with original XML element, aliases, and
     * name-resolution info. Keyed by bean name.
     */
    private Map<String, EnhancedBeanDefinition> enhancedBeanInfoMap;

    /**
     * Component-scan configurations captured from the parsed XML documents. Each entry corresponds to one
     * {@code <context:component-scan>} element and carries its attributes (base-package, use-default-filters,
     * resource-pattern).
     */
    private List<ComponentScanInfo> globalComponentScans;

    private TranspilationContext(Builder builder) {
        this.annotatedElement = builder.annotatedElement;
        this.annotation = builder.annotation;
        this.processingEnvironment = builder.processingEnvironment;

        // Derive configuration from annotation
        this.targetPackage = determineTargetPackage(builder);
        this.targetClassName = determineTargetClassName(builder);
        this.xmlLocations = determineXmlLocations(builder);
        this.includePatterns = compilePatterns(annotation.includes());
        this.excludePatterns = compilePatterns(annotation.excludes());
        this.publicAccess = annotation.publicAccess();
        this.proxyBeanMethods = annotation.proxyBeanMethods();
        this.componentScanStrategy = annotation.componentScanStrategy();

        // Initialize mutable state
        this.allBeanDefinitions = new HashMap<>();
        this.processedBeans = new HashSet<>();
        this.enhancedBeanInfoMap = new HashMap<>();
        this.globalComponentScans = new ArrayList<>();
    }

    /** Test-friendly constructor that doesn't require annotation processor dependencies. */
    private TranspilationContext(
            String targetPackage,
            String targetClassName,
            boolean publicAccess,
            boolean proxyBeanMethods,
            String[] excludes) {
        this.annotatedElement = null;
        this.annotation = null;
        this.processingEnvironment = null;

        // Direct configuration for testing
        this.targetPackage = targetPackage;
        this.targetClassName = targetClassName;
        this.xmlLocations = new String[] {"test.xml"}; // Minimal for testing
        this.includePatterns = new Pattern[] {Pattern.compile(".*")}; // Include all
        this.excludePatterns = compilePatterns(excludes);
        this.publicAccess = publicAccess;
        this.proxyBeanMethods = proxyBeanMethods;
        this.componentScanStrategy = ComponentScanStrategy.INCLUDE;

        // Initialize mutable state
        this.allBeanDefinitions = new HashMap<>();
        this.processedBeans = new HashSet<>();
        this.enhancedBeanInfoMap = new HashMap<>();
        this.globalComponentScans = new ArrayList<>();
    }

    // Getters
    public TypeElement getAnnotatedElement() {
        return annotatedElement;
    }

    public TranspileXmlConfig getAnnotation() {
        return annotation;
    }

    public ProcessingEnvironment getProcessingEnvironment() {
        return processingEnvironment;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public String[] getXmlLocations() {
        return xmlLocations;
    }

    public Pattern[] getIncludePatterns() {
        return includePatterns;
    }

    public Pattern[] getExcludePatterns() {
        return excludePatterns;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public boolean isProxyBeanMethods() {
        return proxyBeanMethods;
    }

    public ComponentScanStrategy getComponentScanStrategy() {
        return componentScanStrategy;
    }

    // Mutable state management methods

    /**
     * Set the complete map of bean definitions for this transpilation context. This is typically called once after
     * loading all XML definitions.
     *
     * @param beanDefinitions the complete map of bean definitions
     */
    public void setAllBeanDefinitions(Map<String, BeanDefinition> beanDefinitions) {
        this.allBeanDefinitions = new HashMap<>(beanDefinitions);
    }

    /**
     * Get the complete map of bean definitions for this transpilation context.
     *
     * @return the map of bean definitions, never null
     */
    public Map<String, BeanDefinition> getAllBeanDefinitions() {
        return allBeanDefinitions;
    }

    /**
     * Set the enhanced bean info map for alias handling.
     *
     * @param enhancedBeanInfoMap the map of bean names to their enhanced information
     */
    public void setEnhancedBeanInfos(Map<String, EnhancedBeanDefinition> enhancedBeanInfoMap) {
        this.enhancedBeanInfoMap = new HashMap<>(enhancedBeanInfoMap);
    }

    /**
     * Get the enhanced bean info map.
     *
     * @return the map of bean names to their enhanced information
     */
    public Map<String, EnhancedBeanDefinition> getEnhancedBeanInfos() {
        return enhancedBeanInfoMap;
    }

    public void setComponentScans(List<ComponentScanInfo> globalComponentScans) {
        this.globalComponentScans = globalComponentScans;
    }

    public List<ComponentScanInfo> getComponentScans() {
        return globalComponentScans;
    }

    /**
     * Mark a bean as processed to avoid duplicate processing.
     *
     * @param beanName the name of the bean that has been processed
     */
    public void markBeanProcessed(String beanName) {
        this.processedBeans.add(beanName);
    }

    /**
     * Check if a bean has already been processed.
     *
     * @param beanName the bean name to check
     * @return true if the bean has been processed
     */
    public boolean isBeanProcessed(String beanName) {
        return processedBeans.contains(beanName);
    }

    /**
     * Get a specific bean definition by name.
     *
     * @param beanName the bean name
     * @return the bean definition, or null if not found
     */
    public BeanDefinition getBeanDefinition(String beanName) {
        return allBeanDefinitions.get(beanName);
    }

    /**
     * Check if a bean name should be included based on include/exclude patterns. This method also considers aliases -
     * if a bean or any of its aliases match an exclude pattern, the bean is excluded. Similarly for include patterns.
     *
     * @param beanName the bean name to check
     * @return true if the bean should be included
     */
    public boolean shouldIncludeBean(String beanName) {
        // Get the EnhancedBeanInfo to access aliases
        Map<String, EnhancedBeanDefinition> allBeanNames = getEnhancedBeanInfos();
        EnhancedBeanDefinition enhancedBeanInfo = allBeanNames.get(beanName);

        if (enhancedBeanInfo == null) {
            // Fallback to simple check if no enhanced bean info available
            return shouldIncludeBeanSimple(beanName);
        }

        // Collect all names to check: original name + aliases
        Set<String> allNamesToCheck = new LinkedHashSet<>();
        allNamesToCheck.add(beanName);
        allNamesToCheck.addAll(enhancedBeanInfo.getAliases());

        // Check exclude patterns first (they take precedence)
        // If ANY name (bean or alias) matches an exclude pattern, exclude the entire
        // bean
        for (String nameToCheck : allNamesToCheck) {
            for (Pattern excludePattern : excludePatterns) {
                if (excludePattern.matcher(nameToCheck).matches()) {
                    return false;
                }
            }
        }

        // Check include patterns
        // If ANY name (bean or alias) matches an include pattern, include the bean
        for (String nameToCheck : allNamesToCheck) {
            for (Pattern includePattern : includePatterns) {
                if (includePattern.matcher(nameToCheck).matches()) {
                    return true;
                }
            }
        }

        // If no include patterns match and we have include patterns, exclude
        return includePatterns.length == 0;
    }

    /**
     * Check if a component-scanned bean should be included based on include/exclude patterns. Checks both the fully
     * qualified class name and the default bean name against the patterns.
     *
     * <p>Matching rules (same precedence as XML bean filtering):
     *
     * <ol>
     *   <li>If any name matches an exclude pattern, the bean is excluded
     *   <li>If any name matches an include pattern, the bean is included
     *   <li>If no include pattern matched but includes are set, the bean is excluded
     *   <li>If no include patterns are configured, the bean is included
     * </ol>
     *
     * @param fqcn the fully qualified class name of the component
     * @param beanName the default bean name (simple class name with first letter lowercased)
     * @return true if the bean should be included
     */
    public boolean shouldIncludeComponentScannedBean(String fqcn, String beanName) {
        Set<String> namesToCheck = new LinkedHashSet<>();
        namesToCheck.add(fqcn);
        namesToCheck.add(beanName);

        // Check exclude patterns first (they take precedence)
        for (String nameToCheck : namesToCheck) {
            for (Pattern excludePattern : excludePatterns) {
                if (excludePattern.matcher(nameToCheck).matches()) {
                    return false;
                }
            }
        }

        // Check include patterns
        for (String nameToCheck : namesToCheck) {
            for (Pattern includePattern : includePatterns) {
                if (includePattern.matcher(nameToCheck).matches()) {
                    return true;
                }
            }
        }

        return includePatterns.length == 0;
    }

    /** Simple bean inclusion check without alias consideration (fallback). */
    private boolean shouldIncludeBeanSimple(String beanName) {
        // Check exclude patterns first (they take precedence)
        for (Pattern excludePattern : excludePatterns) {
            if (excludePattern.matcher(beanName).matches()) {
                return false;
            }
        }

        // Check include patterns
        for (Pattern includePattern : includePatterns) {
            if (includePattern.matcher(beanName).matches()) {
                return true;
            }
        }

        // If no include patterns match and we have include patterns, exclude
        return includePatterns.length == 0;
    }

    // Private helper methods for deriving configuration

    private String determineTargetPackage(Builder builder) {
        String specifiedPackage = builder.annotation.targetPackage().trim();
        if (!specifiedPackage.isEmpty()) {
            return specifiedPackage;
        }

        // Use the same package as the annotated element
        return builder.processingEnvironment
                .getElementUtils()
                .getPackageOf(builder.annotatedElement)
                .getQualifiedName()
                .toString();
    }

    private String determineTargetClassName(Builder builder) {
        String specifiedClassName = builder.annotation.targetClass().trim();
        if (!specifiedClassName.isEmpty()) {
            return specifiedClassName;
        }

        // Use annotated class name with "_Generated" suffix
        return builder.annotatedElement.getSimpleName().toString() + "_Generated";
    }

    private String[] determineXmlLocations(Builder builder) {
        // Use 'value' if specified, otherwise use 'locations'
        String[] locations = builder.annotation.value();
        if (locations.length == 0) {
            locations = builder.annotation.locations();
        }

        if (locations.length == 0) {
            throw new IllegalArgumentException("@TranspileXmlConfig must specify either 'value' or 'locations'");
        }

        return locations;
    }

    private Pattern[] compilePatterns(String[] patternStrings) {
        Pattern[] patterns = new Pattern[patternStrings.length];
        for (int i = 0; i < patternStrings.length; i++) {
            patterns[i] = Pattern.compile(patternStrings[i]);
        }
        return patterns;
    }

    // Builder pattern implementation

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a minimal TranspilationContext for testing purposes. This bypasses the annotation processor dependencies
     * that are not available in unit tests.
     */
    public static TranspilationContext forTesting(
            String targetPackage, String targetClassName, boolean publicAccess, boolean proxyBeanMethods) {
        return new TranspilationContext(targetPackage, targetClassName, publicAccess, proxyBeanMethods, new String[0]);
    }

    /**
     * Create a minimal TranspilationContext for testing with exclude patterns. This bypasses the annotation processor
     * dependencies that are not available in unit tests.
     */
    public static TranspilationContext forTesting(
            String targetPackage,
            String targetClassName,
            boolean publicAccess,
            boolean proxyBeanMethods,
            String[] excludes) {
        return new TranspilationContext(targetPackage, targetClassName, publicAccess, proxyBeanMethods, excludes);
    }

    public static class Builder {
        private TypeElement annotatedElement;
        private TranspileXmlConfig annotation;
        private ProcessingEnvironment processingEnvironment;

        public Builder annotatedElement(TypeElement annotatedElement) {
            this.annotatedElement = annotatedElement;
            return this;
        }

        public Builder annotation(TranspileXmlConfig annotation) {
            this.annotation = annotation;
            return this;
        }

        public Builder processingEnvironment(ProcessingEnvironment processingEnvironment) {
            this.processingEnvironment = processingEnvironment;
            return this;
        }

        public TranspilationContext build() {
            if (annotatedElement == null) {
                throw new IllegalStateException("annotatedElement is required");
            }
            if (annotation == null) {
                throw new IllegalStateException("annotation is required");
            }
            if (processingEnvironment == null) {
                throw new IllegalStateException("processingEnvironment is required");
            }

            return new TranspilationContext(this);
        }
    }

    /**
     * Get a unique suffix for method names to avoid collisions across different configurations. This generates a
     * consistent hash based on the target class name and XML locations.
     *
     * @return a short unique identifier for this transpilation context
     */
    public String getUniqueMethodSuffix() {
        // Create a stable hash based on context information
        String contextKey = targetPackage + "." + targetClassName + ":" + String.join(",", xmlLocations);
        int hash = contextKey.hashCode();

        // Convert to positive hex string and take first 6 characters for brevity
        String hexHash = Integer.toHexString(Math.abs(hash));
        return hexHash.length() > 6 ? hexHash.substring(0, 6) : hexHash;
    }

    public void printMessage(Kind kind, String message) {
        // Skip empty or whitespace-only messages
        if (message == null || message.isBlank()) {
            return;
        }

        if (getProcessingEnvironment() == null) {
            // during tests
            Logger.getLogger(TranspilationContext.class.getName()).info(() -> "%s: %s".formatted(kind, message));
        } else {
            getProcessingEnvironment().getMessager().printMessage(kind, message);
        }
    }
}
