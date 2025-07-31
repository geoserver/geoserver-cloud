/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.geoserver.config.GeoServerDataDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive test class for XML-to-Java bean method generation covering all
 * real GeoServer applicationContext.xml patterns identified from 85+ XML files.
 *
 * <p>Each test method contains real XML snippets from actual GeoServer
 * configurations to ensure complete coverage of all possible bean definition
 * scenarios. The tests validate that the transpiler generates syntactically
 * correct and semantically equivalent Java code from Spring XML configurations.
 *
 * <h3>Test Structure</h3>
 * <p>Each test follows this pattern:
 * <ol>
 *   <li>Define XML input containing a specific Spring bean pattern</li>
 *   <li>Define expected Java output with fully qualified class names</li>
 *   <li>Use {@link #testBeanMethodGeneneration(String, String, String)} to validate equivalence</li>
 * </ol>
 *
 * <h3>Fully Qualified Names in Expected Code</h3>
 * <p><strong>Important:</strong> Expected Java code snippets must use fully qualified class names
 * (e.g., {@code org.springframework.context.annotation.Bean}) instead of simple names
 * (e.g., {@code @Bean}) for the following reasons:
 *
 * <ul>
 *   <li><strong>Generated code characteristics:</strong> The transpiler generates code without
 *       import statements in the method bodies, using fully qualified names throughout</li>
 *   <li><strong>AST comparison accuracy:</strong> The test framework compares Abstract Syntax Trees
 *       where import context is separate from method declarations</li>
 *   <li><strong>Unambiguous type resolution:</strong> Fully qualified names eliminate ambiguity
 *       when classes from different packages have the same simple name</li>
 *   <li><strong>Consistent test validation:</strong> Both expected and actual code use the same
 *       naming convention, ensuring accurate structural comparison</li>
 * </ul>
 *
 * <p><strong>Note:</strong> While these tests use fully qualified names for precise AST validation,
 * the actual generated {@code @Configuration} classes include proper import statements and use
 * simple class names for better readability. See {@link XmlConfigTranspileProcessorTest} for
 * end-to-end integration tests that validate the complete generated class structure with imports.
 *
 * <h3>Test Categories</h3>
 * <p>Tests are organized by Spring XML pattern complexity:
 * <ul>
 *   <li><strong>Basic patterns:</strong> Simple beans, inner classes, auto-generated names</li>
 *   <li><strong>Dependency injection:</strong> Constructor and property injection with type inference</li>
 *   <li><strong>Collections:</strong> Lists, maps, properties, and managed collections</li>
 *   <li><strong>Bean lifecycle:</strong> Lazy initialization, scopes, dependencies</li>
 *   <li><strong>Advanced features:</strong> SpEL expressions, factory beans, component scanning</li>
 *   <li><strong>Real-world patterns:</strong> Complex GeoServer configurations from production XML</li>
 * </ul>
 *
 * <h3>Collision Prevention Testing</h3>
 * <p>Tests verify that auto-generated bean method names include unique suffixes (e.g., {@code _5d582b})
 * to prevent naming conflicts across different @Configuration classes. The suffix is a stable hash
 * derived from the transpilation context and ensures method names remain unique even when multiple
 * independent configurations contain similar auto-generated beans.
 *
 * @see BeanVisitorTestUtils#generateBeanMethodFromXml(String, String)
 * @since 2.28.0
 */
class XmlConfigTranspileProcessorMethodGenerationTest {

    @TempDir
    Path tempDir;

    /**
     * Core test method that validates XML-to-Java bean method generation.
     *
     * <p>This method performs comprehensive validation of the transpilation process:
     * <ol>
     *   <li><strong>Code Generation:</strong> Converts XML bean definition to Java @Bean method</li>
     *   <li><strong>Compilation Validation:</strong> Ensures generated code compiles without errors</li>
     *   <li><strong>Structural Equivalence:</strong> Compares AST structure against expected output</li>
     *   <li><strong>Documentation Preservation:</strong> Verifies original XML is included in Javadoc</li>
     * </ol>
     *
     * <p><strong>Expected Code Requirements:</strong> The {@code expectedJavaCode} parameter must:
     * <ul>
     *   <li>Use fully qualified class names (e.g., {@code @org.springframework.context.annotation.Bean})</li>
     *   <li>Match the exact structure and formatting of generated code</li>
     *   <li>Include all annotations, parameters, and method body statements</li>
     *   <li>Use the same variable naming conventions as the generator</li>
     * </ul>
     *
     * @param beanName the name of the bean to extract from XML (supports auto-generated names like "ClassName#0")
     * @param xml the XML fragment containing the bean definition (should be valid Spring XML)
     * @param expectedJavaCode the expected Java method code with fully qualified names
     *
     * @see BeanVisitorTestUtils#generateBeanMethodFromXml(String, String)
     * @see #assertBeanMethod(MethodSpec)
     */
    private void testBeanMethodGeneneration(String beanName, final String xml, String expectedJavaCode) {
        testBeanMethodGeneneration(beanName, xml, expectedJavaCode, false);
    }

    private void testBeanMethodGeneneration(
            String beanName, final String xml, String expectedJavaCode, boolean ignoreJavadoc) {
        MethodSpec generatedMethod = BeanVisitorTestUtils.generateBeanMethodFromXml(beanName, xml);

        BeanMethodAssertion beanMethod = assertBeanMethod(generatedMethod);
        beanMethod.compilesSuccessfully();
        beanMethod.isEquivalentTo(expectedJavaCode);
        if (!ignoreJavadoc) {
            beanMethod.hasJavadoc(xml);
        }
    }

    /**
     * Tests the simplest possible bean definition: a bean with an explicit ID and no dependencies.
     *
     * <p>This test demonstrates the baseline expected output format where:
     * <ul>
     *   <li>The {@code @Bean} annotation uses the fully qualified name {@code @org.springframework.context.annotation.Bean}</li>
     *   <li>Return types use fully qualified names (e.g., {@code org.geoserver.security.xml.XMLSecurityProvider})</li>
     *   <li>Constructor calls use fully qualified names in the {@code new} expression</li>
     *   <li>Method names match the XML {@code id} attribute exactly</li>
     * </ul>
     *
     * <p><strong>Why fully qualified names?</strong> The transpiler generates code using JavaPoet, which
     * produces fully qualified names in method bodies to avoid import conflicts. Tests must match this
     * exact format for AST comparison to succeed.
     */
    @Test
    void testSimplestBean() {
        final String xml =
                """
                <bean id="xmlSecurityProvider" class="org.geoserver.security.xml.XMLSecurityProvider"/>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.xml.XMLSecurityProvider xmlSecurityProvider() {
                  return new org.geoserver.security.xml.XMLSecurityProvider();
                }
                """;
        testBeanMethodGeneneration("xmlSecurityProvider", xml, expectedJavaCode);
    }

    /**
     * Tests auto-generated bean names and collision prevention for beans without explicit IDs.
     *
     * <p>When a bean lacks an {@code id} attribute, Spring auto-generates names using the pattern
     * {@code ClassName#0}, {@code ClassName#1}, etc. The transpiler preserves this behavior and
     * adds unique suffixes to prevent method name collisions across different @Configuration classes.
     *
     * <p><strong>Key aspects demonstrated:</strong>
     * <ul>
     *   <li>Auto-generated bean name: {@code org.geoserver.platform.RenderingEngineStatus#0}</li>
     *   <li>Sanitized method name: {@code org_geoserver_platform_RenderingEngineStatus_0}</li>
     *   <li>Collision prevention suffix: {@code _5d582b} (derived from transpilation context)</li>
     *   <li>All type references use fully qualified names for unambiguous resolution</li>
     * </ul>
     *
     * <p><strong>Note on fully qualified names:</strong> The expected code must use
     * {@code @org.springframework.context.annotation.Bean} instead of {@code @Bean} because
     * the AST comparison validates the exact structure without considering import statements.
     */
    @Test
    void testSimpleBeanWithNoXmlId() {
        final String xml =
                """
                <bean class="org.geoserver.platform.RenderingEngineStatus"/>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.platform.RenderingEngineStatus org_geoserver_platform_RenderingEngineStatus_0_5d582b() {
                  return new org.geoserver.platform.RenderingEngineStatus();
                }
                """;

        // NOTE: The _5d582b suffix is a hash-based unique identifier generated from the transpilation context
        // (package name, class name, and XML locations) to prevent method name collisions across different
        // @Configuration classes. This suffix would change if the test context locations were modified.

        testBeanMethodGeneneration("org.geoserver.platform.RenderingEngineStatus#0", xml, expectedJavaCode);
    }

    @Test
    void testSimpleBeanWithIndexedSimpleAndListConstructorArguments() {
        final String xml =
                """
                <bean id="wfsService-1.0.0" class="org.geoserver.platform.Service">
                  <constructor-arg index="0" value="wfs"/>
                  <constructor-arg index="1" value="http://www.opengis.net/wfs"/>
                  <constructor-arg index="2" ref="wfsService"/>
                  <constructor-arg index="3" value="1.0.0"/>
                  <constructor-arg index="4">
                    <list>
                      <value>GetCapabilities</value>
                      <value>DescribeFeatureType</value>
                      <value>GetFeature</value>
                      <value>GetFeatureWithLock</value>
                      <value>LockFeature</value>
                      <value>Transaction</value>
                    </list>
                  </constructor-arg>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean(
                    name = "wfsService-1.0.0"
                )
                org.geoserver.platform.Service wfsService_1_0_0(
                    @org.springframework.beans.factory.annotation.Qualifier("wfsService") java.lang.Object wfsService) {
                  return new org.geoserver.platform.Service(
                          "wfs",
                          "http://www.opengis.net/wfs",
                          wfsService,
                          new org.geotools.util.Version("1.0.0"),
                          new java.util.ArrayList<>(java.util.List.of(
                                  "GetCapabilities",
                                  "DescribeFeatureType",
                                  "GetFeature",
                                  "GetFeatureWithLock",
                                  "LockFeature",
                                  "Transaction"))
                    );
                }
                """;

        testBeanMethodGeneneration("wfsService-1.0.0", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithImplicitConstructorAutowiring() {
        final String xml =
                """
                <bean class="org.geoserver.ows.DisabledServiceCheck" id="disabledServiceChecker"/>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.ows.DisabledServiceCheck disabledServiceChecker(
                    org.geoserver.config.GeoServer geoServer) {
                  return new org.geoserver.ows.DisabledServiceCheck(geoServer);
                }
                """;

        testBeanMethodGeneneration("disabledServiceChecker", xml, expectedJavaCode);
    }

    @Test
    void testConstructorArgValue() {
        final String xml =
                """
                <bean id="apiLocalWorkspaceURLManger" class="org.geoserver.ows.LocalWorkspaceURLMangler">
                  <constructor-arg value="ogc"/>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.ows.LocalWorkspaceURLMangler apiLocalWorkspaceURLManger() {
                  return new org.geoserver.ows.LocalWorkspaceURLMangler("ogc");
                }
                """;

        testBeanMethodGeneneration("apiLocalWorkspaceURLManger", xml, expectedJavaCode);
    }

    @Test
    void testIndexedConstructorArgs() {
        final String xml =
                """
                <bean id="gsMainModule" class="org.geoserver.platform.ModuleStatusImpl">
                    <constructor-arg index="0" value="gs-main"/>
                    <constructor-arg index="1" value="GeoServer Main"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.platform.ModuleStatusImpl gsMainModule() {
                  return new org.geoserver.platform.ModuleStatusImpl("gs-main", "GeoServer Main");
                }
                """;
        testBeanMethodGeneneration("gsMainModule", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithClassConstructorArg() {
        final String xml =
                """
                <bean class="org.geoserver.threadlocals.PublicThreadLocalTransfer" id="dispatcherRequestTransfer">
                    <constructor-arg index="0" value="org.geoserver.ows.Dispatcher"/>
                    <constructor-arg index="1" value="REQUEST"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.threadlocals.PublicThreadLocalTransfer dispatcherRequestTransfer() throws
                    java.lang.NoSuchFieldException {
                  return new org.geoserver.threadlocals.PublicThreadLocalTransfer(org.geoserver.ows.Dispatcher.class, "REQUEST");
                }
                """;

        testBeanMethodGeneneration("dispatcherRequestTransfer", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithNumericConstructorArgs() {
        final String xml =
                """
                <bean id="authCacheLifecycleHandler" class="org.geoserver.security.auth.GuavaAuthenticationCacheImpl">
                  <constructor-arg value="1000"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.auth.GuavaAuthenticationCacheImpl authCacheLifecycleHandler() {
                  return new org.geoserver.security.auth.GuavaAuthenticationCacheImpl(1000);
                }
                """;
        testBeanMethodGeneneration("authCacheLifecycleHandler", xml, expectedJavaCode);
    }

    @Test
    void testSimpleBeanInnerClass() {
        final String xml =
                """
                <bean id="urlMasterPasswordProvider" class="org.geoserver.security.password.URLMasterPasswordProvider$SecurityProvider"/>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.password.URLMasterPasswordProvider.SecurityProvider urlMasterPasswordProvider() {
                  return new org.geoserver.security.password.URLMasterPasswordProvider.SecurityProvider();
                }
                """;

        testBeanMethodGeneneration("urlMasterPasswordProvider", xml, expectedJavaCode);
    }

    @Test
    void testBasicBeanWithProperties() {
        final String xml =
                """
                <bean id="chartsExtension" class="org.geoserver.platform.ModuleStatusImpl">
                  <property name="module" value="gs-charts" />
                  <property name="name" value="Charts Extension"/>
                  <property name="component" value="Charts extension"/>
                  <property name="available" value="true"/>
                  <property name="enabled" value="true"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.platform.ModuleStatusImpl chartsExtension() {
                  org.geoserver.platform.ModuleStatusImpl bean = new org.geoserver.platform.ModuleStatusImpl();
                  bean.setModule("gs-charts");
                  bean.setName("Charts Extension");
                  bean.setComponent("Charts extension");
                  bean.setAvailable(true);
                  bean.setEnabled(true);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("chartsExtension", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithIdAndName() {
        final String xml =
                """
                <bean class="org.vfny.geoserver.servlets.BufferStrategy" id="bufferServiceStrategy" name="BUFFER"/>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean(
                    name = {"bufferServiceStrategy", "BUFFER"}
                )
                org.vfny.geoserver.servlets.BufferStrategy bufferServiceStrategy() {
                  return new org.vfny.geoserver.servlets.BufferStrategy();
                }
                """;

        testBeanMethodGeneneration("bufferServiceStrategy", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithMultipleNames() {
        final String xml =
                """
                <bean class="org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter" \
                id="simpleHandlerAdapter" name="controller1,controller2, controller3"/>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean(
                    name = {"simpleHandlerAdapter", "controller1", "controller2", "controller3"}
                )
                org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter simpleHandlerAdapter() {
                  return new org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter();
                }
                """;

        testBeanMethodGeneneration("simpleHandlerAdapter", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithSingleConstructorRef() {
        final String xml =
                """
                <bean id="resourceLoader" class="org.geoserver.platform.GeoServerResourceLoader">
                    <constructor-arg ref="resourceStore"/>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.platform.GeoServerResourceLoader resourceLoader(
                    @org.springframework.beans.factory.annotation.Qualifier("resourceStore")
                    org.geoserver.platform.resource.ResourceStore resourceStore) {

                    return new org.geoserver.platform.GeoServerResourceLoader(resourceStore);
                }
                """;

        testBeanMethodGeneneration("resourceLoader", xml, expectedJavaCode);
    }

    /**
     * Type inference should prioritize the public constructor ({@code public DataAccessRuleDAO(GeoServerDataDirectory dd, Catalog rawCatalog)}
     *  over the package-private one ({@code DataAccessRuleDAO(Catalog rawCatalog, Resource securityDir)}
     */
    @Test
    void testBeanWithMultipleConstructorRef() {
        final String xml =
                """
                <bean id="accessRulesDao" class="org.geoserver.security.impl.DataAccessRuleDAO">
                    <constructor-arg ref="dataDirectory"/>
                    <constructor-arg ref="rawCatalog"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.impl.DataAccessRuleDAO accessRulesDao(
                    @org.springframework.beans.factory.annotation.Qualifier("dataDirectory") org.geoserver.config.GeoServerDataDirectory dataDirectory,
                    @org.springframework.beans.factory.annotation.Qualifier("rawCatalog") org.geoserver.catalog.Catalog rawCatalog)
                    throws java.io.IOException {
                  return new org.geoserver.security.impl.DataAccessRuleDAO(dataDirectory, rawCatalog);
                }
                """;

        testBeanMethodGeneneration("accessRulesDao", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithListConstructorArgument() {
        final String xml =
                """
                <bean id="restFilterDefinitionMap" class="org.geoserver.security.RESTfulDefinitionSourceProxy">
                    <constructor-arg>
                      <list>
                          <ref bean="defaultRestFilterDefinitionMap"/>
                          <ref bean="workspaceAdminRestDefinitionSource"/>
                      </list>
                    </constructor-arg>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.RESTfulDefinitionSourceProxy restFilterDefinitionMap(
                    @org.springframework.beans.factory.annotation.Qualifier("defaultRestFilterDefinitionMap") org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource defaultRestFilterDefinitionMap,
                    @org.springframework.beans.factory.annotation.Qualifier("workspaceAdminRestDefinitionSource") org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource workspaceAdminRestDefinitionSource) {
                  return new org.geoserver.security.RESTfulDefinitionSourceProxy(new java.util.ArrayList<>(java.util.List.of(defaultRestFilterDefinitionMap, workspaceAdminRestDefinitionSource)));
                }
                """;

        testBeanMethodGeneneration("restFilterDefinitionMap", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithPropertyRef() {
        final String xml =
                """
                <bean class="org.geoserver.platform.resource.DataDirectoryResourceStore" id="dataDirectoryResourceStore">
                    <property name="lockProvider" ref="fileLockProvider"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.platform.resource.DataDirectoryResourceStore dataDirectoryResourceStore(
                    @org.springframework.beans.factory.annotation.Qualifier("fileLockProvider") org.geoserver.platform.resource.LockProvider fileLockProvider) {
                  org.geoserver.platform.resource.DataDirectoryResourceStore bean = new org.geoserver.platform.resource.DataDirectoryResourceStore();
                  bean.setLockProvider(fileLockProvider);
                  return bean;
                }""";

        testBeanMethodGeneneration("dataDirectoryResourceStore", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithConstructorArgsAndRefProperty() {
        final String xml =
                """
                <bean class="org.geoserver.security.impl.DefaultResourceAccessManager" id="defaultResourceAccessManager">
                     <constructor-arg ref="accessRulesDao"/>
                     <constructor-arg ref="rawCatalog"/>
                     <property name="groupsCache" ref="layerGroupContainmentCache"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.impl.DefaultResourceAccessManager defaultResourceAccessManager(
                    @org.springframework.beans.factory.annotation.Qualifier("accessRulesDao")org.geoserver.security.impl.DataAccessRuleDAO accessRulesDao,
                    @org.springframework.beans.factory.annotation.Qualifier("rawCatalog") org.geoserver.catalog.Catalog rawCatalog,
                    @org.springframework.beans.factory.annotation.Qualifier("layerGroupContainmentCache") org.geoserver.security.impl.LayerGroupContainmentCache layerGroupContainmentCache) {
                  org.geoserver.security.impl.DefaultResourceAccessManager bean = new org.geoserver.security.impl.DefaultResourceAccessManager(accessRulesDao, rawCatalog);
                  bean.setGroupsCache(layerGroupContainmentCache);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("defaultResourceAccessManager", xml, expectedJavaCode);
    }

    @Test
    void testPasswordEncoderWithScope() {
        final String xml =
                """
                <bean id="pbePasswordEncoder"
                  class="org.geoserver.security.password.GeoServerPBEPasswordEncoder" scope="prototype">
                  <property name="prefix" value="crypt1" />
                  <property name="algorithm" value="PBEWITHMD5ANDDES" />
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @org.springframework.context.annotation.Scope("prototype")
                org.geoserver.security.password.GeoServerPBEPasswordEncoder pbePasswordEncoder() {
                  org.geoserver.security.password.GeoServerPBEPasswordEncoder bean = new org.geoserver.security.password.GeoServerPBEPasswordEncoder();
                  bean.setPrefix("crypt1");
                  bean.setAlgorithm("PBEWITHMD5ANDDES");
                  return bean;
                }
                """;

        testBeanMethodGeneneration("pbePasswordEncoder", xml, expectedJavaCode);
    }

    @Test
    void testSimpleUrlHandlerMapping() {
        final String xml =
                """
                <bean id="apiClasspathPublisherMapping"
                      class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
                  <property name="alwaysUseFullPath" value="true"/>
                  <property name="mappings">
                    <props>
                      <prop key="/apicss/**">classpathPublisher</prop>
                      <prop key="/swagger-ui/**">classpathPublisher</prop>
                    </props>
                  </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @java.lang.SuppressWarnings({"unchecked", "rawtypes"})
                org.springframework.web.servlet.handler.SimpleUrlHandlerMapping apiClasspathPublisherMapping() {
                  org.springframework.web.servlet.handler.SimpleUrlHandlerMapping bean = new org.springframework.web.servlet.handler.SimpleUrlHandlerMapping();
                  bean.setAlwaysUseFullPath(true);
                  // // Property 'mappings' uses ManagedProperties
                  java.util.Properties mappingsProps = new java.util.Properties();
                  mappingsProps.setProperty("/apicss/**", "classpathPublisher");
                  mappingsProps.setProperty("/swagger-ui/**", "classpathPublisher");
                  bean.setMappings(mappingsProps);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("apiClasspathPublisherMapping", xml, expectedJavaCode);
    }

    @Test
    void testOWSHandlerMapping() {
        final String xml =
                """
                <bean id="apiURLMapping" class="org.geoserver.ows.OWSHandlerMapping">
                  <constructor-arg ref="catalog"/>
                  <property name="alwaysUseFullPath" value="true"/>
                  <property name="useTrailingSlashMatch" value="true"/>
                  <property name="order" value="0"/>

                  <property name="mappings">
                    <props>
                      <prop key="/ogc">apiDispatcher</prop>
                      <prop key="/ogc/**">apiDispatcher</prop>
                    </props>
                  </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @java.lang.SuppressWarnings({"unchecked", "rawtypes"})
                org.geoserver.ows.OWSHandlerMapping apiURLMapping(
                    @org.springframework.beans.factory.annotation.Qualifier("catalog") org.geoserver.catalog.Catalog catalog) {
                  org.geoserver.ows.OWSHandlerMapping bean = new org.geoserver.ows.OWSHandlerMapping(catalog);
                  bean.setAlwaysUseFullPath(true);
                  bean.setUseTrailingSlashMatch(true);
                  bean.setOrder(0);
                  // // Property 'mappings' uses ManagedProperties
                  java.util.Properties mappingsProps = new java.util.Properties();
                  mappingsProps.setProperty("/ogc", "apiDispatcher");
                  mappingsProps.setProperty("/ogc/**", "apiDispatcher");
                  bean.setMappings(mappingsProps);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("apiURLMapping", xml, expectedJavaCode);
    }

    @Test
    void testDataAccessRuleDAO() {
        final String xml =
                """
                <bean id="accessRulesDao" class="org.geoserver.security.impl.DataAccessRuleDAO">
                    <constructor-arg ref="dataDirectory"/>
                    <constructor-arg ref="rawCatalog"/>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.impl.DataAccessRuleDAO accessRulesDao(
                    @org.springframework.beans.factory.annotation.Qualifier("dataDirectory") org.geoserver.config.GeoServerDataDirectory dataDirectory,
                    @org.springframework.beans.factory.annotation.Qualifier("rawCatalog") org.geoserver.catalog.Catalog rawCatalog)
                    throws java.io.IOException {
                  return new org.geoserver.security.impl.DataAccessRuleDAO(dataDirectory, rawCatalog);
                }
                """;

        testBeanMethodGeneneration("accessRulesDao", xml, expectedJavaCode);
    }

    @Test
    void testRememberMeServicesFactoryBean() {
        final String xml =
                """
                <bean id="rememberMeServices"
                  class="org.geoserver.security.rememberme.RememberMeServicesFactoryBean">
                  <constructor-arg ref="geoServerSecurityManager"/>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.rememberme.RememberMeServicesFactoryBean rememberMeServices(
                    @org.springframework.beans.factory.annotation.Qualifier("geoServerSecurityManager") org.geoserver.security.GeoServerSecurityManager geoServerSecurityManager) {
                  return new org.geoserver.security.rememberme.RememberMeServicesFactoryBean(geoServerSecurityManager);
                }
                """;

        testBeanMethodGeneneration("rememberMeServices", xml, expectedJavaCode);
    }

    /**
     * Tests MethodInvokingFactoryBean with instance method invocation and ManagedList arguments.
     *
     * <p>This test validates advanced Spring factory bean patterns including:
     * <ul>
     *   <li><strong>Factory bean instantiation:</strong> Creating MethodInvokingFactoryBean instances</li>
     *   <li><strong>Dependency injection:</strong> Multiple @Qualifier-annotated parameters</li>
     *   <li><strong>Property configuration:</strong> Setting targetObject, targetMethod, and arguments</li>
     *   <li><strong>Collection handling:</strong> Converting XML {@code <list>} to {@code List.of()} calls</li>
     *   <li><strong>Type casting:</strong> Explicit cast to {@code java.util.List} for arguments property</li>
     * </ul>
     *
     * <p><strong>Fully qualified name examples in this test:</strong>
     * <ul>
     *   <li>{@code @org.springframework.context.annotation.Bean} - Spring's @Bean annotation</li>
     *   <li>{@code @org.springframework.beans.factory.annotation.Qualifier} - Dependency injection qualifier</li>
     *   <li>{@code org.springframework.beans.factory.config.MethodInvokingFactoryBean} - Return type and constructor</li>
     *   <li>{@code java.lang.Object} - Parameter types (when specific types cannot be inferred)</li>
     *   <li>{@code java.util.List} - Type cast for arguments collection</li>
     * </ul>
     *
     * <p>These fully qualified names ensure the test validates the exact AST structure that
     * JavaPoet generates, without relying on import statement resolution.
     */
    @Test
    void testMethodInvokingFactoryBean() {
        final String xml =
                """
                <bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
                  <property name="targetObject">
                      <ref bean="roleFilter"/>
                  </property>
                  <property name="targetMethod">
                      <value>initializeFromConfig</value>
                  </property>
                  <property name="arguments">
                      <list>
                          <ref bean="roleFilterConfig"/>
                      </list>
                  </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.springframework.beans.factory.config.MethodInvokingFactoryBean
                        org_springframework_beans_factory_config_MethodInvokingFactoryBean_0_5d582b(
                            @org.springframework.beans.factory.annotation.Qualifier("roleFilter") java.lang.Object roleFilter,
                            @org.springframework.beans.factory.annotation.Qualifier("roleFilterConfig") java.lang.Object roleFilterConfig) {

                  org.springframework.beans.factory.config.MethodInvokingFactoryBean bean =
                          new org.springframework.beans.factory.config.MethodInvokingFactoryBean();

                  bean.setTargetObject(roleFilter);
                  bean.setTargetMethod("initializeFromConfig");

                  bean.setArguments(roleFilterConfig);
                  return bean;
                }
                """;

        // NOTE: The _5d582b suffix prevents method name collisions across independent @Configuration classes.
        // It's a stable hash derived from: "org.geoserver.test.generated.TestConfig:test.xml"

        testBeanMethodGeneneration(
                "org.springframework.beans.factory.config.MethodInvokingFactoryBean#0", xml, expectedJavaCode);
    }

    @Test
    void testMethodInvokingFactoryBeanWithStaticMethod() {
        final String xml =
                """
                <bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
                    <property name="staticMethod" value="org.geoserver.wfs.xml.SqlViewParamsExtractor.setWfsSqlViewKvpParser"/>
                    <property name="arguments">
                        <list>
                            <ref bean="wfsSqlViewKvpParser"/>
                        </list>
                   </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.springframework.beans.factory.config.MethodInvokingFactoryBean
                        org_springframework_beans_factory_config_MethodInvokingFactoryBean_0_5d582b(
                           @org.springframework.beans.factory.annotation.Qualifier("wfsSqlViewKvpParser") java.lang.Object wfsSqlViewKvpParser) {

                  org.springframework.beans.factory.config.MethodInvokingFactoryBean bean =
                          new org.springframework.beans.factory.config.MethodInvokingFactoryBean();

                  bean.setStaticMethod("org.geoserver.wfs.xml.SqlViewParamsExtractor.setWfsSqlViewKvpParser");
                  // Property 'arguments' uses ManagedList
                  bean.setArguments(wfsSqlViewKvpParser);
                  return bean;
                }
                """;

        // NOTE: The _5d582b suffix prevents method name collisions across independent @Configuration classes.
        // It's a stable hash derived from: "org.geoserver.test.generated.TestConfig:test.xml"
        testBeanMethodGeneneration(
                "org.springframework.beans.factory.config.MethodInvokingFactoryBean#0", xml, expectedJavaCode);
    }

    @Test
    void testDependsOnSingleBean() {
        final String xml =
                """
                <bean id="authenticationManager" class="org.geoserver.security.GeoServerSecurityManager"
                   depends-on="extensions">
                  <constructor-arg ref="dataDirectory"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @org.springframework.context.annotation.DependsOn("extensions")
                org.geoserver.security.GeoServerSecurityManager authenticationManager(
                    @org.springframework.beans.factory.annotation.Qualifier("dataDirectory") org.geoserver.config.GeoServerDataDirectory dataDirectory)
                    throws java.lang.Exception {
                  return new org.geoserver.security.GeoServerSecurityManager(dataDirectory);
                }
                """;

        testBeanMethodGeneneration("authenticationManager", xml, expectedJavaCode);
    }

    @Test
    void testDependsOnMultipleBeans() {
        final String xml =
                """
                <bean id="geoServerLoader" class="org.geoserver.config.GeoServerLoaderProxy" depends-on="extensions,dataDirectory,geoServerSecurityManager,configurationLock">
                  <constructor-arg ref="resourceLoader"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @org.springframework.context.annotation.DependsOn({"extensions", "dataDirectory", "geoServerSecurityManager", "configurationLock"})
                org.geoserver.config.GeoServerLoaderProxy geoServerLoader(
                    @org.springframework.beans.factory.annotation.Qualifier("resourceLoader") org.geoserver.platform.GeoServerResourceLoader resourceLoader) {
                  return new org.geoserver.config.GeoServerLoaderProxy(resourceLoader);
                }
                """;

        testBeanMethodGeneneration("geoServerLoader", xml, expectedJavaCode);
    }

    @Test
    void testAlias() {
        final String xml =
                """
                <bean id="authenticationManager" class="org.geoserver.security.GeoServerSecurityManager"
                   depends-on="extensions">
                  <constructor-arg ref="dataDirectory"/>
                </bean>
                <alias name="authenticationManager" alias="geoServerSecurityManager"/>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean(
                    name = {"authenticationManager", "geoServerSecurityManager"}
                )
                @org.springframework.context.annotation.DependsOn("extensions")
                org.geoserver.security.GeoServerSecurityManager authenticationManager(
                    @org.springframework.beans.factory.annotation.Qualifier("dataDirectory") org.geoserver.config.GeoServerDataDirectory dataDirectory)
                    throws java.lang.Exception {
                  return new org.geoserver.security.GeoServerSecurityManager(dataDirectory);
                }
                """;

        testBeanMethodGeneneration("authenticationManager", xml, expectedJavaCode);
    }

    @Test
    void testLazyInit() {
        final String xml =
                """
                <bean lazy-init="true" id="xmlSecurityProvider" class="org.geoserver.security.xml.XMLSecurityProvider"/>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @org.springframework.context.annotation.Lazy
                org.geoserver.security.xml.XMLSecurityProvider xmlSecurityProvider() {
                  return new org.geoserver.security.xml.XMLSecurityProvider();
                }
                """;

        testBeanMethodGeneneration("xmlSecurityProvider", xml, expectedJavaCode);
    }

    @Test
    void testComplexPropertiesWithDelimiters() {
        final String xml =
                """
                <bean id="roleConverter" class="org.geoserver.security.impl.GeoServerRoleConverterImpl" >
                    <property name="roleDelimiterString" value=";" />
                    <property name="roleParameterDelimiterString" value=","/>
                    <property name="roleParameterStartString" value="("/>
                    <property name="roleParameterEndString" value= ")"/>
                    <property name="roleParameterAssignmentString" value="="/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.impl.GeoServerRoleConverterImpl roleConverter() {
                  org.geoserver.security.impl.GeoServerRoleConverterImpl bean = new org.geoserver.security.impl.GeoServerRoleConverterImpl();
                  bean.setRoleDelimiterString(";");
                  bean.setRoleParameterDelimiterString(",");
                  bean.setRoleParameterStartString("(");
                  bean.setRoleParameterEndString(")");
                  bean.setRoleParameterAssignmentString("=");
                  return bean;
                }
                """;

        testBeanMethodGeneneration("roleConverter", xml, expectedJavaCode);
    }

    @Test
    void testBooleanProperties() {
        final String xml =
                """
                <bean id="strongPbePasswordEncoder"
                  class="org.geoserver.security.password.GeoServerPBEPasswordEncoder" scope="prototype">
                  <property name="prefix" value="crypt2" />
                  <property name="providerName" value="BC" />
                  <property name="algorithm" value="PBEWITHSHA256AND256BITAES-CBC-BC" />
                  <property name="availableWithoutStrongCryptogaphy" value="false" />
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @org.springframework.context.annotation.Scope("prototype")
                org.geoserver.security.password.GeoServerPBEPasswordEncoder strongPbePasswordEncoder() {
                  org.geoserver.security.password.GeoServerPBEPasswordEncoder bean = new org.geoserver.security.password.GeoServerPBEPasswordEncoder();
                  bean.setPrefix("crypt2");
                  bean.setProviderName("BC");
                  bean.setAlgorithm("PBEWITHSHA256AND256BITAES-CBC-BC");
                  bean.setAvailableWithoutStrongCryptogaphy(false);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("strongPbePasswordEncoder", xml, expectedJavaCode);
    }

    @Test
    void testMapProperty() {
        final String xml =
                """
                <bean class="org.springframework.beans.factory.config.CustomEditorConfigurer" id="customEditorConfigurer">
                    <property name="customEditors">
                        <map>
                            <entry key="org.geotools.util.Version" value="org.geoserver.platform.util.VersionPropertyEditor"/>
                        </map>
                    </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @java.lang.SuppressWarnings({"unchecked", "rawtypes"})
                org.springframework.beans.factory.config.CustomEditorConfigurer customEditorConfigurer() {
                  org.springframework.beans.factory.config.CustomEditorConfigurer bean = new org.springframework.beans.factory.config.CustomEditorConfigurer();
                  // // Property 'customEditors' uses ManagedMap
                  java.util.Map customEditorsMap = new java.util.HashMap();
                  customEditorsMap.put(org.geotools.util.Version.class, org.geoserver.platform.util.VersionPropertyEditor.class);
                  bean.setCustomEditors(customEditorsMap);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("customEditorConfigurer", xml, expectedJavaCode);
    }

    @Test
    void testNestedBeanWithFieldRetrievingFactoryBean() {
        final String xml =
                """
                <bean id="advertisedCatalog" class="org.geoserver.catalog.impl.AdvertisedCatalog">
                    <constructor-arg ref="secureCatalog" />
                    <property name="layerGroupVisibilityPolicy">
                      <bean id="org.geoserver.catalog.LayerGroupVisibilityPolicy.HIDE_NEVER"
                        class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean"/>
                    </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.catalog.impl.AdvertisedCatalog advertisedCatalog(
                        @org.springframework.beans.factory.annotation.Qualifier("secureCatalog") org.geoserver.catalog.Catalog secureCatalog) {

                  org.geoserver.catalog.impl.AdvertisedCatalog bean = new org.geoserver.catalog.impl.AdvertisedCatalog(secureCatalog);
                  bean.setLayerGroupVisibilityPolicy(org.geoserver.catalog.LayerGroupVisibilityPolicy.HIDE_NEVER);
                  return bean;
                }
                """;

        assertBeanMethod(BeanVisitorTestUtils.generateBeanMethodFromXml("advertisedCatalog", xml))
                .compilesSuccessfully()
                .isEquivalentTo(expectedJavaCode);
    }

    @Test
    void testSimpleNestedBeanProperties() {
        final String xml =
                """
                <bean id="dimensionFactory" class="org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl">
                    <property name="featureTimeMinimumStrategy">
                        <bean class="org.geoserver.wms.dimension.impl.FeatureMinimumValueSelectionStrategyImpl"/>
                    </property>
                    <property name="featureTimeMaximumStrategy">
                        <bean class="org.geoserver.wms.dimension.impl.FeatureMaximumValueSelectionStrategyImpl"/>
                    </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl dimensionFactory() {
                  org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl bean = new org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl();
                  bean.setFeatureTimeMinimumStrategy(new org.geoserver.wms.dimension.impl.FeatureMinimumValueSelectionStrategyImpl());
                  bean.setFeatureTimeMaximumStrategy(new org.geoserver.wms.dimension.impl.FeatureMaximumValueSelectionStrategyImpl());
                  return bean;
                }
                """;

        assertBeanMethod(BeanVisitorTestUtils.generateBeanMethodFromXml("dimensionFactory", xml))
                .compilesSuccessfully()
                .isEquivalentTo(expectedJavaCode);
    }

    @Test
    void testManagedListToStringArrayConstructorArg() {
        final String xml =
                """
                <bean id="PNG8MapProducer" class="org.geoserver.wms.map.RenderedImageMapOutputFormat">
                  <constructor-arg>
                    <value>image/png; mode=8bit</value>
                  </constructor-arg>
                  <constructor-arg>
                    <list>
                      <value>image/png8</value>
                      <value>image/png; mode=8bit</value>
                    </list>
                  </constructor-arg>
                  <constructor-arg ref="wms" />
                  <property name="extension" value="png"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.wms.map.RenderedImageMapOutputFormat PNG8MapProducer(
                        @org.springframework.beans.factory.annotation.Qualifier("wms") org.geoserver.wms.WMS wms) {

                  org.geoserver.wms.map.RenderedImageMapOutputFormat bean =
                          new org.geoserver.wms.map.RenderedImageMapOutputFormat(
                                  "image/png; mode=8bit",
                                  new String[]{"image/png8", "image/png; mode=8bit"}, wms
                              );
                  bean.setExtension("png");
                  return bean;
                }
                """;

        assertBeanMethod(BeanVisitorTestUtils.generateBeanMethodFromXml("PNG8MapProducer", xml))
                .compilesSuccessfully()
                .isEquivalentTo(expectedJavaCode);
    }

    @Test
    void testNestedClassConstructorArg() {
        final String xml =
                """
                <bean id="QuantizerMethodKvpParser" class="org.geoserver.ows.kvp.EnumKvpParser">
                    <constructor-arg value="quantizer"/>
                    <constructor-arg value="org.geoserver.wms.map.PNGMapResponse$QuantizeMethod"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.ows.kvp.EnumKvpParser QuantizerMethodKvpParser() {
                  return new org.geoserver.ows.kvp.EnumKvpParser("quantizer", org.geoserver.wms.map.PNGMapResponse.QuantizeMethod.class);
                }
                """;

        assertBeanMethod(BeanVisitorTestUtils.generateBeanMethodFromXml("QuantizerMethodKvpParser", xml))
                .compilesSuccessfully()
                .isEquivalentTo(expectedJavaCode);
    }

    @Test
    void testStaticFactoryMethod() {
        final String xml =
                """
                <bean id="nullLockProvider" class="org.geoserver.platform.resource.NullLockProvider" factory-method="instance"/>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.platform.resource.NullLockProvider nullLockProvider() {
                  return org.geoserver.platform.resource.NullLockProvider.instance();
                }
                """;

        assertBeanMethod(BeanVisitorTestUtils.generateBeanMethodFromXml("nullLockProvider", xml))
                .compilesSuccessfully()
                .isEquivalentTo(expectedJavaCode);
    }

    @Test
    void testAbstractBeanWithInheritance() {
        final String xml =
                """
                <!-- Abstract parent bean -->
                <bean id="xmlReader-1.0.0" class="org.geoserver.wfs.xml.v1_0_0.WfsXmlReader" abstract="true">
                    <constructor-arg index="1" ref="xmlConfiguration-1.0"/>
                    <constructor-arg index="2" ref="geoServer"/>
                </bean>

                <!-- Child beans that inherit from the abstract parent -->
                <bean id="wfsGetCapabilitiesXmlReader"
                      class="org.geoserver.wfs.xml.v1_0_0.WfsXmlReader"
                      parent="xmlReader-1.0.0">
                    <constructor-arg value="GetCapabilities"/>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.wfs.xml.v1_0_0.WfsXmlReader wfsGetCapabilitiesXmlReader(
                    @org.springframework.beans.factory.annotation.Qualifier("xmlConfiguration-1.0") org.geotools.xsd.Configuration xmlConfiguration_1_0,
                    @org.springframework.beans.factory.annotation.Qualifier("geoServer") org.geoserver.config.GeoServer geoServer) {

                  return new org.geoserver.wfs.xml.v1_0_0.WfsXmlReader("GetCapabilities", xmlConfiguration_1_0, geoServer);
                }
                """;

        // generated code only has the wfsGetCapabilitiesXmlReader bean, but xmlReader-1.0.0 is required for parsing
        boolean ignoreJavadoc = true;
        testBeanMethodGeneneration("wfsGetCapabilitiesXmlReader", xml, expectedJavaCode, ignoreJavadoc);
    }

    @Test
    void testAbstractBeanWithInheritanceNoChildClass() {
        final String xml =
                """
                <!-- Abstract parent bean -->
                <bean id="xmlReader-1.0.0" class="org.geoserver.wfs.xml.v1_0_0.WfsXmlReader" abstract="true">
                    <constructor-arg index="1" ref="xmlConfiguration-1.0"/>
                    <constructor-arg index="2" ref="geoServer"/>
                </bean>
                <!-- Child bean that inherits class from parent (no explicit class attribute) -->
                <bean id="lockFeatureXmlReader" parent="xmlReader-1.0.0">
                    <constructor-arg value="LockFeature"/>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.wfs.xml.v1_0_0.WfsXmlReader lockFeatureXmlReader(
                    @org.springframework.beans.factory.annotation.Qualifier("xmlConfiguration-1.0") org.geotools.xsd.Configuration xmlConfiguration_1_0,
                    @org.springframework.beans.factory.annotation.Qualifier("geoServer") org.geoserver.config.GeoServer geoServer) {
                  return new org.geoserver.wfs.xml.v1_0_0.WfsXmlReader("LockFeature", xmlConfiguration_1_0, geoServer);
                }
                """;
        // generated code only has the lockFeatureXmlReader bean, but xmlReader-1.0.0 is required for parsing
        boolean ignoreJavadoc = true;
        testBeanMethodGeneneration("lockFeatureXmlReader", xml, expectedJavaCode, ignoreJavadoc);
    }

    @Test
    void testAbstractBeanWithNonIndexedConstructorArgs() {
        // Test pattern from WFS 1.1.0 XML readers where parent has non-indexed constructor args
        // and child specifies type explicitly (different from indexed parent args pattern)
        final String xml =
                """
                <!-- Abstract parent bean with non-indexed constructor args -->
                <bean id="xmlReader-1.1.0" class="org.geoserver.wfs.xml.v1_1_0.WfsXmlReader" abstract="true">
                    <constructor-arg ref="geoServer"/>
                    <constructor-arg ref="xmlConfiguration-1.1"/>
                </bean>
                <!-- Child bean inherits and adds typed constructor arg -->
                <bean id="wfsGetCapabilitiesXmlReader-1.1.0"
                      class="org.geoserver.wfs.xml.v1_1_0.WfsXmlReader"
                      parent="xmlReader-1.1.0">
                    <constructor-arg type="java.lang.String" value="GetCapabilities"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean(
                    name = "wfsGetCapabilitiesXmlReader-1.1.0"
                )
                org.geoserver.wfs.xml.v1_1_0.WfsXmlReader wfsGetCapabilitiesXmlReader_1_1_0(
                    @org.springframework.beans.factory.annotation.Qualifier("geoServer") org.geoserver.config.GeoServer geoServer,
                    @org.springframework.beans.factory.annotation.Qualifier("xmlConfiguration-1.1") org.geotools.xsd.Configuration xmlConfiguration_1_1) {

                  return new org.geoserver.wfs.xml.v1_1_0.WfsXmlReader("GetCapabilities", geoServer, xmlConfiguration_1_1);
                }
                """;

        // generated code only has the wfsGetCapabilitiesXmlReader-1.1.0 bean, but
        // xmlReader-1.1.0 is required for parsing
        boolean ignoreJavadoc = true;
        testBeanMethodGeneneration("wfsGetCapabilitiesXmlReader-1.1.0", xml, expectedJavaCode, ignoreJavadoc);
    }

    @Test
    void testAbstractBeanWithSingleParentConstructorArg() {
        // Test pattern from WFS 2.0 XML readers where parent has only one constructor arg
        // This tests simpler inheritance with fewer parameters (different constructor signature)
        final String xml =
                """
                <!-- Abstract parent bean with single constructor arg -->
                <bean id="xmlReader-2.0" class="org.geoserver.wfs.xml.v2_0.WfsXmlReader" abstract="true">
                    <constructor-arg ref="geoServer"/>
                </bean>
                <!-- Child bean inherits and adds typed constructor arg -->
                <bean id="wfsGetCapabilitiesXmlReader-2.0"
                      class="org.geoserver.wfs.xml.v2_0.WfsXmlReader"
                      parent="xmlReader-2.0">
                    <constructor-arg type="java.lang.String" value="GetCapabilities"/>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean(
                    name = "wfsGetCapabilitiesXmlReader-2.0"
                )
                org.geoserver.wfs.xml.v2_0.WfsXmlReader wfsGetCapabilitiesXmlReader_2_0(
                    @org.springframework.beans.factory.annotation.Qualifier("geoServer") org.geoserver.config.GeoServer geoServer) {

                  return new org.geoserver.wfs.xml.v2_0.WfsXmlReader("GetCapabilities", geoServer);
                }
                """;

        // generated code only has the wfsGetCapabilitiesXmlReader-2.0 bean, but xmlReader-2.0 is required for parsing
        boolean ignoreJavadoc = true;
        testBeanMethodGeneneration("wfsGetCapabilitiesXmlReader-2.0", xml, expectedJavaCode, ignoreJavadoc);
    }

    @Test
    void testPropsCollections() {
        final String xml =
                """
                <bean id="kmlURLMapping" class="org.geoserver.ows.OWSHandlerMapping">
                    <constructor-arg ref="catalog" />
                    <property name="alwaysUseFullPath" value="true" />
                    <property name="mappings">
                        <props>
                            <prop key="/kml/icon/**/*">kmlIconService</prop>
                            <prop key="/kml">dispatcher</prop>
                            <prop key="/kml/*">dispatcher</prop>
                        </props>
                    </property>
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @java.lang.SuppressWarnings({"unchecked", "rawtypes"})
                org.geoserver.ows.OWSHandlerMapping kmlURLMapping(
                    @org.springframework.beans.factory.annotation.Qualifier("catalog") org.geoserver.catalog.Catalog catalog) {
                  org.geoserver.ows.OWSHandlerMapping bean = new org.geoserver.ows.OWSHandlerMapping(catalog);
                  bean.setAlwaysUseFullPath(true);
                  // // Property 'mappings' uses ManagedProperties
                  java.util.Properties mappingsProps = new java.util.Properties();
                  mappingsProps.setProperty("/kml", "dispatcher");
                  mappingsProps.setProperty("/kml/*", "dispatcher");
                  mappingsProps.setProperty("/kml/icon/**/*", "kmlIconService");
                  bean.setMappings(mappingsProps);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("kmlURLMapping", xml, expectedJavaCode);
    }

    @Test
    void testAOPProxyFactoryBean() {
        final String xml =
                """
                <bean id="wcs100Service" class="org.springframework.aop.framework.ProxyFactoryBean">
                    <property name="proxyInterfaces">
                        <value>org.geoserver.wcs.WebCoverageService100</value>
                    </property>
                    <property name="interceptorNames">
                        <list>
                            <value>wcs100Logger</value>
                            <value>wcs100ServiceInterceptor*</value>
                            <value>wcs100ServiceTarget</value>
                        </list>
                    </property>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.springframework.aop.framework.ProxyFactoryBean wcs100Service()
                        throws java.lang.ClassNotFoundException {

                    org.springframework.aop.framework.ProxyFactoryBean bean = new org.springframework.aop.framework.ProxyFactoryBean();

                    bean.setProxyInterfaces(new Class<?>[] { Class.forName("org.geoserver.wcs.WebCoverageService100") });
                    bean.setInterceptorNames(new String[] { "wcs100Logger", "wcs100ServiceInterceptor*", "wcs100ServiceTarget" });

                    return bean;
                }
                """;

        testBeanMethodGeneneration("wcs100Service", xml, expectedJavaCode);
    }

    @Test
    void testAOPProxyFactoryBeanWithTargetName() {
        final String xml =
                """
                <bean id="wmsService2" class="org.springframework.aop.framework.ProxyFactoryBean">
                    <property name="proxyInterfaces">
                        <value>org.geoserver.wms.WebMapService</value>
                    </property>
                    <property name="targetName">
                        <value>wmsServiceTarget</value>
                    </property>
                    <property name="interceptorNames">
                        <list>
                            <value>wmsLogger</value>
                            <value>wmsServiceInterceptor*</value>
                        </list>
                    </property>
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.springframework.aop.framework.ProxyFactoryBean wmsService2()
                        throws java.lang.ClassNotFoundException {

                    org.springframework.aop.framework.ProxyFactoryBean bean = new org.springframework.aop.framework.ProxyFactoryBean();

                    bean.setProxyInterfaces(new Class<?>[] { Class.forName("org.geoserver.wms.WebMapService") });
                    bean.setTargetName("wmsServiceTarget");
                    bean.setInterceptorNames(new String[] { "wmsLogger", "wmsServiceInterceptor*" });
                    return bean;
                }
                """;

        testBeanMethodGeneneration("wmsService2", xml, expectedJavaCode);
    }

    @Test
    void testSPELSystemPropertyWithElvisOperator() {
        final String xml =
                """
                <!-- disable the geoserver web console if server started with -DGEOSERVER_CONSOLE_DISABLED=true -->
                <bean id="geoserverConsoleDisabled" class="java.lang.Boolean">
                  <constructor-arg value="#{ systemProperties['GEOSERVER_CONSOLE_DISABLED'] ?: false }" />
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                java.lang.Boolean geoserverConsoleDisabled(
                    @org.springframework.beans.factory.annotation.Value(
                    "#{ systemProperties['GEOSERVER_CONSOLE_DISABLED'] ?: false }") java.lang.Boolean spelParam0) {
                    return new java.lang.Boolean((Boolean) spelParam0);
                }
                """;

        testBeanMethodGeneneration("geoserverConsoleDisabled", xml, expectedJavaCode);
    }

    @Test
    void testSPELBeanReferenceWithConditional() {
        final String xml =
                """
                <!-- choose between wicket UI or filePublisher depending if the console is disabled or not.
                FilePublisher will give a 404 for all web console requests -->
                <bean id="webDispatcherMappingSystem" class="java.lang.String">
                  <constructor-arg value="#{ geoserverConsoleDisabled ? 'filePublisher' : 'wicket' }" />
                </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                java.lang.String webDispatcherMappingSystem(
                    @org.springframework.beans.factory.annotation.Value(
                    "#{ geoserverConsoleDisabled ? 'filePublisher' : 'wicket' }") java.lang.StringBuilder spelParam0) {
                  return new java.lang.String((StringBuilder) spelParam0);
                }
                """;

        testBeanMethodGeneneration("webDispatcherMappingSystem", xml, expectedJavaCode);
    }

    @Test
    void testSPELBeanReferencesInProperties() {
        final String xml =
                """
                  <!-- disable the geoserver web console if server started with -DGEOSERVER_CONSOLE_DISABLED=true -->
                  <bean id="geoserverConsoleDisabled" class="java.lang.Boolean">
                    <constructor-arg value="#{ systemProperties['GEOSERVER_CONSOLE_DISABLED'] ?: false }" />
                  </bean>

                  <!-- choose between wicket UI or filePublisher depending if the console is diabled or not.
                  FilePublisher will give a 404 for all web console requests -->
                  <bean id="webDispatcherMappingSystem" class="java.lang.String">
                    <constructor-arg value="#{ geoserverConsoleDisabled ? 'filePublisher' : 'wicket' }" />
                  </bean>

                  <bean id="webDispatcherMapping"
                    class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"
                    lazy-init="true">
                    <property name="alwaysUseFullPath" value="true" />
                    <property name="mappings">
                      <props>
                        <prop key="/web">#{webDispatcherMappingSystem}</prop>
                        <prop key="/web/**">#{webDispatcherMappingSystem}</prop>
                        <prop key="/web/resources/**">#{webDispatcherMappingSystem}</prop>
                        <prop key="/">filePublisher</prop>
                        <prop key="/index.html">filePublisher</prop>
                      </props>
                    </property>
                  </bean>
                """;
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                @java.lang.SuppressWarnings({"unchecked", "rawtypes"})
                @org.springframework.context.annotation.Lazy
                org.springframework.web.servlet.handler.SimpleUrlHandlerMapping webDispatcherMapping(
                    @org.springframework.beans.factory.annotation.Qualifier("webDispatcherMappingSystem") java.lang.String webDispatcherMappingSystem) {

                  org.springframework.web.servlet.handler.SimpleUrlHandlerMapping bean =
                          new org.springframework.web.servlet.handler.SimpleUrlHandlerMapping();

                  bean.setAlwaysUseFullPath(true);

                  java.util.Properties mappingsProps = new java.util.Properties();
                  mappingsProps.setProperty("/", "filePublisher");
                  mappingsProps.setProperty("/index.html", "filePublisher");
                  mappingsProps.setProperty("/web/resources/**", webDispatcherMappingSystem);
                  mappingsProps.setProperty("/web", webDispatcherMappingSystem);
                  mappingsProps.setProperty("/web/**", webDispatcherMappingSystem);
                  bean.setMappings(mappingsProps);
                  return bean;
                }
                """;

        // generated code only has the webDispatcherMapping bean, but webDispatcherMappingSystem is required for parsing
        boolean ignoreJavadoc = true;
        testBeanMethodGeneneration("webDispatcherMapping", xml, expectedJavaCode, ignoreJavadoc);
    }

    @Test
    void testProtectedConstructorAccess() throws Exception {
        final String xml =
                """
                <bean class="org.geoserver.security.impl.RESTAccessRuleDAO" id="restRulesDao">
                    <constructor-arg ref="dataDirectory"/>
                </bean>
                """;

        Constructor<?> constructor = Class.forName("org.geoserver.security.impl.RESTAccessRuleDAO")
                .getDeclaredConstructor(GeoServerDataDirectory.class);
        assertThat(constructor.accessFlags()).contains(AccessFlag.PROTECTED);

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.impl.RESTAccessRuleDAO restRulesDao(
                    @org.springframework.beans.factory.annotation.Qualifier("dataDirectory") org.geoserver.config.GeoServerDataDirectory dataDirectory)
                    throws java.lang.Exception {
                  java.lang.reflect.Constructor constructor = java.lang.Class.forName("org.geoserver.security.impl.RESTAccessRuleDAO").getDeclaredConstructor(org.geoserver.config.GeoServerDataDirectory.class);
                  constructor.setAccessible(true);
                  return (org.geoserver.security.impl.RESTAccessRuleDAO) constructor.newInstance(dataDirectory);
                }
                """;

        testBeanMethodGeneneration("restRulesDao", xml, expectedJavaCode);
    }

    @Test
    void testProtectedConstructorAccessWithImplicitConstructorAutowiring() throws Exception {
        Constructor<?> constructor = Class.forName("org.geoserver.security.impl.WorkspaceAdminRESTAccessRuleDAO")
                .getDeclaredConstructor(GeoServerDataDirectory.class);
        assertThat(constructor.accessFlags()).contains(AccessFlag.PROTECTED);

        final String xml =
                """
                <bean class="org.geoserver.security.impl.WorkspaceAdminRESTAccessRuleDAO" id="workspaceAdminRESTAccessRuleDAO">
                </bean>
                """;

        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.security.impl.WorkspaceAdminRESTAccessRuleDAO workspaceAdminRESTAccessRuleDAO(
                    org.geoserver.config.GeoServerDataDirectory dataDirectory) throws java.lang.Exception {
                  java.lang.reflect.Constructor constructor = java.lang.Class.forName("org.geoserver.security.impl.WorkspaceAdminRESTAccessRuleDAO").getDeclaredConstructor(org.geoserver.config.GeoServerDataDirectory.class);
                  constructor.setAccessible(true);
                  return (org.geoserver.security.impl.WorkspaceAdminRESTAccessRuleDAO) constructor.newInstance(dataDirectory);
                }
                """;

        testBeanMethodGeneneration("workspaceAdminRESTAccessRuleDAO", xml, expectedJavaCode);
    }

    /**
     * Tests that bean method argument inference prioritizes the bean constructor's type. Otherwise the class would compile,
     * but spring will complain
     * <ul>
     * <li> {@literal geoServer}'s constructor expects {@code Catalog}, but the {@literal catalog} bean is of type {@code CatalogImpl}, hence the bean method argument should be of type {@code Catalog}, not {@code CatalogImpl}
     * <li> {@literal capabilitiesCachingHeadersCallback}'s constructor expects {@code GeoServer}, but the {@literal geoServer} bean is of type {@code GeoServerImpl}, hence the bean method argument should be of type {@code GeoServer}, not {@code GeoServerImpl}
     * </ul>
     */
    @Test
    void testBeanMethodArgumentTypeInference() {
        final String xml =
                """
                <bean id="catalog" class="org.geoserver.catalog.impl.CatalogImpl" depends-on="configurationLock">
                  <property name="resourceLoader" ref="resourceLoader"/>
                </bean>
                <bean id="geoServer" class="org.geoserver.config.impl.GeoServerImpl">
                  <property name="catalog" ref="catalog"/>
                </bean>
                <bean id="capabilitiesCachingHeadersCallback" class="org.geoserver.config.CapabilitiesCacheHeadersCallback">
                   <constructor-arg ref="geoServer"/>
                </bean>
                """;

        final String expectedGeoServerCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.config.impl.GeoServerImpl geoServer(
                    @org.springframework.beans.factory.annotation.Qualifier("catalog") org.geoserver.catalog.Catalog catalog) {
                  org.geoserver.config.impl.GeoServerImpl bean = new org.geoserver.config.impl.GeoServerImpl();
                  bean.setCatalog(catalog);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("geoServer", xml, expectedGeoServerCode, true);

        final String expectedcapabilitiesCachingHeadersCallbackCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.config.CapabilitiesCacheHeadersCallback capabilitiesCachingHeadersCallback(
                    @org.springframework.beans.factory.annotation.Qualifier("geoServer") org.geoserver.config.GeoServer geoServer) {
                  return new org.geoserver.config.CapabilitiesCacheHeadersCallback(geoServer);
                }
                """;

        testBeanMethodGeneneration(
                "capabilitiesCachingHeadersCallback", xml, expectedcapabilitiesCachingHeadersCallbackCode, true);
    }

    /**
     * Test case for generic collection parameter type inference.
     * <p>
     * This test demonstrates the issue where constructor parameters that are part of a generic collection
     * (like List&lt;Service&gt;) should infer the correct generic type (Service) for individual bean references,
     * not fall back to Object.
     * <p>
     * When a constructor takes {@code List<Service>} and the XML defines a list with service beans,
     * each service bean reference should be typed as {@code Service} in the method parameters,
     * not as {@code Object}.
     */
    @Test
    void testConstructorParameterGenericTypeInference() {
        final String xml =
                """
                <bean id="wfsService-1.0.0" class="org.geoserver.wfs.WfsService"/>
                <bean id="wfsService-1.1.0" class="org.geoserver.wfs.WfsService"/>
                <bean id="geoServer" class="org.geoserver.config.impl.GeoServerImpl"/>

                <bean id="wfsExceptionHandler" class="org.geoserver.wfs.response.WfsExceptionHandler">
                    <constructor-arg>
                        <list>
                            <ref bean="wfsService-1.0.0"/>
                            <ref bean="wfsService-1.1.0"/>
                        </list>
                    </constructor-arg>
                    <constructor-arg ref="geoServer"/>
                </bean>
                """;

        // The WfsExceptionHandler constructor is: WfsExceptionHandler(List<Service> services, GeoServer gs)
        // So the service parameters should be typed as Service (the actual interface), not Object
        // @Qualifier should use original bean names (with dashes) to match XML config
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.wfs.response.WfsExceptionHandler wfsExceptionHandler(
                    @org.springframework.beans.factory.annotation.Qualifier("geoServer") org.geoserver.config.GeoServer geoServer,
                    @org.springframework.beans.factory.annotation.Qualifier("wfsService-1.0.0") org.geoserver.platform.Service wfsService_1_0_0,
                    @org.springframework.beans.factory.annotation.Qualifier("wfsService-1.1.0") org.geoserver.platform.Service wfsService_1_1_0) {
                  return new org.geoserver.wfs.response.WfsExceptionHandler(new java.util.ArrayList<>(java.util.List.of(wfsService_1_0_0, wfsService_1_1_0)), geoServer);
                }
                """;

        testBeanMethodGeneneration("wfsExceptionHandler", xml, expectedJavaCode, true);
    }

    @Test
    void testBeanWithVarargsPropertySetter() {
        final String xml =
                """
                <bean id="wfsURLMapping" class="org.geoserver.ows.OWSHandlerMapping">
                    <property name="interceptors">
                        <list>
                            <ref bean="citeComplianceHack"/>
                            <ref bean="wfsWorkspaceQualifier"/>
                        </list>
                    </property>
                </bean>
                """;

        // OWSHandlerMapping.setInterceptors(Object... interceptors) expects varargs, not a List
        // Should generate: bean.setInterceptors(citeComplianceHack, wfsWorkspaceQualifier);
        // Not: bean.setInterceptors(new ArrayList<>(List.of(citeComplianceHack, wfsWorkspaceQualifier)));
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.ows.OWSHandlerMapping wfsURLMapping(
                    org.geoserver.catalog.Catalog catalog,
                    @org.springframework.beans.factory.annotation.Qualifier("citeComplianceHack") java.lang.Object citeComplianceHack,
                    @org.springframework.beans.factory.annotation.Qualifier("wfsWorkspaceQualifier") java.lang.Object wfsWorkspaceQualifier) {
                  org.geoserver.ows.OWSHandlerMapping bean = new org.geoserver.ows.OWSHandlerMapping(catalog);
                  bean.setInterceptors(citeComplianceHack, wfsWorkspaceQualifier);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("wfsURLMapping", xml, expectedJavaCode);
    }

    @Test
    void testBeanWithClassPropertyShouldUseClassForName() {
        final String xml =
                """
                <bean autowire="default" class="org.geoserver.web.data.settings.SettingsPluginPanelInfo" id="CogSettingsPanel" lazy-init="default">
                    <description>This bean adds the necessary form fields for COG Settings</description>
                    <property name="id" value="cogSettingsPanel"/>
                    <property name="titleKey" value="CogSettings"/>
                    <property name="componentClass" value="org.geoserver.web.data.store.cog.panel.CogSettingsPluginPanel"/>
                    <property name="priority" value="100"/>
                </bean>
                """;

        // The setComponentClass property should use Class.forName() instead of a string
        final String expectedJavaCode =
                """
                @org.springframework.context.annotation.Bean
                org.geoserver.web.data.settings.SettingsPluginPanelInfo CogSettingsPanel()
                        throws java.lang.ClassNotFoundException {
                  org.geoserver.web.data.settings.SettingsPluginPanelInfo bean = new org.geoserver.web.data.settings.SettingsPluginPanelInfo();
                  bean.setId("cogSettingsPanel");
                  bean.setTitleKey("CogSettings");
                  bean.setComponentClass((Class) java.lang.Class.forName("org.geoserver.web.data.store.cog.panel.CogSettingsPluginPanel"));
                  bean.setPriority(100);
                  return bean;
                }
                """;

        testBeanMethodGeneneration("CogSettingsPanel", xml, expectedJavaCode);
    }

    /**
     * Create a fluent assertion builder for verifying MethodSpec properties
     */
    private BeanMethodAssertion assertBeanMethod(MethodSpec method) {
        return new BeanMethodAssertion(method);
    }

    /**
     * Fluent assertion API for validating generated @Bean methods
     */
    private class BeanMethodAssertion {
        private final MethodSpec method;

        public BeanMethodAssertion(MethodSpec method) {
            this.method = method;
        }

        public BeanMethodAssertion compilesSuccessfully() {
            List<String> errors = List.of();
            try {
                String generatedClass = generateTestConfiguration("TestConfiguration", method);
                errors = compileJavaCode("TestConfiguration.java", generatedClass);
                assertTrue(errors.isEmpty(), "Generated code should compile without errors. Errors: " + errors);
            } catch (Exception e) {
                fail("Failed to test compilation: " + errors, e);
            }
            return this;
        }

        public BeanMethodAssertion isEquivalentTo(String expectedJavaCode) {
            try {
                // Parse both expected and actual method code into AST
                JavaParser parser = new JavaParser();

                MethodDeclaration expectedMethod;
                MethodDeclaration actualMethod;

                // Remove comments from expected code but preserve structure for parsing
                String cleanExpectedCode = removeComments(expectedJavaCode.trim());
                expectedMethod = parser.parseMethodDeclaration(cleanExpectedCode)
                        .getResult()
                        .orElseThrow(() -> new AssertionError("Failed to parse expected method code"));

                // Parse actual code (without Javadoc and comments)
                String methodString = method.toString();
                String methodWithoutJavadocString = methodString.replaceAll("/\\*\\*[\\s\\S]*?\\*/\\s*", "");
                String cleanActualCode = removeComments(methodWithoutJavadocString.trim());
                actualMethod = parser.parseMethodDeclaration(cleanActualCode)
                        .getResult()
                        .orElseThrow(() -> new AssertionError("Failed to parse actual method code"));

                // Compare AST structures instead of strings
                if (!isMethodStructurallyEquivalent(expectedMethod, actualMethod)) {
                    throw new AssertionError(
                            """
                            Methods should be structurally equivalent:
                            Expected (clean):
                            %s

                            Actual (without Javadoc, clean):
                            %s

                            Expected AST:
                            %s

                            Actual AST:
                            %s"""
                                    .formatted(cleanExpectedCode, cleanActualCode, expectedMethod, actualMethod));
                }

                return this;
            } catch (Exception e) {
                throw new AssertionError("Failed to perform structural comparison: " + e.getMessage(), e);
            }
        }

        public BeanMethodAssertion hasJavadoc(String xmlContent) {

            if (method.javadoc == null || method.javadoc.isEmpty()) {
                throw new AssertionError("Method should have Javadoc but none was found");
            }

            String javadocContent = method.javadoc.toString();

            // Extract XML content from the javadoc snippet block
            String actualXml = extractXmlFromJavadoc(javadocContent);
            if (actualXml == null) {
                throw new AssertionError("Could not extract XML content from javadoc snippet block");
            }

            // Use XMLUnit for proper XML comparison that ignores attribute order and whitespace
            try {
                // Wrap both XMLs in a root element if they contain multiple root elements
                String wrappedActual = wrapInRootIfNeeded(actualXml);
                String wrappedExpected = wrapInRootIfNeeded(xmlContent.trim());

                org.xmlunit.assertj3.XmlAssert.assertThat(wrappedActual)
                        .and(wrappedExpected)
                        .ignoreComments()
                        .ignoreWhitespace()
                        .areIdentical();
            } catch (AssertionError e) {
                throw new AssertionError(
                        """
                        Method Javadoc XML should be equivalent to expected XML:
                        Expected XML:
                        %s

                        Actual XML from Javadoc:
                        %s

                        Javadoc content:
                        %s

                        XMLUnit comparison error: %s"""
                                .formatted(xmlContent.trim(), actualXml, javadocContent, e.getMessage()));
            }

            return this;
        }

        /**
         * Extract XML content from a javadoc {@snippet lang=xml : ... } block
         */
        private String extractXmlFromJavadoc(String javadocContent) {
            // Look for {@snippet lang=xml : ... } pattern
            String snippetStart = "{@snippet lang=xml :";
            String snippetEnd = "}";

            int startIndex = javadocContent.indexOf(snippetStart);
            if (startIndex == -1) {
                return null;
            }

            startIndex += snippetStart.length();
            int endIndex = javadocContent.lastIndexOf(snippetEnd);
            if (endIndex == -1 || endIndex <= startIndex) {
                return null;
            }

            String xmlContent = javadocContent.substring(startIndex, endIndex).trim();
            return xmlContent;
        }

        /**
         * Wrap XML in a root element if it contains multiple root elements
         */
        private String wrapInRootIfNeeded(String xml) {
            if (xml == null || xml.trim().isEmpty()) {
                return xml;
            }

            String trimmed = xml.trim();

            // Count potential root elements by looking for opening tags at the start of lines
            // This is a simple heuristic - count lines that start with < and don't start with </ or <!
            String[] lines = trimmed.split("\n");
            int rootElementCount = 0;

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("<")
                        && !trimmedLine.startsWith("</")
                        && !trimmedLine.startsWith("<!--")
                        && !trimmedLine.startsWith("<?xml")) {
                    rootElementCount++;
                }
            }

            // If we have multiple root elements, wrap in a container
            if (rootElementCount > 1) {
                return "<root>" + trimmed + "</root>";
            }

            return trimmed;
        }

        private boolean isMethodStructurallyEquivalent(MethodDeclaration expected, MethodDeclaration actual) {
            // Compare method signatures (name, return type, parameters)
            if (!expected.getName().equals(actual.getName())) {
                return false;
            }

            if (!expected.getType().equals(actual.getType())) {
                return false;
            }

            if (expected.getParameters().size() != actual.getParameters().size()) {
                return false;
            }

            // Compare parameters (type and annotations)
            for (int i = 0; i < expected.getParameters().size(); i++) {
                var expectedParam = expected.getParameters().get(i);
                var actualParam = actual.getParameters().get(i);

                if (!expectedParam.getType().equals(actualParam.getType())) {
                    return false;
                }

                if (!expectedParam.getName().equals(actualParam.getName())) {
                    return false;
                }

                // Compare parameter annotations structurally
                if (!areAnnotationsEquivalent(expectedParam.getAnnotations(), actualParam.getAnnotations())) {
                    return false;
                }
            }

            // Compare method annotations
            if (!areAnnotationsEquivalent(expected.getAnnotations(), actual.getAnnotations())) {
                return false;
            }

            // Compare method body (if present)
            if (expected.getBody().isPresent() != actual.getBody().isPresent()) {
                return false;
            }

            if (expected.getBody().isPresent()) {
                // For now, do a simple structural comparison of the body
                // Could be enhanced to be more sophisticated
                String expectedBody = expected.getBody().get().toString();
                String actualBody = actual.getBody().get().toString();

                // Normalize whitespace for body comparison
                String expectedBodyNormalized = normalizeWhitespace(expectedBody);
                String actualBodyNormalized = normalizeWhitespace(actualBody);

                if (!expectedBodyNormalized.equals(actualBodyNormalized)) {
                    return false;
                }
            }

            return true;
        }

        private boolean areAnnotationsEquivalent(
                java.util.List<AnnotationExpr> expected, java.util.List<AnnotationExpr> actual) {
            if (expected.size() != actual.size()) {
                return false;
            }

            // Simple comparison - could be enhanced to handle annotation order differences
            for (int i = 0; i < expected.size(); i++) {
                AnnotationExpr expectedAnn = expected.get(i);
                AnnotationExpr actualAnn = actual.get(i);

                // Compare annotation names
                if (!expectedAnn.getName().equals(actualAnn.getName())) {
                    return false;
                }

                // Compare annotation values (basic implementation)
                if (!expectedAnn.toString().equals(actualAnn.toString())) {
                    // For annotations, we still need string comparison for values
                    // but JavaParser handles the structural parsing
                    String expectedNormalized = normalizeAnnotationWhitespace(expectedAnn.toString());
                    String actualNormalized = normalizeAnnotationWhitespace(actualAnn.toString());
                    if (!expectedNormalized.equals(actualNormalized)) {
                        return false;
                    }
                }
            }

            return true;
        }

        private String normalizeAnnotationWhitespace(String annotation) {
            // More aggressive normalization for annotations to handle formatting differences
            return annotation
                    .replaceAll("\\s+", " ")
                    .replaceAll("\\(\\s+", "(")
                    .replaceAll("\\s+\\)", ")")
                    .trim();
        }

        private String normalizeWhitespace(String code) {
            return code.replaceAll("\\s+", " ")
                    .replace("&#42;", "*") // Decode HTML entity for asterisk
                    .replaceAll("//[^\\n]*", "") // Remove single-line comments
                    .replaceAll("/\\*.*?\\*/", "") // Remove multi-line comments
                    .trim();
        }

        /**
         * Remove comments from code while preserving structure for JavaParser.
         * Unlike normalizeWhitespace, this preserves line breaks and indentation.
         * This method is smart about not removing // inside string literals.
         */
        private String removeComments(String code) {
            return code.replaceAll(
                            "(?m)^\\s*//.*$",
                            "") // Remove single-line comments (lines starting with optional whitespace + //)
                    .replaceAll("/\\*.*?\\*/", "") // Remove multi-line comments
                    .replace("&#42;", "*") // Decode HTML entity for asterisk
                    .trim();
        }
    }

    /**
     * Generate a complete test @Configuration class containing the method
     */
    private String generateTestConfiguration(String className, MethodSpec method) throws IOException {
        TypeSpec configClass = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(org.springframework.context.annotation.Configuration.class)
                .addMethod(method)
                .build();

        JavaFile javaFile =
                JavaFile.builder("org.geoserver.test.generated", configClass).build();

        StringWriter writer = new StringWriter();
        javaFile.writeTo(writer);
        return writer.toString();
    }

    /**
     * Compile Java code using the JavaCompiler API and return any compilation errors
     */
    private List<String> compileJavaCode(String javaFileName, String javaCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No Java compiler available. Make sure you're running on a JDK, not JRE.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        try {
            // Use JUnit's @TempDir for compilation output
            fileManager.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT, List.of(tempDir.toFile()));

            // Create in-memory source file
            JavaFileObject sourceFile = new StringJavaFileObject(javaFileName, javaCode);

            // Compile
            JavaCompiler.CompilationTask compilationTask =
                    compiler.getTask(null, fileManager, diagnostics, null, null, Arrays.asList(sourceFile));

            boolean compilationSuccess = compilationTask.call();
            // Collect compilation errors
            List<String> errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .map(d -> d.getMessage(null))
                    .toList();

            if (!compilationSuccess) {
                Logger.getLogger(XmlConfigTranspileProcessorMethodGenerationTest.class.getName())
                        .warning("compilation failed, errors will be returned: " + errors);
            }
            return errors;

        } catch (IOException e) {
            throw new RuntimeException("Failed to set compilation output directory: " + e.getMessage(), e);
        } finally {
            try {
                fileManager.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    /**
     * In-memory Java source file for compilation testing
     */
    private static class StringJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        public StringJavaFileObject(String fileName, String sourceCode) {
            super(URI.create("string:///" + fileName), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    }
}
