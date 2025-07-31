/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.nio.file.Path;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for XmlConfigTranspileProcessor annotation processing capabilities.
 *
 * Focuses on testing the processor's ability to handle different annotation
 * configurations: - JAR and classpath URL patterns - Include/exclude filtering
 * - Target package and class naming - Public access control - Multiple
 * annotations on same class
 *
 * Uses simple bean definitions from GeoServer's main applicationContext.xml for
 * realistic testing.
 */
class XmlConfigTranspileProcessorTest {

    @TempDir
    Path tempDir;

    private Compiler compilerWithProcessor() {
        return Compiler.javac().withProcessors(new XmlConfigTranspileProcessor());
    }

    private Compilation assertCompiles(String sourceCode) {

        String packageName = sourceCode
                .lines()
                .filter(l -> l.startsWith("package "))
                .map(l -> l.substring(8).replace(";", "").trim())
                .findFirst()
                .orElseThrow();
        String className = sourceCode
                .lines()
                .filter(l -> l.startsWith("public class "))
                .map(l -> l.substring(13).split("\\s+")[0].trim())
                .findFirst()
                .orElseThrow();
        String fullyQualifiedName = "%s.%s".formatted(packageName, className);

        JavaFileObject source = JavaFileObjects.forSourceString(fullyQualifiedName, sourceCode);
        Compilation compilation = compilerWithProcessor().compile(source);

        CompilationSubject.assertThat(compilation).succeeded();
        return compilation;
    }

    // Simple XML beans are now in src/test/resources/test-beans.xml

