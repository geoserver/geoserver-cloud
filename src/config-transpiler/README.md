# Spring XML Configuration Transpiler

A build-time annotation processor that converts Spring XML configuration files into type-safe Java `@Configuration` classes with `@Bean` methods, eliminating runtime XML parsing overhead and improving application startup performance.

## Overview

The config-transpiler processes `@TranspileXmlConfig` annotations during compilation to generate equivalent Java configuration classes from XML bean definitions. This approach provides faster startup times, compile-time validation, and better IDE support while maintaining compatibility with existing Spring XML configurations.

## Quick Start

### Maven Configuration

#### Dependencies

First, add the annotation processor as an optional dependency (compile-time only):

```xml
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-config-transpiler-processor</artifactId>
    <version>${revision}</version>
    <optional>true</optional>
</dependency>
```

#### Compiler Plugin Configuration

Configure the Maven compiler plugin with the annotation processor and required dependencies:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <!-- Other processors (e.g., Lombok) -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <!-- Config transpiler processor -->
            <path>
                <groupId>org.geoserver.cloud</groupId>
                <artifactId>gs-config-transpiler-processor</artifactId>
                <version>${revision}</version>
            </path>
            <!-- Required dependencies for XML processing -->
            <path>
                <groupId>org.geoserver</groupId>
                <artifactId>gs-main</artifactId>
                <version>${gs.version}</version>
            </path>
            <path>
                <groupId>javax.servlet</groupId>
                <artifactId>javax.servlet-api</artifactId>
                <version>4.0.1</version>
            </path>
            <path>
                <groupId>org.springframework</groupId>
                <artifactId>spring-webmvc</artifactId>
                <version>${spring.version}</version>
            </path>
        </annotationProcessorPaths>
        <!-- Enable annotation processing -->
        <proc>full</proc>
    </configuration>
