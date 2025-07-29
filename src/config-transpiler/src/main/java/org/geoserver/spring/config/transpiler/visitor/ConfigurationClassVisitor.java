/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.visitor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.ComponentScanInfo;
import org.geoserver.spring.config.transpiler.context.EnhancedBeanInfo;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.util.XmlBeanDefinitionParser;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Visitor that generates the main {@code @Configuration} class containing all {@code @Bean} methods.
 *
 * <p>This visitor is responsible for:
 * <ul>
 *   <li>Creating the main class structure with {@code @Configuration} annotation</li>
 *   <li>Loading and parsing XML resources to extract bean definitions</li>
 *   <li>Delegating to specialized visitors for individual {@code @Bean} methods</li>
 *   <li>Applying filtering rules to include/exclude beans</li>
 *   <li>Assembling the complete class specification</li>
 * </ul>
 *
 * @since 2.28.0
 */
public class ConfigurationClassVisitor {

    private final List<BeanDefinitionVisitor> beanVisitors;

    /**
     * Result of parsing a single resource, containing bean definitions and enhanced bean info.
     */
    private static class ResourceParsingResult {
        final Map<String, BeanDefinition> beanDefinitions;
        final List<ComponentScanInfo> componentScans;
        final Map<String, EnhancedBeanInfo> enhancedBeanInfos;

        ResourceParsingResult(
                Map<String, BeanDefinition> beanDefinitions,
                List<ComponentScanInfo> componentScans,
                Map<String, EnhancedBeanInfo> enhancedBeanInfos) {
            this.beanDefinitions = beanDefinitions;
            this.componentScans = componentScans;
            this.enhancedBeanInfos = enhancedBeanInfos;
        }
    }

    /**
     * Create a new configuration class visitor with default bean visitors.
     */
    public ConfigurationClassVisitor() {
        this.beanVisitors = createDefaultBeanVisitors();
    }

    /**
     * Generate the complete {@code @Configuration} class for the given context.
     *
     * @param context the transpilation context
     * @return the generated {@code @Configuration} class specification
     */
    public TypeSpec generateConfigurationClass(TranspilationContext context) {

        // Determine class visibility
        final Modifier[] classModifiers =
                context.isPublicAccess() ? new Modifier[] {Modifier.PUBLIC} : new Modifier[0]; // Package-private

        // Create the class builder
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(context.getTargetClassName())
                .addModifiers(classModifiers)
                .addAnnotation(Configuration.class);

        // Load bean definitions from XML and generate {@code @Bean} methods
        try {
            loadBeanDefinitions(context);

            // Track excluded beans for javadoc
            List<String> excludedBeans = new ArrayList<>();

            // Deduplicate bean definitions - if the same BeanDefinition object appears under multiple names,
            // only process it once using the primary name (the first occurrence)
            Map<BeanDefinition, String> processedBeanDefinitions = new LinkedHashMap<>();

            for (Map.Entry<String, BeanDefinition> entry :
                    context.getAllBeanDefinitions().entrySet()) {
                String beanName = entry.getKey();
                BeanDefinition beanDefinition = entry.getValue();

                // If we've already processed this exact BeanDefinition object, skip it
                if (processedBeanDefinitions.containsKey(beanDefinition)) {
                    String primaryName = processedBeanDefinitions.get(beanDefinition);
                    context.printMessage(
                            javax.tools.Diagnostic.Kind.NOTE,
                            "Skipping duplicate bean definition: " + beanName + " (already processed as " + primaryName
                                    + ")");
                    continue;
                }

                // Mark this BeanDefinition as processed
                processedBeanDefinitions.put(beanDefinition, beanName);

                // Create bean generation context with transpilation context reference
                BeanGenerationContext beanContext = createBeanGenerationContext(beanName, beanDefinition, context);

                // Skip if bean should be excluded
                if (!context.shouldIncludeBean(beanContext.getBeanName())) {
                    excludedBeans.add(beanName);
                    continue;
                }

                // Find appropriate visitor and generate {@code @Bean} method
                BeanDefinitionVisitor visitor = findVisitorForBean(beanDefinition, context);
                if (visitor != null) {
                    // Use the simplified API - all visitors now use the same method signature
                    MethodSpec beanMethod = visitor.generateBeanMethod(beanContext);
                    classBuilder.addMethod(beanMethod);
                }
            }

            // Generate @ComponentScan annotations for component-scan elements
            for (ComponentScanInfo componentScan : context.getComponentScans()) {
                generateComponentScanAnnotation(componentScan, classBuilder);
            }

            addClassJavadocs(context, excludedBeans, classBuilder);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate configuration class: " + e.getMessage(), e);
        }

        return classBuilder.build();
    }