    /**
     * Tests basic classpath resource processing with default annotation settings.
     * <p>
     * Verifies that the processor can load XML from {@literal classpath:} URIs and
     * generate valid {@literal @Configuration} classes with {@literal @Bean}
     * methods.
     */
    @Test
    void testBasicClasspathProcessing() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(locations = "classpath:test-beans.xml")
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        // Verify the generated source contains expected content
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);
        assertThat(generatedSource)
                .as("Generated class should have @Configuration")
                .contains("@Configuration")
                .contains("proxyBeanMethods = false")
                .as("Generated class should contain gsMainModule bean method")
                .contains("ModuleStatusImpl gsMainModule(")
                .as("Generated class should contain @Bean annotations")
                .contains("@Bean");
    }

    @Test
    void testProxyBeanMethodsTrue() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    proxyBeanMethods = true
                )
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        // Verify the generated source contains expected content
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);
        assertThat(generatedSource)
                .as("Generated class should have @Configuration")
                .contains("@Configuration")
                .doesNotContain("proxyBeanMethods");
    }

    /**
     * Tests JAR pattern resource resolution using {@literal jar:} pattern/resource
     * syntax.
     * <p>
     * Focuses on the processor's ability to locate and parse XML files from JAR
     * resources on the classpath using regex patterns like
     * {@literal jar:gs-main-.*!/applicationContext.xml}.
     */
    @Test
    void testJarPatternProcessingSingleJar() {
        // This will test against gs-main JAR that's already on the classpath
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = {"jar:gs-main-.*!/applicationContext.xml"},
                     excludes={"advertisedCatalog","updateSequenceListener", "resourceLoader"}
                )
                @Import(JarTestConfiguration_Generated.class)
                public class JarTestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.JarTestConfiguration_Generated");

        // Verify generated class contains expected beans from JAR pattern
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.JarTestConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        compilation = compilerWithProcessor().compile(generatedFile);

        CompilationSubject.assertThat(compilation).succeeded();

        assertThat(generatedSource)
                .as("Generated class should have @Configuration")
                .contains("@Configuration")
                .contains("proxyBeanMethods = false")
                .as("Should contain extensions bean method")
                .contains("GeoServerExtensions extensions(")
                .as("Should contain filterFactory bean method")
                .contains("FilterFactoryImpl filterFactory(");
    }

    @Test
    void testJarPatternProcessingMultipleJars() {
        // This will test against gs-main JAR that's already on the classpath
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = {"jar:gs-main-.*!/applicationSecurityContext.xml","jar:gs-main-.*!/applicationContext.xml"},
                     excludes={"advertisedCatalog","updateSequenceListener", "resourceLoader"}
                )
                @Import(JarTestConfiguration_Generated.class)
                public class JarTestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.JarTestConfiguration_Generated");

        // Verify generated class contains expected beans from JAR pattern
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.JarTestConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        compilation = compilerWithProcessor().compile(generatedFile);

        CompilationSubject.assertThat(compilation).succeeded();

        assertThat(generatedSource)
                .as("Generated class should have @Configuration")
                .contains("@Configuration")
                .contains("proxyBeanMethods = false")
                .as("Should contain extensions bean method")
                .contains("GeoServerExtensions extensions(")
                .as("Should contain filterFactory bean method")
                .contains("FilterFactoryImpl filterFactory(");
    }

    @Test
    void testJarPatternProcessingWfsService() {
        // This will test against gs-wfs JAR that's already on the classpath
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = {"jar:gs-wfs-.*!/applicationContext.xml"}
                )
                @Import(JarTestConfiguration_Generated.class)
                public class JarTestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.JarTestConfiguration_Generated");

        // Verify generated class contains expected beans from JAR pattern
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.JarTestConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Generated class should have @Configuration")
                .contains("@Configuration")
                .contains("proxyBeanMethods = false")
                .as("Should contain WFS-specific bean methods")
                .contains("GML3OutputFormat gml3OutputFormat")
                .as("Should contain WFS workspace qualifier bean")
                .contains("WFSWorkspaceQualifier wfsWorkspaceQualifier");
    }

    @Test
    void testJarPatternProcessingWmsService() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;
                import com.example.wms.WmsConfig;

                @Configuration
                @TranspileXmlConfig(
                        locations = "jar:gs-wms-.*!/applicationContext.xml",
                        targetPackage = "com.example.wms",
                        targetClass = "WmsConfig",
                        publicAccess = true,
                        excludes = {"legendSample", "wmsExceptionHandler"})
                @Import({
                    WmsConfig.class
                })
                public class WmsAnnotationConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        // Should generate two separate configuration classes
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.wms.WmsConfig");
    }
    /**
     * Tests include/exclude regex pattern filtering functionality.
     * <p>
     * Verifies that only beans matching include patterns are generated while beans
     * matching exclude patterns are filtered out, demonstrating selective bean
     * processing from XML configurations.
     */
    @Test
    void testIncludeExcludeFiltering() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    includes = {".*Provider.*"},
                    excludes = {"nullLockProvider"}
                )
                @Import(FilteredConfiguration_Generated.class)
                public class FilteredConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        // Verify generated class contains memoryLockProvider but not nullLockProvider
        // or gsMainModule
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.FilteredConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Should include memoryLockProvider (matches .*Provider.*)")
                .contains("MemoryLockProvider memoryLockProvider(")
                .as("Should exclude nullLockProvider (in excludes)")
                .doesNotContain("NullLockProvider nullLockProvider(")
                .as("Should exclude gsMainModule (doesn't match .*Provider.*)")
                .doesNotContain("ModuleStatusImpl gsMainModule(")
                .as("Javadoc should mention excluded beans")
                .contains(
                        "<li>authenticationManager</li>",
                        "<li>dataDirectoryResourceStore</li>",
                        "<li>geoServerSecurityManager (alias for authenticationManager)</li>",
                        "<li>gsMainModule</li>",
                        "<li>nullLockProvider</li>",
                        "<li>resourceLoader</li>");
    }

    @Test
    void testExcludeBeanAlsoExcludesAlias() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    excludes = {"authenticationManager"}
                )
                @Import(FilteredConfiguration_Generated.class)
                public class FilteredConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        // Verify generated class contains memoryLockProvider but not nullLockProvider
        // or gsMainModule
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.FilteredConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Excluding authenticationManager should exclude the alias geoServerSecurityManager")
                .doesNotContain(
                        "GeoServerSecurityManager authenticationManager(",
                        "GeoServerSecurityManager geoServerSecurityManager(")
                .as("Other beans should be present")
                .contains(
                        "ModuleStatusImpl gsMainModule(",
                        "NullLockProvider nullLockProvider(",
                        "MemoryLockProvider memoryLockProvider(",
                        "DataDirectoryResourceStore dataDirectoryResourceStore(",
                        "GeoServerResourceLoader resourceLoader(")
                .as("Javadoc should mention excluded beans")
                .contains(
                        "<li>authenticationManager</li>",
                        "<li>geoServerSecurityManager (alias for authenticationManager)</li>");
    }

    @Test
    void testExcludeAliasAlsoExcludesBean() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    excludes = {"geoServerSecurityManager"}
                )
                @Import(FilteredConfiguration_Generated.class)
                public class FilteredConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        // Verify generated class contains memoryLockProvider but not nullLockProvider
        // or gsMainModule
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.FilteredConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Excluding geoServerSecurityManager should exclude the bean authenticationManager")
                .doesNotContain(
                        "GeoServerSecurityManager geoServerSecurityManager(",
                        "GeoServerSecurityManager authenticationManager(")
                .as("Other beans should be present")
                .contains(
                        "ModuleStatusImpl gsMainModule(",
                        "NullLockProvider nullLockProvider(",
                        "MemoryLockProvider memoryLockProvider(",
                        "DataDirectoryResourceStore dataDirectoryResourceStore(",
                        "GeoServerResourceLoader resourceLoader(")
                .as("Javadoc should mention excluded beans")
                .contains(
                        "<li>authenticationManager</li>",
                        "<li>geoServerSecurityManager (alias for authenticationManager)</li>");
    }

    /**
     * Tests custom target package and class name generation.
     * <p>
     * Verifies that the targetPackage and targetClass annotation parameters
     * correctly control where and how the generated {@literal @Configuration} class
     * is created, allowing for flexible organization of generated code.
     */
    @Test
    void testCustomTargetPackageAndClass() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    targetPackage = "com.example.generated",
                    targetClass = "CustomGeneratedConfig",
                    publicAccess = true
                )
                @Import(com.example.generated.CustomGeneratedConfig.class)
                public class CustomTargetConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.generated.CustomGeneratedConfig");

        // Verify generated class contains expected content
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.generated.CustomGeneratedConfig")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Generated class should be in custom package")
                .contains("package com.example.generated")
                .as("Generated class should have custom name")
                .contains("class CustomGeneratedConfig")
                .as("Generated class should have @Configuration")
                .contains("@Configuration")
                .as("Generated class should contain gsMainModule bean method")
                .contains("ModuleStatusImpl gsMainModule(");
    }

    /**
     * Tests publicAccess parameter for controlling class visibility.
     * <p>
     * Verifies that when publicAccess=true, the generated {@literal @Configuration}
     * class is public, while {@literal @Bean} methods remain package-private per
     * Spring conventions. When publicAccess=false (default), the class is
     * package-private.
     */
    @Test
    void testPublicAccess() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    publicAccess = true
                )
                @Import(PublicConfiguration_Generated.class)
                public class PublicConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.PublicConfiguration_Generated");

        // Verify generated class has public access modifiers
        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.PublicConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Generated class should be public")
                .contains("public class PublicConfiguration_Generated")
                .as("Bean methods should be package-private (Spring convention)")
                .contains("ModuleStatusImpl gsMainModule(")
                .as("Generated class should have @Configuration")
                .contains("@Configuration");
    }

    /**
     * Tests multiple {@literal @TranspileXmlConfig} annotations on the same class.
     * <p>
     * Verifies that the processor can handle multiple annotations with different
     * configurations and generate separate {@literal @Configuration} classes for
     * each, demonstrating the {@literal @Repeatable} annotation functionality.
     */
    @Test
    void testMultipleAnnotationsClasspath() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    includes = {".*Provider.*"},
                    targetClass = "ProviderConfig"
                )
                @TranspileXmlConfig(
                    locations = "classpath:test-beans.xml",
                    includes = {"gsMainModule"},
                    targetClass = "ModuleConfig"
                )
                @Import({ProviderConfig.class, ModuleConfig.class})
                public class MultipleAnnotationConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        // Should generate two separate configuration classes
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.ProviderConfig");
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.ModuleConfig");
    }

    @Test
    void testMultipleAnnotationsJars() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                        locations = "jar:gs-wms-.*!/applicationContext.xml",
                        targetPackage = "com.example.wms",
                        targetClass = "WmsConfig",
                        publicAccess = true,
                        excludes = {"legendSample", "wmsExceptionHandler"})
                @TranspileXmlConfig(
                        locations = "jar:gs-wfs-.*!/applicationContext.xml",
                        targetPackage = "com.example.wfs",
                        targetClass = "WfsConfig",
                        publicAccess = true,
                        includes = {
                            "gml.*OutputFormat",
                            "bboxKvpParser",
                            "featureIdKvpParser",
                            "filter.*_KvpParser",
                            "cqlKvpParser",
                            "maxFeatureKvpParser",
                            "sortByKvpParser",
                            "xmlConfiguration.*",
                            "gml[1-9]*SchemaBuilder",
                            "wfsXsd.*",
                            "wfsSqlViewKvpParser"
                        })
                @Import({
                    com.example.wms.WmsConfig.class,
                    com.example.wfs.WfsConfig.class
                })
                public class MultipleAnnotationConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        // Should generate two separate configuration classes
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.wms.WmsConfig");
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.wfs.WfsConfig");
    }

    /**
     * Tests error handling for invalid resource locations.
     * <p>
     * Verifies that the processor properly handles missing XML resources by failing
     * compilation with appropriate error messages, ensuring build-time validation
     * of resource existence.
     */
    @Test
    void testErrorHandling() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                @TranspileXmlConfig(locations = "classpath:nonexistent.xml")
                public class ErrorConfiguration {
                }
                """;

        String fullyQualifiedName = "com.example.ErrorConfiguration";
        JavaFileObject source = JavaFileObjects.forSourceString(fullyQualifiedName, sourceCode);

        Compilation compilation = compilerWithProcessor().compile(source);

        // Should fail compilation due to missing XML resource
        CompilationSubject.assertThat(compilation).failed();
    }

    /**
     * Tests error handling for missing JAR pattern resources.
     * <p>
     * Verifies that the processor fails compilation when no JARs match the
     * specified pattern, providing clear feedback about missing dependencies at
     * build time rather than silently generating empty classes.
     */
    @Test
    void testJarPatternNotFound() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                @TranspileXmlConfig(locations = "jar:nonexistent-.*!/applicationContext.xml")
                public class MissingJarConfiguration {
                }
                """;

        String fullyQualifiedName = "com.example.MissingJarConfiguration";
        JavaFileObject source = JavaFileObjects.forSourceString(fullyQualifiedName, sourceCode);

        Compilation compilation = compilerWithProcessor().compile(source);

        // Should fail compilation due to missing JAR pattern
        CompilationSubject.assertThat(compilation).failed();
    }

    /**
     * Tests component-scan processing from XML configurations.
     * <p>
     * Verifies that {@literal <context:component-scan>} elements are correctly
     * parsed and generate {@literal @ComponentScan} annotations on the
     * configuration class.
     * <p>
     * This test uses test-component-scan.xml which contains various component-scan
     * scenarios: single packages, multiple packages, and component-scan with
     * additional attributes.
     */
    @Test
    void testComponentScanProcessing() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig("classpath:test-component-scan.xml")
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        // Get the generated file
        JavaFileObject generated = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedContent = getSourceContent(generated);

        // Verify component-scan functionality using method chaining
        assertThat(generatedContent)
                .as("Should import ComponentScan annotation")
                .contains("import org.springframework.context.annotation.ComponentScan;")
                .as("Should have @ComponentScan annotations")
                .contains("@ComponentScan")
                .as("Should have basePackages attribute")
                .contains("basePackages")
                .as("Should have @Configuration annotation")
                .contains("@Configuration")
                .as("Should generate the correct class name")
                .contains("class TestConfiguration_Generated")
                .as("Should generate single package component-scan")
                .containsPattern(
                        """
                                @ComponentScan\\s*\\(\\s* \
                                basePackages\\s*=\\s*"com\\.example\\.single"\\s*\\)
                                """)
                .as("Should generate multiple packages component-scan")
                .containsPattern(
                        """
                        @ComponentScan\\s*\\(\\s* \
                        basePackages\\s*=\\s*\\{"com\\.example\\.first",\\s*"com\\.example\\.second"\\}\\s*\\)
                        """)
                .as("Should generate component-scan with additional attributes")
                .containsPattern(
                        """
                        @ComponentScan\\s*\\([^)]*basePackages\\s*=\\s*"com\\.example\\.advanced"[^)]* \
                        useDefaultFilters\\s*=\\s*false[^)]* \
                        resourcePattern\\s*=\\s*"\\*\\*/\\*\\.class"[^)]*\\)
                        """);
    }

    // Helper methods

    private String getSourceContent(JavaFileObject file) {
        try {
            CharSequence fileContents = file.getCharContent(false);
            return fileContents.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read generated source", e);
        }
    }
}
