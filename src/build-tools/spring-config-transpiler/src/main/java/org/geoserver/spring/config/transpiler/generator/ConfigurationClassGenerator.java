/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.CLASS_PATH;
import static javax.tools.StandardLocation.SOURCE_PATH;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.ComponentScanInfo;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.xml.EnhancedBeanDefinition;
import org.geoserver.spring.config.transpiler.xml.XmlBeanDefinitionParser;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Generator that generates the main {@code @Configuration} class containing all {@code @Bean} methods.
 *
 * <p>This generator is responsible for:
 *
 * <ul>
 *   <li>Creating the main class structure with {@code @Configuration} annotation
 *   <li>Loading and parsing XML resources to extract bean definitions
 *   <li>Delegating to specialized generators for individual {@code @Bean} methods
 *   <li>Applying filtering rules to include/exclude beans
 *   <li>Assembling the complete class specification
 * </ul>
 *
 * @since 3.0.0
 */
public class ConfigurationClassGenerator {

    private final List<BeanMethodGenerator> beanGenerators;

    /** Result of parsing a single resource, containing bean definitions and enhanced bean info. */
    private static class ResourceParsingResult {
        final Map<String, BeanDefinition> beanDefinitions;
        final List<ComponentScanInfo> componentScans;
        final Map<String, EnhancedBeanDefinition> enhancedBeanInfos;

        ResourceParsingResult(
                Map<String, BeanDefinition> beanDefinitions,
                List<ComponentScanInfo> componentScans,
                Map<String, EnhancedBeanDefinition> enhancedBeanInfos) {
            this.beanDefinitions = beanDefinitions;
            this.componentScans = componentScans;
            this.enhancedBeanInfos = enhancedBeanInfos;
        }
    }

    /** Create a new configuration class generator with default bean method generators. */
    public ConfigurationClassGenerator() {
        this.beanGenerators = createDefaultBeanMethodGenerators();
    }

    /**
     * Generate the complete {@code @Configuration} class for the given context.
     *
     * @param context the transpilation context
     * @return the generated {@code @Configuration} class specification
     */
    public TypeSpec generateConfigurationClass(TranspilationContext context) {

        // Base type spec with @Configuration annotation and class modifiers
        TypeSpec.Builder classBuilder = initializeTypeSpec(context);

        try {
            // Load bean definitions from XML and generate {@code @Bean} methods
            loadBeanDefinitions(context);

            List<String> excludedBeans = addBeanMethods(context, classBuilder);

            addComponentScan(context, classBuilder);

            addClassJavadocs(context, excludedBeans, classBuilder);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate configuration class: %s".formatted(e.getMessage()), e);
        }

        return classBuilder.build();
    }

    private List<String> addBeanMethods(TranspilationContext context, TypeSpec.Builder classBuilder) {
        // Track excluded beans for javadoc
        List<String> excludedBeans = new ArrayList<>();

        // Deduplicate bean definitions. If the same BeanDefinition object appears under
        // multiple names, only process it once using the primary name (the first occurrence).
        // Use IdentityHashMap to compare by object reference, not equals(), so that
        // distinct beans with identical property values (e.g. breadcrumbCatData and
        // breadcrumbCatLayerData) are not incorrectly deduplicated.
        Map<BeanDefinition, String> processedBeanDefinitions = new IdentityHashMap<>();

        final Map<String, BeanDefinition> allBeanDefinitions = context.getAllBeanDefinitions();

        for (Map.Entry<String, BeanDefinition> entry : allBeanDefinitions.entrySet()) {

            final String beanName = entry.getKey();
            final BeanDefinition beanDefinition = entry.getValue();

            // If we've already processed this exact BeanDefinition object, skip it
            if (skipDuplicate(beanName, beanDefinition, processedBeanDefinitions, context)) {
                continue;
            }
            // Mark this BeanDefinition as processed
            processedBeanDefinitions.put(beanDefinition, beanName);

            Optional<MethodSpec> beanMethod = generateBeanMethod(beanName, beanDefinition, context);
            beanMethod.ifPresentOrElse(classBuilder::addMethod, () -> excludedBeans.add(beanName));
        }
        return excludedBeans;
    }

    private Optional<MethodSpec> generateBeanMethod(
            String beanName, BeanDefinition beanDefinition, TranspilationContext context) {

        BeanGenerationContext beanContext = createBeanGenerationContext(beanName, beanDefinition, context);

        // Skip if bean should be excluded based on include/exclude patterns in the TranspileXmlConfig annotation
        if (context.shouldIncludeBean(beanContext.getBeanName())) {
            // Find appropriate generator and generate {@code @Bean} method
            return findGeneratorForBean(beanDefinition).map(generator -> generator.generateBeanMethod(beanContext));
        }
        return Optional.empty();
    }