    private void addClassJavadocs(
            TranspilationContext context, List<String> excludedBeans, TypeSpec.Builder classBuilder) {

        classBuilder
                .addJavadoc("Generated Spring Configuration class from XML definitions.\n")
                .addJavadoc("\n")
                .addJavadoc("Source XML files:\n")
                .addJavadoc("<ul>\n");

        // Add source XML files to javadoc
        for (String location : context.getXmlLocations()) {
            classBuilder.addJavadoc(" <li>$L</li>\n", location);
        }
        classBuilder.addJavadoc("</ul>\n");

        // Add excluded beans to javadoc if any were filtered out
        if (!excludedBeans.isEmpty()) {
            classBuilder.addJavadoc("\n").addJavadoc("Excluded beans:\n").addJavadoc("<ul>\n");
            Collections.sort(excludedBeans);
            for (String beanName : excludedBeans) {

                List<String> aliases =
                        context.getEnhancedBeanInfos().get(beanName).getAliases();

                classBuilder.addJavadoc(" <li>$L</li>\n", beanName);
                for (String alias : aliases) {
                    classBuilder.addJavadoc(" <li>$L (alias for $L)</li>\n", alias, beanName);
                }
            }
            classBuilder.addJavadoc("</ul>\n");
        }
    }

    private void loadBeanDefinitions(TranspilationContext context) throws Exception {
        Map<String, BeanDefinition> allBeanDefinitions = new LinkedHashMap<>();

        // Global component scan information collected during parsing
        List<ComponentScanInfo> globalComponentScans = new ArrayList<>();

        // Global enhanced bean information with original XML and name resolution collected during parsing
        Map<String, EnhancedBeanInfo> globalEnhancedBeanInfos = new LinkedHashMap<>();

        // Process each XML location
        for (String location : context.getXmlLocations()) {
            try {
                List<Resource> resources = resolveResources(location, context);

                // Parse all resolved resources and combine results
                for (Resource resource : resources) {
                    ResourceParsingResult parsingResult = parseResourceWithNameInfo(resource, context);
                    allBeanDefinitions.putAll(parsingResult.beanDefinitions);
                    globalComponentScans.addAll(parsingResult.componentScans);
                    globalEnhancedBeanInfos.putAll(parsingResult.enhancedBeanInfos);

                    // Log successful parsing
                    context.printMessage(
                            Diagnostic.Kind.NOTE,
                            "Loaded " + parsingResult.beanDefinitions.size() + " bean definitions from: "
                                    + resource.getDescription());
                }
            } catch (Exception e) {
                // Handle resource not found gracefully
                if (e instanceof IOException
                        && (e.getMessage().contains("Resource not found")
                                || e.getMessage().contains("No resources found matching"))) {
                    context.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Resource not found for location '" + location + "': " + e.getMessage());
                    throw e; // Fail compilation
                } else {
                    throw e; // Re-throw other exceptions
                }
            }
        }

        // Set the bean definitions and enhanced bean info on the context for visitor access
        context.setAllBeanDefinitions(allBeanDefinitions);
        context.setEnhancedBeanInfos(globalEnhancedBeanInfos);
        context.setComponentScans(globalComponentScans);
    }

    /**
     * Resolve resources from a location string (supporting classpath: and jar: patterns).
     */
    private List<Resource> resolveResources(String location, TranspilationContext context) throws Exception {
        if (location.startsWith("jar:")) {
            return resolveJarPatternResources(location, context);
        } else if (location.startsWith("classpath:")) {
            return resolveClasspathResources(location, context);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported location type: " + location + ". Supported: classpath: and jar: resources.");
        }
    }

