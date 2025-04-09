# Adding Extensions to GeoServer Cloud

This guide outlines the process for adding new extensions to the GeoServer Cloud ecosystem. Extensions allow for modularity and flexibility in the codebase, enabling features to be optionally included based on application requirements.

## Extension Architecture

GeoServer Cloud organizes extensions in a modular structure:

1. **Extension Module** - The core implementation
   - Located in `src/extensions/<category>/<extension-name>`
   - Contains auto-configuration, properties, and conditional annotations
   - Self-contained with clear dependencies

2. **Starter Module** - The convenient inclusion mechanism
   - Located in `src/starters/<category>`
   - Depends on relevant extension modules
   - Provides a single dependency for users to include

## Step-by-Step Guide

### 1. Create the Extension Module Structure

First, create the appropriate directory structure for your extension:

```
src/extensions/
  └── <category>/
      └── <extension-name>/
          ├── pom.xml
          └── src/
              ├── main/
              │   ├── java/
              │   │   └── org/geoserver/cloud/autoconfigure/extensions/...
              │   └── resources/
              │       └── META-INF/
              │           └── spring.factories (or spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
              └── test/
                  ├── java/
                  │   └── org/geoserver/cloud/autoconfigure/extensions/...
                  └── resources/
```

Where:
- `<category>` is the functional category (e.g., `security`, `vector-formats`, `output-formats`, etc.)
- `<extension-name>` is the specific extension name

### 2. Configure the Module POM

Create a `pom.xml` file for your extension with the appropriate dependencies:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.geoserver.cloud.extensions</groupId>
    <artifactId>gs-cloud-extensions-<category></artifactId>
    <version>${revision}</version>
  </parent>
  <artifactId>gs-cloud-extension-<category>-<extension-name></artifactId>
  <packaging>jar</packaging>
  <description>GeoServer Cloud <extension-name> extension</description>

  <dependencies>
    <!-- Core extension dependency -->
    <dependency>
      <groupId>org.geoserver.cloud.extensions</groupId>
      <artifactId>gs-cloud-extensions-core</artifactId>
    </dependency>
    
    <!-- Extension-specific dependencies -->
    <dependency>
      <groupId>org.geoserver</groupId>
      <artifactId>gs-<extension-related-module></artifactId>
      <optional>true</optional>
    </dependency>
    
    <!-- Test dependencies are inherited from parent -->
  </dependencies>
</project>
```

### 3. Add the Extension to the Parent POM

Add your extension module to its category's parent `pom.xml`. For example, if adding a security extension, update:

```xml
<!-- In src/extensions/security/pom.xml -->
<modules>
  <!-- existing modules -->
  <module><extension-name></module>
</modules>
```

### 4. Add the Extension to the Dependency Management Section

Add your extension to the dependency management section in the root `pom.xml`:

```xml
<dependencyManagement>
  <dependencies>
    <!-- existing dependencies -->
    <dependency>
      <groupId>org.geoserver.cloud.extensions</groupId>
      <artifactId>gs-cloud-extension-<category>-<extension-name></artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 5. Create Configuration Properties

Create a configuration properties class to enable/disable and configure your extension:

```java
package org.geoserver.cloud.autoconfigure.extensions.<category>.<extensionname>;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = <Extension>ConfigProperties.PREFIX)
public class <Extension>ConfigProperties {

    public static final String PREFIX = "geoserver.extension.<category>.<extension-name>";
    
    /** Whether the extension is enabled (default: false) */
    public static final boolean DEFAULT = false;
    
    /** Enable/disable the extension */
    private boolean enabled = DEFAULT;
    
    /** Additional configuration properties */
    private String someProperty;
}
```

### 6. Create a Conditional Annotation

Create a custom conditional annotation to control when your extension's beans are registered:

```java
package org.geoserver.cloud.autoconfigure.extensions.<category>.<extensionname>;

import java.lang.annotation.*;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServer
@ConditionalOnProperty(
        prefix = <Extension>ConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = <Extension>ConfigProperties.DEFAULT)
public @interface ConditionalOn<Extension> {}
```

### 7. Create Auto-Configuration Class

Create an auto-configuration class for your extension:

```java
package org.geoserver.cloud.autoconfigure.extensions.<category>.<extensionname>;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(<Extension>ConfigProperties.class)
@ConditionalOn<Extension>
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.<category>.<extensionname>")
public class <Extension>AutoConfiguration {

    private final <Extension>ConfigProperties properties;

    public <Extension>AutoConfiguration(<Extension>ConfigProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void log() {
        log.info("GeoServer <Extension> extension enabled");
    }

    @Bean
    public SomeExtensionBean someExtensionBean() {
        return new SomeExtensionBean(properties.getSomeProperty());
    }
}
```

### 8. Register Auto-Configuration

Depending on your Spring Boot version:

For Spring Boot 2.x, add to `META-INF/spring.factories`:
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.geoserver.cloud.autoconfigure.extensions.<category>.<extensionname>.<Extension>AutoConfiguration
```

For Spring Boot 3.x, add to `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
```
org.geoserver.cloud.autoconfigure.extensions.<category>.<extensionname>.<Extension>AutoConfiguration
```

