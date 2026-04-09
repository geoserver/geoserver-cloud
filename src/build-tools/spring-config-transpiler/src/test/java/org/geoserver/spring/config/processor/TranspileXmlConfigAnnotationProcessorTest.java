/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
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
import java.util.List;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Tests for {@link TranspileXmlConfigAnnotationProcessor} annotation processing capabilities.
 *
 * <p>Focuses on testing the processor's ability to handle different annotation configurations: - JAR and classpath URL
 * patterns - Include/exclude filtering - Target package and class naming - Public access control - Multiple annotations
 * on same class
 *
 * <p>Uses simple bean definitions from GeoServer's main applicationContext.xml for realistic testing.
 *
 * @since 3.0.0
 */
@Execution(value = ExecutionMode.CONCURRENT)
class TranspileXmlConfigAnnotationProcessorTest {

    private Compiler compilerWithProcessor() {
        return Compiler.javac().withProcessors(new TranspileXmlConfigAnnotationProcessor());
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
     *
     * <p>Verifies that the processor can load XML from {@literal classpath:} URIs and generate valid
     * {@literal @Configuration} classes with {@literal @Bean} methods.
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
     * Tests JAR pattern resource resolution using {@literal jar:} pattern/resource syntax.
     *
     * <p>Focuses on the processor's ability to locate and parse XML files from JAR resources on the classpath using
     * regex patterns like {@literal jar:gs-main-.*!/applicationContext.xml}.
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
    void testJarPatternProcessingWfsCore() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = {"jar:gs-wfs-core.*!/applicationContext.xml"}
                )
                @Import(JarTestConfiguration_Generated.class)
                public class JarTestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.JarTestConfiguration_Generated");

        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.JarTestConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Generated class should have @Configuration")
                .contains("@Configuration")
                .contains("proxyBeanMethods = false")
                .as("Should contain WFS workspace qualifier bean from gs-wfs-core")
                .contains("WFSWorkspaceQualifier wfsWorkspaceQualifier")
                .as("GML3OutputFormat moved to gs-wfs1_x, should not be in gs-wfs-core")
                .doesNotContain("GML3OutputFormat gml3OutputFormat");
    }

    @Test
    void testJarPatternProcessingWfsVersionModules() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = {"jar:gs-wfs1_x.*!/applicationContext.xml", "jar:gs-wfs2_x.*!/applicationContext.xml"}
                )
                @Import(JarTestConfiguration_Generated.class)
                public class JarTestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);

        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.JarTestConfiguration_Generated");

        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.JarTestConfiguration_Generated")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Should contain GML3OutputFormat from gs-wfs1_x")
                .contains("GML3OutputFormat gml3OutputFormat")
                .as("Should contain GML32OutputFormat from gs-wfs2_x")
                .contains("GML32OutputFormat gml32OutputFormat")
                .as("Should contain xmlConfiguration-1.0 from gs-wfs1_x")
                .contains("WFSConfiguration xmlConfiguration_1_0");
    }

    @Test
    void testJarPatternProcessingWmsCore() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;
                import com.example.wms.WmsConfig;

                @Configuration
                @TranspileXmlConfig(
                        locations = "jar:gs-wms-core.*!/applicationContext.xml",
                        targetPackage = "com.example.wms",
                        targetClass = "WmsConfig",
                        publicAccess = true,
                        excludes = {"legendSample"})
                @Import({
                    WmsConfig.class
                })
                public class WmsAnnotationConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.wms.WmsConfig");

        JavaFileObject generatedFile =
                compilation.generatedSourceFile("com.example.wms.WmsConfig").get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Should contain wmsServiceTarget from gs-wms-core")
                .contains("DefaultWebMapService wmsServiceTarget");
    }

    @Test
    void testJarPatternProcessingWmsVersionModules() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;
                import com.example.wms.WmsVersionConfig;

                @Configuration
                @TranspileXmlConfig(
                        locations = {"jar:gs-wms1_1.*!/applicationContext.xml", "jar:gs-wms1_3.*!/applicationContext.xml"},
                        targetPackage = "com.example.wms",
                        targetClass = "WmsVersionConfig",
                        publicAccess = true)
                @Import({
                    WmsVersionConfig.class
                })
                public class WmsVersionAnnotationConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.wms.WmsVersionConfig");

        JavaFileObject generatedFile = compilation
                .generatedSourceFile("com.example.wms.WmsVersionConfig")
                .get();
        String generatedSource = getSourceContent(generatedFile);

        assertThat(generatedSource)
                .as("Should contain WMS 1.1.1 service descriptor from gs-wms1_1")
                .contains("Service wms_1_1_1_ServiceDescriptor")
                .as("Should contain WMS 1.3.0 service descriptor from gs-wms1_3")
                .contains("Service wms_1_3_0_ServiceDescriptor");
    }
    /**
     * Tests include/exclude regex pattern filtering functionality.
     *
     * <p>Verifies that only beans matching include patterns are generated while beans matching exclude patterns are
     * filtered out, demonstrating selective bean processing from XML configurations.
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
     *
     * <p>Verifies that the targetPackage and targetClass annotation parameters correctly control where and how the
     * generated {@literal @Configuration} class is created, allowing for flexible organization of generated code.
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
     *
     * <p>Verifies that when publicAccess=true, the generated {@literal @Configuration} class is public, while
     * {@literal @Bean} methods remain package-private per Spring conventions. When publicAccess=false (default), the
     * class is package-private.
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
     *
     * <p>Verifies that the processor can handle multiple annotations with different configurations and generate
     * separate {@literal @Configuration} classes for each, demonstrating the {@literal @Repeatable} annotation
     * functionality.
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
                        locations = "jar:gs-wms.*!/applicationContext.xml",
                        targetPackage = "com.example.wms",
                        targetClass = "WmsConfig",
                        publicAccess = true,
                        excludes = {"legendSample", "wmsExceptionHandler"})
                @TranspileXmlConfig(
                        locations = "jar:gs-wfs.*!/applicationContext.xml",
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

        // Verify WMS config includes beans from gs-wms-core, gs-wms1_1, and gs-wms1_3
        String wmsSource = getSourceContent(
                compilation.generatedSourceFile("com.example.wms.WmsConfig").get());
        assertThat(wmsSource)
                .as("Should contain wmsServiceTarget from gs-wms-core")
                .contains("DefaultWebMapService wmsServiceTarget")
                .as("Should contain WMS 1.1.1 service descriptor from gs-wms1_1")
                .contains("Service wms_1_1_1_ServiceDescriptor")
                .as("Should contain WMS 1.3.0 service descriptor from gs-wms1_3")
                .contains("Service wms_1_3_0_ServiceDescriptor");

        // Verify WFS config includes beans from gs-wfs-core, gs-wfs1_x, and gs-wfs2_x
        String wfsSource = getSourceContent(
                compilation.generatedSourceFile("com.example.wfs.WfsConfig").get());
        assertThat(wfsSource)
                .as("Should contain maxFeatureKvpParser from gs-wfs-core")
                .contains("NumericKvpParser maxFeatureKvpParser")
                .as("Should contain gml3OutputFormat from gs-wfs1_x")
                .contains("GML3OutputFormat gml3OutputFormat")
                .as("Should contain gml32OutputFormat from gs-wfs2_x")
                .contains("GML32OutputFormat gml32OutputFormat")
                .as("Should contain xmlConfiguration-1.0 from gs-wfs1_x")
                .contains("WFSConfiguration xmlConfiguration_1_0");
    }

    /**
     * Tests error handling for invalid resource locations.
     *
     * <p>Verifies that the processor properly handles missing XML resources by failing compilation with appropriate
     * error messages, ensuring build-time validation of resource existence.
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
     *
     * <p>Verifies that the processor fails compilation when no JARs match the specified pattern, providing clear
     * feedback about missing dependencies at build time rather than silently generating empty classes.
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
     *
     * <p>Verifies that {@literal <context:component-scan>} elements are correctly parsed and generate
     * {@literal @ComponentScan} annotations on the configuration class.
     *
     * <p>This test uses test-component-scan.xml which contains various component-scan scenarios: single packages,
     * multiple packages, and component-scan with additional attributes.
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

    /**
     * Verifies that setting {@code componentScanStrategy = IGNORE} prevents {@code <context:component-scan>} elements
     * from being transpiled into {@code @ComponentScan} annotations.
     */
    @Test
    void testIgnoreComponentScanProcessing() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.geoserver.spring.config.annotations.ComponentScanStrategy;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    value = "classpath:test-component-scan.xml",
                    componentScanStrategy = ComponentScanStrategy.IGNORE
                )
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        JavaFileObject generated = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedContent = getSourceContent(generated);

        assertThat(generatedContent)
                .as("Should have @Configuration annotation")
                .contains("@Configuration")
                .as("Should generate the correct class name")
                .contains("class TestConfiguration_Generated")
                .as("Should NOT import ComponentScan when strategy=IGNORE")
                .doesNotContain("import org.springframework.context.annotation.ComponentScan;")
                .as("Should NOT have @ComponentScan annotations when strategy=IGNORE")
                .doesNotContain("@ComponentScan");
    }

    /**
     * Tests that the GENERATE component scan strategy performs build-time classpath scanning and generates @Bean
     * methods for discovered components in a static inner @Configuration class.
     *
     * <p>Uses test fixture components in {@code org.geoserver.spring.config.test.components}:
     *
     * <ul>
     *   <li>{@code SimpleComponent} — no-arg constructor, should generate simple @Bean method
     *   <li>{@code ComponentWithDependency} — single-param constructor, should generate @Bean with parameter
     *   <li>{@code AbstractComponent} — abstract, should be skipped
     *   <li>{@code InterfaceComponent} — interface, should be skipped
     * </ul>
     */
    @Test
    void testComponentScanGenerateStrategy() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.geoserver.spring.config.annotations.ComponentScanStrategy;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    value = "classpath:test-component-scan-generate.xml",
                    componentScanStrategy = ComponentScanStrategy.GENERATE
                )
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        JavaFileObject generated = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedContent = getSourceContent(generated);

        assertThat(generatedContent)
                .as("Should have outer @Configuration annotation")
                .contains("@Configuration")
                .as("Should NOT have @ComponentScan annotations in GENERATE mode")
                .doesNotContainPattern("(?m)^\\s*@ComponentScan")
                .as("Should @Import the inner ComponentScannedBeans class")
                .contains("@Import(TestConfiguration_Generated.ComponentScannedBeans.class)")
                .as("Should have inner static ComponentScannedBeans class")
                .contains("static class ComponentScannedBeans")
                .as("Should generate @Bean method for SimpleComponent (no-arg constructor)")
                .contains("SimpleComponent simpleComponent()")
                .contains("return new SimpleComponent()")
                .as("Should have per-method javadoc with FQCN, base-package, and XML source")
                .contains("discovered through")
                .contains("context:component-scan base-package=\"org.geoserver.spring.config.test.components\"")
                .contains("classpath:test-component-scan-generate.xml")
                .as("Should generate @Bean method for ComponentWithDependency (constructor injection)")
                .contains("ComponentWithDependency componentWithDependency(")
                .contains("GeoServer geoServer")
                .contains("return new ComponentWithDependency(geoServer)")
                .as("Should generate @Bean method for @ControllerAdvice (meta-annotated with @Component)")
                .contains("ControllerAdviceComponent controllerAdviceComponent()")
                .as("Should use @Autowired constructor when multiple constructors exist")
                .contains("ComponentWithAutowiredConstructor componentWithAutowiredConstructor(")
                .contains("GeoServerResourceLoader geoServerResourceLoader")
                .as("Should generate @Bean method for outer component")
                .contains("OuterComponent outerComponent()")
                .as("Should NOT generate methods for abstract classes")
                .doesNotContain("AbstractComponent abstractComponent()")
                .as("Should NOT generate methods for interfaces")
                .doesNotContain("InterfaceComponent interfaceComponent()")
                .as("Should NOT generate @Bean methods for member/inner classes")
                .doesNotContain("InnerConfiguration innerConfiguration()")
                .doesNotContain("InnerConfiguration outerComponent$InnerConfiguration()");
    }

    /** Tests that the GENERATE strategy respects exclude patterns, matching against the default bean name. */
    @Test
    void testComponentScanGenerateWithExcludes() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.geoserver.spring.config.annotations.ComponentScanStrategy;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    value = "classpath:test-component-scan-generate.xml",
                    componentScanStrategy = ComponentScanStrategy.GENERATE,
                    excludes = {"simpleComponent"}
                )
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        JavaFileObject generated = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedContent = getSourceContent(generated);

        assertThat(generatedContent)
                .as("Should NOT contain excluded bean")
                .doesNotContain("SimpleComponent simpleComponent()")
                .as("Should still contain non-excluded bean")
                .contains("ComponentWithDependency componentWithDependency(");
    }

    /** Tests that the GENERATE strategy respects exclude patterns matching against FQCNs. */
    @Test
    void testComponentScanGenerateWithFqcnExcludes() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.geoserver.spring.config.annotations.ComponentScanStrategy;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    value = "classpath:test-component-scan-generate.xml",
                    componentScanStrategy = ComponentScanStrategy.GENERATE,
                    excludes = {".*ComponentWithDependency"}
                )
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        JavaFileObject generated = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedContent = getSourceContent(generated);

        assertThat(generatedContent)
                .as("Should NOT contain FQCN-excluded bean")
                .doesNotContain("componentWithDependency(")
                .as("Should still contain non-excluded bean")
                .contains("SimpleComponent simpleComponent(");
    }

    /** Tests the GENERATE strategy with a real GeoServer package (org.geoserver.system.status from gs-main). */
    @Test
    void testComponentScanGenerateWithRealGeoServerPackage() {
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.geoserver.spring.config.annotations.ComponentScanStrategy;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    value = "classpath:test-beans.xml",
                    componentScanStrategy = ComponentScanStrategy.GENERATE
                )
                @Import(TestConfiguration_Generated.class)
                public class TestConfiguration {
                }
                """;

        // test-beans.xml has no component-scan, so GENERATE with no scans should produce no inner class
        Compilation compilation = assertCompiles(sourceCode);
        CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.TestConfiguration_Generated");

        JavaFileObject generated = compilation
                .generatedSourceFile("com.example.TestConfiguration_Generated")
                .get();
        String generatedContent = getSourceContent(generated);

        assertThat(generatedContent)
                .as("Should NOT have inner class when no component-scan elements exist")
                .doesNotContain("ComponentScannedBeans");
    }

    @Test
    void testWebCore() {
        // This will test against gs-main JAR that's already on the classpath
        String sourceCode =
                """
                package com.example;

                import org.geoserver.spring.config.annotations.TranspileXmlConfig;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @TranspileXmlConfig(
                    locations = {"jar:gs-web-core-.*!/applicationContext.xml"}
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
                .contains("proxyBeanMethods = false");

        List<String> expectedBeanNames = List.of(
                "aboutMenuPage",
                "authenticatedAuthorizer",
                "basicResourceConfigPanel",
                "breadcrumbCatData",
                "breadcrumbCatLayerData",
                "breadcrumbCatLayerGroupMenu",
                "breadcrumbCatLayerMenu",
                "breadcrumbCatServiceOverrides",
                "breadcrumbCatTileCaching",
                "breadcrumbCatWorkspace",
                "breadcrumbItemLayerEdit",
                "breadcrumbItemLayerGroupEdit",
                "breadcrumbItemLayerStores",
                "breadcrumbItemWsEditSettings",
                "breadcrumbItemWsLayerGroups",
                "breadcrumbItemWsLayers",
                "breadcrumbItemWsStores",
                "contactMenuPage",
                "coverageAccessMenuPage",
                "coverageBandsConfigPanel",
                "coverageResourceConfigPanel",
                "dataCategory",
                "defaultCoverageStorePanel",
                "defaultDataStorePanel",
                "demoMenuPage",
                "featureResourceConfigPanel",
                "formatHeadingConfigPanel",
                "geopkgStorePanel",
                "geoserverCategory",
                "geoserverConsoleDisabled",
                "geoserverFormLoginButton",
                "geotiffStorePanel",
                "globalSettingsMenuPage",
                "httpLayerConfigPanel",
                "httpLayerGroupConfigPanel",
                "imageMosaicStorePanel",
                "JAIMenuPage",
                "layerAccessDataRulePanelInfo",
                "layerGroupMenuPage",
                "layerMenuPage",
                "logsPage",
                "mapHeadingConfigPanel",
                "ndLayerEditTabPanelInfo",
                "postgisDataStorePanel",
                "propertyDataStorePanel",
                "rasterHeadingConfigPanel",
                "rootLayerConfigPanel",
                "serverCategory",
                "ServiceInfoCapabilitiesProvider",
                "serviceLayerConfigurationPanelInfo",
                "servicesCategory",
                "settingsHeadingConfigPanel",
                "shapefileDataStorePanel",
                "shapefileDirectoryDataStorePanel",
                "sidebarNewLayer",
                "sidebarNewLayerGroup",
                "sidebarNewStore",
                "sidebarNewWorkspace",
                "statusMenuPage",
                "storeMenuPage",
                "toolMenuPage",
                "utilitiesCategory",
                "vectorHeadingConfigPanel",
                "webApplication",
                "webCoreExtension",
                "webDispatcherMapping",
                "webDispatcherMappingSystem",
                "welcomeMenuPage",
                "wfsDataStorePanel",
                "wicket",
                "wicketAdminRequestCallback",
                "wicketConfigurationLocker",
                "wicketEnvVariableInjector",
                "workspaceAdminAuthorizer",
                "workspaceMenuPage");

        for (String beanName : expectedBeanNames) {
            assertThat(generatedSource).contains(" %s(".formatted(beanName));
        }
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