    /**
     * Resolve JAR pattern resources (e.g., "jar:gs-main-.*!/applicationContext.xml").
     * Handles robust resource loading with fallback strategies for annotation processor environments.
     */
    private List<Resource> resolveJarPatternResources(String location, TranspilationContext context) throws Exception {
        context.printMessage(Diagnostic.Kind.NOTE, "Resolving JAR pattern: " + location);

        // Extract JAR name pattern and resource path from location
        // Format: jar:gs-main-.*!/applicationContext.xml
        String jarNamePattern = location.substring("jar:".length(), location.indexOf("!"));
        String resourcePath = location.substring(location.indexOf("!/") + 2);

        context.printMessage(
                Diagnostic.Kind.NOTE, "JAR pattern: " + jarNamePattern + ", Resource path: " + resourcePath);

        List<Resource> matchingResources = new ArrayList<>();

        // Use annotation processor's classloader directly
        ClassLoader processorClassLoader = this.getClass().getClassLoader();
        context.printMessage(Diagnostic.Kind.NOTE, "Annotation processor classloader: " + processorClassLoader);

        // Try direct resource access first - this should work for annotationProcessorPaths JARs
        java.net.URL resourceUrl = processorClassLoader.getResource(resourcePath);
        if (resourceUrl != null) {
            context.printMessage(Diagnostic.Kind.NOTE, "Found resource directly: " + resourceUrl);

            // Check if this URL matches our JAR pattern
            String urlString = resourceUrl.toString();
            context.printMessage(Diagnostic.Kind.NOTE, "Resource URL: " + urlString);

            // Convert pattern to regex for matching
            String jarNameExpression = jarNamePattern.replaceAll("\\*", ".*");

            // Extract JAR filename from URL for proper matching
            String jarFileName = urlString.replaceAll(".*/([^/]+\\.jar)!.*", "$1");
            context.printMessage(Diagnostic.Kind.NOTE, "Extracted JAR filename from direct access: " + jarFileName);

            if (jarFileName.matches(jarNameExpression)) {
                context.printMessage(Diagnostic.Kind.NOTE, "Resource matches JAR pattern: " + jarNameExpression);

                // Create a Spring Resource wrapper
                matchingResources.add(new UrlResource(resourceUrl));
            } else {
                context.printMessage(
                        Diagnostic.Kind.NOTE,
                        "Resource doesn't match JAR pattern. Filename: " + jarFileName + ", Pattern: "
                                + jarNameExpression);
            }
        } else {
            context.printMessage(
                    Diagnostic.Kind.NOTE, "Resource not found via direct classloader access: " + resourcePath);
        }

        // If direct access didn't work, try using Spring's ResourcePatternResolver with processor classloader
        if (matchingResources.isEmpty()) {
            context.printMessage(
                    Diagnostic.Kind.NOTE, "Trying Spring ResourcePatternResolver with processor classloader");

            try {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(processorClassLoader);

                // Try to get the resource directly using classpath: syntax
                Resource[] resources = resolver.getResources("classpath*:" + resourcePath);
                context.printMessage(
                        Diagnostic.Kind.NOTE,
                        "Found " + resources.length + " resources matching classpath*:" + resourcePath);

                for (Resource resource : resources) {
                    try {
                        String resourceUrlString = resource.getURL().toString();
                        context.printMessage(Diagnostic.Kind.NOTE, "Checking resource: " + resourceUrlString);

                        // Check if this matches our JAR pattern
                        String jarNameExpression = jarNamePattern.replaceAll("\\*", ".*");
                        // Extract JAR filename from URL for proper matching
                        String jarFileName = resourceUrlString.replaceAll(".*/([^/]+\\.jar)!.*", "$1");
                        context.printMessage(Diagnostic.Kind.NOTE, "Extracted JAR filename: " + jarFileName);

                        if (jarFileName.matches(jarNameExpression)) {
                            context.printMessage(
                                    Diagnostic.Kind.NOTE, "Resource matches JAR pattern: " + jarNameExpression);
                            matchingResources.add(resource);
                        } else {
                            context.printMessage(
                                    Diagnostic.Kind.NOTE,
                                    "Resource doesn't match JAR pattern. Filename: " + jarFileName + ", Pattern: "
                                            + jarNameExpression);
                        }
                    } catch (Exception e) {
                        context.printMessage(Diagnostic.Kind.NOTE, "Error checking resource: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                context.printMessage(Diagnostic.Kind.NOTE, "ResourcePatternResolver failed: " + e.getMessage());
            }
        }

        context.printMessage(
                Diagnostic.Kind.NOTE,
                "Resolved " + matchingResources.size() + " resources from JAR pattern: " + location);

        if (matchingResources.isEmpty()) {
            throw new IllegalArgumentException("No resources found matching JAR pattern: " + location
                    + ". This typically means the JAR is not on the annotation processor classpath. "
                    + "For Maven, add the JAR to <annotationProcessorPaths> in maven-compiler-plugin configuration. "
                    + "Note that annotation processor classpath is separate from compilation classpath.");
        }

        return matchingResources;
    }

    /**
     * Resolve classpath resources (e.g., "classpath:applicationContext.xml").
     * Uses multiple fallback strategies to handle annotation processor environments.
     */
    private List<Resource> resolveClasspathResources(String location, TranspilationContext context) throws Exception {
        String resourcePath = location.substring("classpath:".length());

        context.printMessage(Diagnostic.Kind.NOTE, "Resolving classpath resource: " + location);

        List<Resource> resources = new ArrayList<>();

        // Try to load resource from the annotation processing environment using multiple locations
        java.io.InputStream inputStream = null;
        try {
            // Try CLASS_OUTPUT first (most common for generated resources)
            try {
                javax.tools.FileObject fileObject = context.getProcessingEnvironment()
                        .getFiler()
                        .getResource(javax.tools.StandardLocation.CLASS_OUTPUT, "", resourcePath);
                inputStream = fileObject.openInputStream();
                context.printMessage(Diagnostic.Kind.NOTE, "Found resource in CLASS_OUTPUT: " + resourcePath);
            } catch (Exception e1) {
                // Try SOURCE_PATH
                try {
                    javax.tools.FileObject fileObject = context.getProcessingEnvironment()
                            .getFiler()
                            .getResource(javax.tools.StandardLocation.SOURCE_PATH, "", resourcePath);
                    inputStream = fileObject.openInputStream();
                    context.printMessage(Diagnostic.Kind.NOTE, "Found resource in SOURCE_PATH: " + resourcePath);
                } catch (Exception e2) {
                    // Try CLASS_PATH
                    try {
                        javax.tools.FileObject fileObject = context.getProcessingEnvironment()
                                .getFiler()
                                .getResource(javax.tools.StandardLocation.CLASS_PATH, "", resourcePath);
                        inputStream = fileObject.openInputStream();
                        context.printMessage(Diagnostic.Kind.NOTE, "Found resource in CLASS_PATH: " + resourcePath);
                    } catch (Exception e3) {
                        // Fallback to Spring ClassPathResource
                        ClassPathResource resource = new ClassPathResource(resourcePath);
                        if (resource.exists()) {
                            inputStream = resource.getInputStream();
                            context.printMessage(
                                    Diagnostic.Kind.NOTE, "Found resource via ClassPathResource: " + resourcePath);
                        } else {
                            throw new IllegalArgumentException("Resource not found: " + location
                                    + " (tried CLASS_OUTPUT, SOURCE_PATH, CLASS_PATH, and ClassPathResource)");
                        }
                    }
                }
            }

            if (inputStream != null) {
                // Create InputStreamResource wrapper
                resources.add(new org.springframework.core.io.InputStreamResource(inputStream) {
                    @Override
                    public String getDescription() {
                        return "Annotation processor resource [" + resourcePath + "]";
                    }
                });
            }

        } catch (Exception e) {
            // Final fallback: try using ResourcePatternResolver for pattern matching
            try {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resolvedResources = resolver.getResources(location);
                if (resolvedResources.length > 0) {
                    resources.addAll(List.of(resolvedResources));
                    context.printMessage(
                            Diagnostic.Kind.NOTE,
                            "Found " + resolvedResources.length + " resources via ResourcePatternResolver");
                }
            } catch (Exception patternResolverException) {
                context.printMessage(
                        Diagnostic.Kind.NOTE,
                        "ResourcePatternResolver also failed: " + patternResolverException.getMessage());
                throw e; // Re-throw original exception
            }
        }

        if (resources.isEmpty()) {
            throw new IOException("No resources found for classpath location: " + location);
        }

        return resources;
    }

    /**
     * Parse a single resource into bean definitions with name resolution information.
     * This enhanced version captures both bean definitions and name resolution metadata.
     */
    private ResourceParsingResult parseResourceWithNameInfo(Resource resource, TranspilationContext context)
            throws Exception {
        context.printMessage(Diagnostic.Kind.NOTE, "Parsing resource: " + resource.getDescription());

        // Read the resource content once for parsing
        String xmlContent = new String(resource.getInputStream().readAllBytes(), "UTF-8");

        // Use the enhanced XML parser to get both bean definitions and original XML
        XmlBeanDefinitionParser.ParsedXmlResult parsedResult = XmlBeanDefinitionParser.parseXmlContent(xmlContent);

        return new ResourceParsingResult(
                parsedResult.getBeanDefinitions(),
                parsedResult.getComponentScans(),
                parsedResult.getEnhancedBeanInfos());
    }

    /**
     * Create a bean generation context for the given bean definition.
     */
    private BeanGenerationContext createBeanGenerationContext(
            String beanName, BeanDefinition beanDefinition, TranspilationContext context) {

        // Get the enhanced bean info which now contains all name resolution information
        EnhancedBeanInfo enhancedBeanInfo = context.getEnhancedBeanInfos().get(beanName);

        return BeanGenerationContext.builder()
                .beanName(beanName)
                .beanDefinition(beanDefinition)
                .enhancedBeanInfo(enhancedBeanInfo) // Pass the consolidated enhanced bean info
                .transpilationContext(context)
                .build();
    }

    /**
     * Find the most appropriate visitor for the given bean definition.
     */
    private BeanDefinitionVisitor findVisitorForBean(BeanDefinition beanDefinition, TranspilationContext context) {
        return beanVisitors.stream()
                .filter(visitor -> visitor.canHandle(beanDefinition, context))
                .min(Comparator.comparingInt(BeanDefinitionVisitor::getPriority))
                .orElse(null);
    }

    /**
     * Generate @ComponentScan annotation for a component-scan configuration.
     * Based on the implementation from the old spring-factory-processor.
     */
    private void generateComponentScanAnnotation(ComponentScanInfo componentScan, TypeSpec.Builder configClass) {

        String[] basePackages = componentScan.getBasePackages();
        if (basePackages.length == 0) {
            // Skip component-scan with empty base-package
            return;
        }

        AnnotationSpec.Builder componentScanBuilder = AnnotationSpec.builder(ComponentScan.class);

        if (basePackages.length == 1) {
            // Single package
            componentScanBuilder.addMember("basePackages", "$S", basePackages[0]);
        } else {
            // Multiple packages - use CodeBlock for proper array syntax
            CodeBlock.Builder arrayBuilder = CodeBlock.builder().add("{");
            for (int i = 0; i < basePackages.length; i++) {
                if (i > 0) {
                    arrayBuilder.add(", ");
                }
                arrayBuilder.add("$S", basePackages[i].trim());
            }
            arrayBuilder.add("}");
            componentScanBuilder.addMember("basePackages", arrayBuilder.build());
        }

        // Add useDefaultFilters if it's explicitly set to false
        if (!componentScan.isUseDefaultFilters()) {
            componentScanBuilder.addMember("useDefaultFilters", "false");
        }

        // Add resourcePattern if specified
        String resourcePattern = componentScan.getResourcePattern();
        if (resourcePattern != null && !resourcePattern.trim().isEmpty()) {
            componentScanBuilder.addMember("resourcePattern", "$S", resourcePattern);
        }

        configClass.addAnnotation(componentScanBuilder.build());
    }

    /**
     * Create the default set of bean definition visitors.
     */
    private List<BeanDefinitionVisitor> createDefaultBeanVisitors() {
        List<BeanDefinitionVisitor> visitors = new ArrayList<>();

        // Add specialized visitors in priority order (lower priority number = higher precedence)
        visitors.add(new AbstractBeanInheritanceVisitor()); // Priority 50 - handles inheritance
        visitors.add(new ProxyFactoryBeanVisitor()); // Priority 40 - handles Spring AOP ProxyFactoryBean
        visitors.add(new ConstructorBasedBeanVisitor()); // Priority 100 - handles constructor injection
        visitors.add(new FactoryMethodBeanVisitor()); // Priority 150 - handles factory methods
        visitors.add(new SimpleBeanVisitor()); // Priority 200 - fallback visitor

        return visitors;
    }
}