    private boolean skipDuplicate(
            String beanName,
            BeanDefinition beanDefinition,
            Map<BeanDefinition, String> processed,
            TranspilationContext context) {

        String primaryName = processed.get(beanDefinition);
        boolean skip = primaryName != null;
        if (skip) {
            context.printMessage(
                    NOTE,
                    "Skipping duplicate bean definition: %s (already processed as %s)"
                            .formatted(beanName, primaryName));
        }
        return skip;
    }

    // Handle component-scan elements based on the configured strategy
    private void addComponentScan(TranspilationContext context, TypeSpec.Builder classBuilder) {
        switch (context.getComponentScanStrategy()) {
            case IGNORE -> {
                // do nothing
            }
            case INCLUDE -> {
                for (ComponentScanInfo componentScan : context.getComponentScans()) {
                    generateComponentScanAnnotation(componentScan, classBuilder);
                }
            }
            case GENERATE -> {
                ComponentScanBeanGenerator scanner = new ComponentScanBeanGenerator();
                TypeSpec innerClass = scanner.generateComponentScannedBeans(context.getComponentScans(), context);
                if (innerClass != null) {
                    classBuilder.addType(innerClass);
                    // Import the inner class so Spring registers its @Bean methods
                    ClassName innerClassName =
                            ClassName.get(context.getTargetPackage(), context.getTargetClassName(), innerClass.name());
                    classBuilder.addAnnotation(
                            AnnotationSpec.builder(org.springframework.context.annotation.Import.class)
                                    .addMember("value", "$T.class", innerClassName)
                                    .build());
                }
            }
        }
    }