</plugin>
```

**Important Notes**:
- The annotation processor requires access to classes referenced in XML files during compilation
- Dependencies must be added to `annotationProcessorPaths`, not just regular `dependencies`
- Include any additional annotation processors (like Lombok) in the same configuration
- The `gs-main` dependency provides GeoServer classes commonly referenced in XML configurations

### Basic Usage

```java
@Configuration
@TranspileXmlConfig("classpath:applicationContext.xml")
@Import(MyConfiguration_Generated.class)
public class MyConfiguration {
}
```

This generates a `MyConfiguration_Generated` class containing `@Bean` methods for all beans defined in the XML file.

## Features

### Resource Location Patterns

The transpiler supports multiple resource location formats:

- **Classpath resources**: `classpath:config/beans.xml`
- **JAR pattern matching**: `jar:gs-main-.*!/applicationContext.xml`
- **Multiple locations**: `{"classpath:beans1.xml", "classpath:beans2.xml"}`

### Bean Filtering

Control which beans are included using regex patterns:

```java
@TranspileXmlConfig(
    locations = "classpath:applicationContext.xml",
    includes = {"catalog.*", "security.*"},
    excludes = {".*Test.*", "mockBean"}
)
```

- Include patterns define which beans to process
- Exclude patterns filter out unwanted beans
- Exclude patterns take precedence over include patterns
- Both bean names and aliases are considered during filtering

### Custom Generated Classes

Configure the output location and naming:

```java
@TranspileXmlConfig(
    locations = "classpath:beans.xml",
    targetPackage = "org.example.generated",
    targetClass = "ApplicationConfig",
    publicAccess = true
)
@Import(org.example.generated.ApplicationConfig.class)
```

### Multiple Configurations

Process different XML files with separate configurations:

```java
@TranspileXmlConfig(
    locations = "classpath:security.xml",
    targetClass = "SecurityConfig"
)
@TranspileXmlConfig(
    locations = "classpath:persistence.xml", 
    targetClass = "PersistenceConfig"
)
@Import({SecurityConfig.class, PersistenceConfig.class})
public class MainConfiguration {
}
```

## Supported Spring Features

### Bean Definitions

- **Simple beans**: Basic bean instantiation with or without XML `id`
- **Constructor injection**: `<constructor-arg>` elements with values and references
- **Indexed constructor arguments**: `<constructor-arg index="0">` for explicit parameter ordering
- **Property injection**: `<property>` elements with values and references
- **Bean dependencies**: `depends-on` attribute (single or multiple dependencies)
- **Bean scopes**: `scope` attribute (e.g., `scope="prototype"`)
- **Lazy initialization**: `lazy-init="true"` attribute
- **Inner classes**: Bean classes with `$` notation converted to `.` notation
- **Protected constructor access**: Reflection-based instantiation for non-public constructors
- **Factory bean patterns**: `MethodInvokingFactoryBean` with both static methods and instance methods

### Bean Naming and Aliases

- **Multiple bean names**: `name` attribute with comma-separated aliases
- **Bean aliases**: `<alias>` elements properly handled
- **Auto-generated names**: Fallback naming for beans without explicit `id`
- **Collision prevention**: Unique suffixes added to method names to prevent conflicts across different configuration classes

### Constructor Arguments

- **Value arguments**: String, numeric, boolean, and class literals
- **Bean references**: `<constructor-arg ref="beanName">`
- **List arguments**: `<list>` elements with bean references
- **Array arguments**: `<list>` elements automatically converted to arrays when constructor expects array types
- **Nested class handling**: Inner class names with `$` notation converted to `.` notation (e.g., `Outer$Inner.class`)
- **Type inference**: Automatic parameter type resolution based on constructor signatures
- **Exception handling**: Proper `throws` clauses for constructors that may fail

### Property Injection

- **Simple values**: String, numeric, and boolean property values
- **Bean references**: `<property name="prop" ref="beanName">`
- **Nested bean definitions**: Inline `<bean>` elements within properties, supporting both FieldRetrievingFactoryBean and simple constructor patterns
- **Managed collections**: 
  - `<props>` elements converted to `Properties` objects
  - `<map>` elements converted to `HashMap` instances
- **Type conversion**: Automatic conversion from string values to target property types

### Collections and References

- **Bean references**: `<ref bean="beanName"/>` with proper `@Qualifier` annotations
- **Managed lists**: `<list>` elements with bean references in constructor arguments and properties
- **Managed properties**: `<props>` elements for key-value pairs
- **Managed maps**: `<map>` elements with class literal support

### Spring Expression Language (SpEL)

- **System properties**: `#{systemProperties['PROPERTY_NAME']}` expressions
- **Elvis operator**: `#{expression ?: defaultValue}` null-safe operations
- **@Value parameters**: SpEL expressions converted to `@Value` annotated method parameters
- **Type-safe conversion**: SpEL results properly typed for constructor arguments

### Component Scanning

XML `<context:component-scan>` elements are converted to `@ComponentScan` annotations:

```xml
<context:component-scan base-package="org.example.services"/>
```

Becomes:

```java
@ComponentScan(basePackages = "org.example.services")
```

### Bean Aliases

Bean aliases are properly handled during filtering - excluding a bean also excludes its aliases:

```xml
<bean id="authManager" class="AuthManager"/>
<alias name="authManager" alias="securityManager"/>
```

Excluding either `authManager` or `securityManager` will exclude both.

## Generated Code Characteristics

### Class Structure

Generated classes follow Spring best practices:

```java
@Configuration
class MyConfiguration_Generated {
    
    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    ModuleStatusImpl gsMainModule() {
        return new ModuleStatusImpl("gs-main", "GeoServer Main");
    }
    
    @Bean
    DataStore dataStore() {
        DataStoreFactoryImpl factory = new DataStoreFactoryImpl();
        DataStore bean = factory.createDataStore();
        bean.setConnectionParams(connectionProperties());
        return bean;
    }
}
```