### 9. Write Tests

Create tests to verify your extension:

```java
package org.geoserver.cloud.autoconfigure.extensions.<category>.<extensionname>;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.config.GeoServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class <Extension>AutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        // Create a mock GeoServer for @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);
        
        contextRunner = new ApplicationContextRunner()
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withConfiguration(AutoConfigurations.of(<Extension>AutoConfiguration.class));
    }

    @Test
    void testDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(<Extension>AutoConfiguration.class);
            assertThat(context).getBean(<Extension>ConfigProperties.class).hasFieldOrPropertyWithValue("enabled", false);
        });
    }

    @Test
    void testExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.<category>.<extension-name>.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(<Extension>AutoConfiguration.class);
                    assertThat(context).hasSingleBean(SomeExtensionBean.class);
                });
    }
}
```

### 10. Add to the Starter Module

Add your extension to the appropriate starter:

```xml
<!-- In src/starters/<category>/pom.xml -->
<dependencies>
  <!-- existing dependencies -->
  <dependency>
    <groupId>org.geoserver.cloud.extensions</groupId>
    <artifactId>gs-cloud-extension-<category>-<extension-name></artifactId>
  </dependency>
</dependencies>
```

### 11. Add Configuration to `geoserver.yml`

Add your extension's configuration to `config/geoserver.yml`:

```yaml
geoserver:
  extension:
    <category>:
      <extension-name>:
        enabled: ${geoserver.<extension-name>.enabled:false}
        # Other properties
```

### 12. Create Documentation

Create a README.md file with documentation for your extension:

```markdown
# GeoServer <Extension> Extension

This module integrates <extension functionality> with GeoServer Cloud.

## Overview

<Overview of what the extension does>

## Configuration

The extension is **disabled by default**. To enable it, add the following configuration:

```yaml
geoserver:
  extension:
    <category>:
      <extension-name>:
        enabled: true
```

## Implementation Details

<Implementation details and dependencies>

## Real-World Examples

### Example: Adding a Security Extension

Here's a simplified example from adding the GeoServer ACL security extension:

1. **Directory Structure**:
   ```
   src/extensions/security/geoserver-acl/
   ```

2. **Configuration Properties**:
   ```java
   @Data
   @ConfigurationProperties(prefix = AclConfigProperties.PREFIX)
   public class AclConfigProperties {
       public static final String PREFIX = "geoserver.extension.security.acl";
       public static final String LEGACY_PREFIX = "geoserver.acl";
       public static final boolean DEFAULT = false;
       private boolean enabled = DEFAULT;
   }
   ```

3. **Conditional Annotation**:
   ```java
   @ConditionalOnGeoServer
   @ConditionalOnProperty(
           prefix = AclConfigProperties.PREFIX,
           name = "enabled",
           havingValue = "true",
           matchIfMissing = AclConfigProperties.DEFAULT)
   public @interface ConditionalOnAcl {}
   ```

4. **Auto-Configuration**:
   ```java
   @AutoConfiguration
   @EnableConfigurationProperties(AclConfigProperties.class)
   @ConditionalOnAcl
   public class AclAutoConfiguration {
       @PostConstruct
       void log() {
           log.info("GeoServer ACL extension enabled");
       }
   }
   ```

## Best Practices

1. **Clear Dependencies**: Make dependencies explicit and use `<optional>true</optional>` for those that might not be available.

2. **Consistent Naming**: Follow naming conventions used by existing extensions.

3. **Default to Disabled**: Extensions should be disabled by default (`DEFAULT = false`).

4. **Test Coverage**: Write comprehensive tests using `ApplicationContextRunner`.

5. **Mock Dependencies**: Use Mockito to mock any beans required by the conditional annotations.

6. **Documentation**: Provide clear documentation for your extension.

7. **Configuration Properties**: Use standard prefixes and provide both new and legacy property support when needed.

8. **Handling Service-Specific Extensions**: For extensions that require specific GeoServer services (like WMS), use additional conditional annotations like `@ConditionalOnGeoServerWMS`.

By following these guidelines, you can create well-structured, testable, and maintainable extensions for GeoServer Cloud.

## Advanced Concepts

### Integration with UI Components

For extensions that integrate with the GeoServer web UI:

1. Add resources to `src/main/resources/org/geoserver/...`
2. Implement UI beans (like `LoginFormInfo`) with appropriate priorities
3. Use the `org.geoserver.web` package structure

### Extension Priority

When multiple extensions provide similar functionality, use the `ExtensionPriority` interface:

```java
public class PrioritizableComponent implements ExtensionPriority {
    private int priority = ExtensionPriority.LOWEST;
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
}
```

### Backward Compatibility

For maintaining backward compatibility with existing configurations:

1. Support both new and legacy property prefixes
2. Use property placeholders in `geoserver.yml`:
   ```yaml
   geoserver:
     extension:
       category:
         name:
           enabled: ${legacy.property.name:false}
   ```

## Conclusion

Creating well-structured extensions for GeoServer Cloud promotes maintainability, testability, and modularity. By following this guide, you can create extensions that seamlessly integrate with the GeoServer Cloud ecosystem while maintaining high code quality standards.