    private TypeSpec.Builder initializeTypeSpec(TranspilationContext context) {
        // Determine class visibility
        final Modifier[] classModifiers =
                context.isPublicAccess() ? new Modifier[] {Modifier.PUBLIC} : new Modifier[0]; // Package-private

        // Create the class builder with @Configuration annotation
        AnnotationSpec.Builder configAnnotationBuilder = AnnotationSpec.builder(Configuration.class);
        if (!context.isProxyBeanMethods()) {
            configAnnotationBuilder.addMember("proxyBeanMethods", "false");
        }

        return TypeSpec.classBuilder(context.getTargetClassName())
                .addModifiers(classModifiers)
                .addAnnotation(configAnnotationBuilder.build());
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

    private void loadBeanDefinitions(TranspilationContext context) throws IOException {
        Map<String, BeanDefinition> allBeanDefinitions = new LinkedHashMap<>();

        // Global component scan information collected during parsing
        List<ComponentScanInfo> globalComponentScans = new ArrayList<>();

        // Global enhanced bean information with original XML and name resolution
        // collected during parsing
        Map<String, EnhancedBeanDefinition> globalEnhancedBeanInfos = new LinkedHashMap<>();

        // Process each XML location
        for (String location : context.getXmlLocations()) {
            List<Resource> resources;
            try {
                resources = resolveResources(location, context);
            } catch (IOException e) {
                context.printMessage(ERROR, "Resource not found for location '" + location + "': " + e.getMessage());
                throw e; // Fail compilation
            }

            // Parse all resolved resources and combine results
            for (Resource resource : resources) {
                ResourceParsingResult parsingResult = parseResourceWithNameInfo(resource, context);
                allBeanDefinitions.putAll(parsingResult.beanDefinitions);
                globalComponentScans.addAll(parsingResult.componentScans);
                globalEnhancedBeanInfos.putAll(parsingResult.enhancedBeanInfos);

                // Log successful parsing
                context.printMessage(
                        NOTE,
                        "Loaded " + parsingResult.beanDefinitions.size() + " bean definitions from: "
                                + resource.getDescription());
            }
        }

        // Set the bean definitions and enhanced bean info on the context for generator access
        context.setAllBeanDefinitions(allBeanDefinitions);
        context.setEnhancedBeanInfos(globalEnhancedBeanInfos);
        context.setComponentScans(globalComponentScans);
    }

    /** Resolve resources from a location string (supporting classpath: and jar: patterns). */
    private List<Resource> resolveResources(String location, TranspilationContext context) throws IOException {
        if (location.startsWith("jar:")) {
            return resolveJarPatternResources(location, context);
        } else if (location.startsWith("classpath:")) {
            return resolveClasspathResources(location, context);
        } else {
            throw new IOException(
                    "Unsupported location type: " + location + ". Supported: classpath: and jar: resources.");
        }
    }

    /**
     * Resolve JAR pattern resources (e.g., "jar:gs-main-.*!/applicationContext.xml"). Handles robust resource loading
     * with fallback strategies for annotation processor environments.
     */
    private List<Resource> resolveJarPatternResources(String location, TranspilationContext context)
            throws IOException {
        context.printMessage(NOTE, "Resolving JAR pattern: " + location);

        // Extract JAR name pattern and resource path from location
        // Format: jar:gs-main-.*!/applicationContext.xml
        final String jarNamePattern = location.substring("jar:".length(), location.indexOf("!"));
        final String resourcePath = location.substring(location.indexOf("!/") + 2);

        context.printMessage(NOTE, "JAR pattern: %s, Resource path: %s".formatted(jarNamePattern, resourcePath));

        List<Resource> matchingResources = new ArrayList<>();

        // Use annotation processor's classloader directly
        ClassLoader processorClassLoader = this.getClass().getClassLoader();
        context.printMessage(NOTE, "Annotation processor classloader: " + processorClassLoader);

        // Try direct resource access first - this should work for annotationProcessorPaths JARs
        final URL resourceUrl = processorClassLoader.getResource(resourcePath);
        if (resourceUrl == null) {
            context.printMessage(NOTE, "Resource not found via direct classloader access: " + resourcePath);
        } else {
            context.printMessage(NOTE, "Found resource directly: " + resourceUrl);

            // Check if this URL matches our JAR pattern
            String urlString = resourceUrl.toString();
            context.printMessage(NOTE, "Resource URL: " + urlString);

            // Convert pattern to regex for matching
            String jarNameExpression = jarNamePattern.replace("\\*", ".*");

            // Extract JAR filename from URL for proper matching
            String jarFileName = urlString.replaceAll(".*/([^/]+\\.jar)!.*", "$1");
            context.printMessage(NOTE, "Extracted JAR filename from direct access: " + jarFileName);

            if (jarFileName.matches(jarNameExpression)) {
                context.printMessage(NOTE, "Resource matches JAR pattern: " + jarNameExpression);

                // Create a Spring Resource wrapper
                matchingResources.add(new UrlResource(resourceUrl));
            }
        }

        // If direct access didn't work, try using Spring's ResourcePatternResolver with
        // processor classloader
        if (matchingResources.isEmpty()) {
            matchingResources = findResourcesWithClassLoader(context, jarNamePattern, resourcePath);
        }

        if (matchingResources.isEmpty()) {
            throw new IOException(
                    """
					No resources found matching JAR pattern: %s.\
					This typically means the JAR is not on the annotation processor classpath.\
					For Maven, add the JAR to <annotationProcessorPaths> in maven-compiler-plugin configuration.\
					Note that annotation processor classpath is separate from compilation classpath.\
					"""
                            .formatted(location));
        } else {
            context.printMessage(
                    NOTE, "Resolved %d resources from JAR pattern: %s".formatted(matchingResources.size(), location));
        }

        return matchingResources;
    }

    private List<Resource> findResourcesWithClassLoader(
            TranspilationContext context, final String jarNamePattern, final String resourcePath) {

        ClassLoader processorClassLoader = this.getClass().getClassLoader();
        List<Resource> matching = new ArrayList<>();
        context.printMessage(NOTE, "Trying Spring ResourcePatternResolver with processor classloader");

        Resource[] resources;
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(processorClassLoader);

            // Try to get the resource directly using classpath: syntax
            resources = resolver.getResources("classpath*:" + resourcePath);
        } catch (IOException e) {
            context.printMessage(NOTE, "ResourcePatternResolver failed: " + e.getMessage());
            return List.of();
        }
        context.printMessage(
                NOTE, "Found %d resources matching classpath*:%s".formatted(resources.length, resourcePath));

        for (Resource resource : resources) {
            String resourceUrlString;
            try {
                resourceUrlString = resource.getURL().toString();
            } catch (IOException e) {
                context.printMessage(NOTE, "Error checking resource: " + e.getMessage());
                continue;
            }
            // Check if this matches our JAR pattern
            String jarNameExpression = jarNamePattern.replace("\\*", ".*");
            // Extract JAR filename from URL for proper matching
            String jarFileName = resourceUrlString.replaceAll(".*/([^/]+\\.jar)!.*", "$1");

            if (jarFileName.matches(jarNameExpression)) {
                context.printMessage(NOTE, "Resource matches JAR pattern: " + jarNameExpression);
                matching.add(resource);
            }
        }
        return matching;
    }