### Documentation

Generated classes include comprehensive Javadoc:

- Source XML file locations
- List of excluded beans (when filtering is applied)
- Generated bean method documentation with original XML snippets

### Type Safety

The transpiler performs type inference and includes:

- Proper return types for `@Bean` methods
- Type-safe property setters
- Constructor parameter type matching
- Import statements for all referenced types

## Performance Benefits

### Startup Time Improvements

- **No XML parsing**: Eliminates DocumentBuilder and SAX parsing overhead
- **No reflection discovery**: Bean definitions are compiled, not discovered
- **Faster DI resolution**: Direct method calls instead of XML traversal
- **Reduced memory usage**: No XML document caching or namespace handling

### Development Experience

- **Compile-time validation**: Catch configuration errors during build
- **IDE support**: Full code completion and refactoring for bean definitions
- **Type safety**: Compiler verification of bean types and dependencies
- **Debugging**: Step through bean creation in generated code

## Integration Testing

The transpiler includes failsafe plugin configuration for integration tests:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-processorpath</arg>
            <arg>${project.build.directory}/${project.build.finalName}.jar</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

This allows integration tests to use the annotation processor without requiring JAR installation to the local repository.

## Migration Strategy

### From XML to Transpiled Configuration

1. **Start with existing XML**: Use current XML files as-is with `@TranspileXmlConfig`
2. **Add filtering**: Gradually exclude beans that are provided elsewhere
3. **Verify functionality**: Run tests to ensure equivalent behavior
4. **Optimize includes**: Use include patterns to process only needed beans
5. **Remove XML files**: Once transpilation covers all needs, remove original XML

### Compatibility Considerations

- **Bean naming**: Generated bean names match XML `id` attributes
- **Initialization order**: `depends-on` relationships are preserved
- **Property values**: String values are converted to appropriate types
- **Scope handling**: Bean scopes are maintained in generated code

## Troubleshooting

### Common Issues

**Resource not found errors**: Ensure XML files are on the annotation processor classpath, not just the compilation classpath.

**Type resolution failures**: The processor uses runtime reflection for type inference. Ensure all referenced classes are available during compilation.

**Missing imports**: Generated classes automatically include necessary import statements. If compilation fails, check for classpath issues.

### Debugging

Enable verbose annotation processor output:

```xml
<compilerArgs>
    <arg>-Xlint:processing</arg>
</compilerArgs>
```

The processor outputs detailed diagnostic messages about:
- Resource resolution and parsing
- Bean filtering decisions  
- Type inference results
- Code generation progress

## Code Examples

### Simple Bean with Properties

**XML Input:**
```xml
<bean id="moduleStatus" class="org.geoserver.platform.ModuleStatusImpl">
    <property name="module" value="gs-main"/>
    <property name="name" value="GeoServer Main"/>
    <property name="available" value="true"/>
</bean>
```

**Generated Java:**
```java
@Bean
org.geoserver.platform.ModuleStatusImpl moduleStatus() {
    org.geoserver.platform.ModuleStatusImpl bean = new org.geoserver.platform.ModuleStatusImpl();
    bean.setModule("gs-main");
    bean.setName("GeoServer Main");
    bean.setAvailable(true);
    return bean;
}
```

### Constructor Injection with Dependencies

**XML Input:**
```xml
<bean id="dataAccessManager" class="org.geoserver.security.impl.DefaultResourceAccessManager">
    <constructor-arg ref="accessRulesDao"/>
    <constructor-arg ref="catalog"/>
    <property name="groupsCache" ref="layerGroupCache"/>
</bean>
```

**Generated Java:**
```java
@Bean
org.geoserver.security.impl.DefaultResourceAccessManager dataAccessManager(
    @org.springframework.beans.factory.annotation.Qualifier("accessRulesDao") 
    org.geoserver.security.impl.DataAccessRuleDAO accessRulesDao,
    @org.springframework.beans.factory.annotation.Qualifier("catalog") 
    org.geoserver.catalog.Catalog catalog,
    @org.springframework.beans.factory.annotation.Qualifier("layerGroupCache") 
    org.geoserver.security.impl.LayerGroupContainmentCache layerGroupCache) {
    
    org.geoserver.security.impl.DefaultResourceAccessManager bean = 
        new org.geoserver.security.impl.DefaultResourceAccessManager(accessRulesDao, catalog);
    bean.setGroupsCache(layerGroupCache);
    return bean;
}
```

### Bean with Collections and SpEL

**XML Input:**
```xml
<bean id="urlMapping" class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
    <property name="mappings">
        <props>
            <prop key="/api/**">apiDispatcher</prop>
            <prop key="/rest/**">restDispatcher</prop>
        </props>
    </property>
</bean>

<bean id="consoleDisabled" class="java.lang.Boolean">
    <constructor-arg value="#{systemProperties['GEOSERVER_CONSOLE_DISABLED'] ?: false}"/>
</bean>
```

**Generated Java:**
```java
@Bean
@java.lang.SuppressWarnings({"unchecked", "rawtypes"})
org.springframework.web.servlet.handler.SimpleUrlHandlerMapping urlMapping() {
    org.springframework.web.servlet.handler.SimpleUrlHandlerMapping bean = 
        new org.springframework.web.servlet.handler.SimpleUrlHandlerMapping();
    // Property 'mappings' uses ManagedProperties
    java.util.Properties mappingsProps = new java.util.Properties();
    mappingsProps.setProperty("/api/**", "apiDispatcher");
    mappingsProps.setProperty("/rest/**", "restDispatcher");
    bean.setMappings(mappingsProps);
    return bean;
}

@Bean
java.lang.Boolean consoleDisabled(
    @org.springframework.beans.factory.annotation.Value(
        "#{systemProperties['GEOSERVER_CONSOLE_DISABLED'] ?: false}") 
    java.lang.Boolean spelParam0) {
    return new java.lang.Boolean(spelParam0);
}
```

### MethodInvokingFactoryBean

**XML Input:**
```xml
<bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
    <property name="staticMethod" value="org.geoserver.wfs.xml.SqlViewParamsExtractor.setWfsSqlViewKvpParser"/>
    <property name="arguments">
        <list>
            <ref bean="wfsSqlViewKvpParser"/>
        </list>
   </property>
</bean>
```

**Generated Java:**
```java
@Bean
org.springframework.beans.factory.config.MethodInvokingFactoryBean org_springframework_beans_factory_config_MethodInvokingFactoryBean_0_5d582b(
    @org.springframework.beans.factory.annotation.Qualifier("wfsSqlViewKvpParser") java.lang.Object wfsSqlViewKvpParser) {
  org.springframework.beans.factory.config.MethodInvokingFactoryBean bean = new org.springframework.beans.factory.config.MethodInvokingFactoryBean();
  bean.setStaticMethod("org.geoserver.wfs.xml.SqlViewParamsExtractor.setWfsSqlViewKvpParser");
  bean.setArguments((java.util.List) java.util.List.of(wfsSqlViewKvpParser));
  return bean;
}
```

**Note**: The `_5d582b` suffix prevents method name collisions across independent @Configuration classes and is derived from the transpilation context.

### Bean with Annotations and Dependencies

**XML Input:**
```xml
<bean id="geoServerLoader" 
      class="org.geoserver.config.GeoServerLoaderProxy" 
      depends-on="extensions,dataDirectory,securityManager"
      lazy-init="true">
    <constructor-arg ref="resourceLoader"/>
</bean>
```