    /**
     * Resolve classpath resources (e.g., "classpath:applicationContext.xml"). Uses multiple fallback strategies to
     * handle annotation processor environments.
     */
    @SuppressWarnings("java:S2095")
    private List<Resource> resolveClasspathResources(String location, TranspilationContext context) throws IOException {
        context.printMessage(NOTE, "Resolving classpath resource: " + location);

        List<Resource> resources = new ArrayList<>();

        final String resourcePath = location.substring("classpath:".length());
        try {
            // Try to load resource from the annotation processing environment using multiple locations
            Resource resource;
            resource = resolveWithCompilerLocation(resourcePath, context, CLASS_OUTPUT, SOURCE_PATH, CLASS_PATH);
            resources.add(resource);
        } catch (IOException e) {
            // Final fallback: try using ResourcePatternResolver for pattern matching
            try {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resolvedResources = resolver.getResources(location);
                if (resolvedResources.length > 0) {
                    resources.addAll(List.of(resolvedResources));
                    context.printMessage(
                            NOTE, "Found %d resources via ResourcePatternResolver".formatted(resolvedResources.length));
                }
            } catch (IOException patternResolverException) {
                context.printMessage(
                        NOTE, "ResourcePatternResolver also failed: " + patternResolverException.getMessage());
                e.addSuppressed(patternResolverException);
                throw e; // Re-throw original exception
            }
        }

        if (resources.isEmpty()) {
            throw new IOException("No resources found for classpath location: " + location);
        }

        return resources;
    }

    private Resource resolveWithCompilerLocation(
            String resourcePath, TranspilationContext context, StandardLocation... locations) throws IOException {
        final Filer filer = context.getProcessingEnvironment().getFiler();

        for (StandardLocation location : locations) {
            FileObject fileObject;
            try {
                fileObject = filer.getResource(location, "", resourcePath);
            } catch (IOException _) {
                continue;
            }

            try (InputStream inputStream = fileObject.openInputStream()) {
                context.printMessage(NOTE, "Found resource in %s: %s".formatted(location, resourcePath));
                byte[] resourceContents = inputStream.readAllBytes();
                String description = "Annotation processor resource [" + resourcePath + "]";

                return new ByteArrayResource(resourceContents, description);
            } catch (IOException _) {
                // resource not found in location
            }
        }
        // Fallback to Spring ClassPathResource
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (resource.exists()) {
            context.printMessage(NOTE, "Found resource via ClassPathResource: " + resourcePath);
            return resource;
        }
        throw new IOException("Resource not found: " + resourcePath
                + " (tried CLASS_OUTPUT, SOURCE_PATH, CLASS_PATH, and ClassPathResource)");
    }

    /**
     * Parse a single resource into bean definitions with name resolution information. This enhanced version captures
     * both bean definitions and name resolution metadata.
     */
    private ResourceParsingResult parseResourceWithNameInfo(Resource resource, TranspilationContext context)
            throws IOException {
        context.printMessage(NOTE, "Parsing resource: " + resource.getDescription());

        // Read the resource content once for parsing
        String xmlContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // Use the enhanced XML parser to get both bean definitions and original XML
        XmlBeanDefinitionParser.ParsedXmlResult parsedResult = XmlBeanDefinitionParser.parseXmlContent(xmlContent);

        return new ResourceParsingResult(
                parsedResult.getBeanDefinitions(),
                parsedResult.getComponentScans(),
                parsedResult.getEnhancedBeanInfos());
    }

    /** Create a bean generation context for the given bean definition. */
    private BeanGenerationContext createBeanGenerationContext(
            String beanName, BeanDefinition beanDefinition, TranspilationContext context) {

        // Get the enhanced bean info which contains all name resolution information
        EnhancedBeanDefinition enhancedBeanInfo = context.getEnhancedBeanInfos().get(beanName);

        return BeanGenerationContext.builder()
                .beanDefinition(beanDefinition)
                .enhancedBeanInfo(enhancedBeanInfo)
                .transpilationContext(context)
                .build();
    }

    /** Find the most appropriate generator for the given bean definition. */
    private Optional<BeanMethodGenerator> findGeneratorForBean(BeanDefinition beanDefinition) {
        return beanGenerators.stream()
                .filter(generator -> generator.canHandle(beanDefinition))
                .min(Comparator.comparingInt(BeanMethodGenerator::getPriority));
    }

    /**
     * Generate @ComponentScan annotation for a component-scan configuration. Based on the implementation from the old
     * spring-factory-processor.
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

    /** Create the default set of bean method generators. */
    private List<BeanMethodGenerator> createDefaultBeanMethodGenerators() {
        List<BeanMethodGenerator> generators = new ArrayList<>();

        // Add specialized generators in priority order (lower priority number = higher
        // precedence)
        generators.add(new AbstractBeanInheritanceMethodGenerator()); // Priority 50 - handles inheritance
        generators.add(new ProxyFactoryBeanMethodGenerator()); // Priority 40 - handles Spring AOP ProxyFactoryBean
        generators.add(new ConstructorBasedBeanMethodGenerator()); // Priority 100 - handles constructor injection
        generators.add(new FactoryMethodBeanMethodGenerator()); // Priority 150 - handles factory methods
        generators.add(new SimpleBeanMethodGenerator()); // Priority 200 - fallback generator

        return generators;
    }
}