**Generated Java:**
```java
@Bean
@org.springframework.context.annotation.DependsOn({
    "extensions", "dataDirectory", "securityManager"
})
@org.springframework.context.annotation.Lazy
org.geoserver.config.GeoServerLoaderProxy geoServerLoader(
    @org.springframework.beans.factory.annotation.Qualifier("resourceLoader") 
    org.geoserver.platform.GeoServerResourceLoader resourceLoader) {
    return new org.geoserver.config.GeoServerLoaderProxy(resourceLoader);
}
```

### Bean with Nested Bean Definitions

**XML Input:**
```xml
<bean id="dimensionFactory" class="org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl">
    <property name="featureTimeMinimumStrategy">
        <bean class="org.geoserver.wms.dimension.impl.FeatureMinimumValueSelectionStrategyImpl"/>
    </property>
    <property name="featureTimeMaximumStrategy">
        <bean class="org.geoserver.wms.dimension.impl.FeatureMaximumValueSelectionStrategyImpl"/>
    </property>
</bean>
```

**Generated Java:**
```java
@Bean
org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl dimensionFactory() {
    org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl bean = 
        new org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl();
    bean.setFeatureTimeMinimumStrategy(new org.geoserver.wms.dimension.impl.FeatureMinimumValueSelectionStrategyImpl());
    bean.setFeatureTimeMaximumStrategy(new org.geoserver.wms.dimension.impl.FeatureMaximumValueSelectionStrategyImpl());
    return bean;
}
```

## Limitations

### Currently Not Supported

The transpiler will throw `UnsupportedOperationException` for these patterns:

- **Static factory methods**: `factory-method` attribute for static factory methods  
- **Bean factory references**: `factory-bean` attribute for instance factory methods
- **Abstract bean inheritance**: `abstract="true"` and `parent` attributes
- **AOP proxy factory beans**: `ProxyFactoryBean` and related AOP configurations

### Partially Supported

- **SpEL expressions**: Basic system property access and elvis operator supported, but complex expressions may not work
- **Collection nesting**: Simple lists and maps work, but complex nested collections may have limitations
- **Custom property editors**: Standard type conversion works, but custom `PropertyEditor` implementations are not supported

### Runtime Limitations  

- **Dynamic bean definitions**: Runtime-created beans cannot be transpiled (by design)
- **Custom namespace handlers**: Non-standard XML namespaces require manual handling
- **Circular dependencies**: Some circular dependency patterns may require adjustment
- **Bean post-processors**: Custom `BeanPostProcessor` logic is not replicated in generated code

### Error Handling

The transpiler provides clear error messages for unsupported patterns:

```
UnsupportedOperationException: Static factory methods are not supported. 
Found factory-method 'createInstance' in bean 'exampleBean'
```

This fail-fast approach ensures you know exactly what needs to be addressed before implementing support for additional patterns.

## Test Suite

The transpiler includes a comprehensive test suite in `src/test/java/org/geoserver/spring/config/processor/XmlConfigTranspileProcessorMethodGenerationTest.java` that covers:

- **51 passing tests** covering all supported Spring XML patterns
- **Real GeoServer configurations** extracted from actual applicationContext.xml files  
- **End-to-end validation** including compilation testing of generated code
- **AST-based comparison** for robust structural validation
- **Javadoc validation** ensuring generated documentation matches source XML

Key test categories:
- Basic bean patterns (simple beans, inner classes, auto-generated names)
- Constructor injection (values, references, indexed arguments, lists)
- Property injection (values, references, managed collections)  
- Bean annotations (lazy, scope, depends-on, suppress warnings)
- Dependency resolution (qualifiers, type inference, implicit autowiring)
- Spring annotations (@ComponentScan conversion)
- SpEL expressions (system properties, elvis operator)
- Factory bean patterns (MethodInvokingFactoryBean with static and instance methods)
- Method name collision prevention (unique suffixes for auto-generated bean methods)
- Complex configurations (OWS handler mappings, security beans, module status)

Each test includes both the source XML and expected generated Java code, making it easy to understand exactly what transformations are supported.