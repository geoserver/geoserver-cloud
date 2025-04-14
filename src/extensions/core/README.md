# GeoServer Cloud Extensions Core

This module provides common infrastructure for GeoServer Cloud extensions, including conditional annotations that help activate components when specific GeoServer services are available.

## Conditional Annotations

The extensions core module defines several conditional annotations that control when extension components should be activated. These annotations follow a consistent pattern:

1. `@ConditionalOnClass` - Verifies that a specific GeoServer class is on the classpath
2. `@ConditionalOnProperty` - Checks for the presence of a configuration property
3. `@ConditionalOnGeoServer` - Base condition that verifies GeoServer core is available

### Conditional Annotation Mappings

Each conditional annotation pairs a class check with a property check, ensuring both are satisfied:

| Annotation | Class Condition | Property Condition | Source JAR |
|------------|----------------|-------------------|------------|
| `@ConditionalOnGeoServer` | `org.geoserver.config.GeoServer` | n/a | gs-main |
| `@ConditionalOnGeoServerWMS` | `org.geoserver.wms.DefaultWebMapService` | `geoserver.service.wms.enabled=true` | gs-wms |
| `@ConditionalOnGeoServerWFS` | `org.geoserver.wfs.DefaultWebFeatureService` | `geoserver.service.wfs.enabled=true` | gs-wfs |
| `@ConditionalOnGeoServerWCS` | `org.geoserver.wcs.responses.CoverageResponseDelegateFinder` | `geoserver.service.wcs.enabled=true` | gs-wcs |
| `@ConditionalOnGeoServerWPS` | `org.geoserver.wps.DefaultWebProcessingService` | `geoserver.service.wps.enabled=true` | gs-wps |
| `@ConditionalOnGeoServerREST` | `org.geoserver.rest.security.RestConfigXStreamPersister` | `geoserver.service.restconfig.enabled=true` | gs-restconfig |
| `@ConditionalOnGeoServerWebUI` | `org.geoserver.web.GeoServerApplication` | `geoserver.service.webui.enabled=true` | gs-web-core |

These property conditions are set to `true` by default in each service's bootstrap configuration file, which ensures reliable detection of the service type during auto-configuration processing.

### Testing with Conditionals

When writing tests for components that use these conditionals, you need to satisfy both the class and property requirements. The recommended approach is to:

1. Add the appropriate dependency with `<scope>test</scope>` to your module's pom.xml
2. Use Spring's `ApplicationContextRunner` or `WebApplicationContextRunner` in tests
3. Set the required property values

Example:

```java
private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        // @ConditionalOnGeoServer
        .withBean(GeoServer.class, () -> mock(GeoServer.class))
        // @ConditionalOnGeoServerWMS
        .withPropertyValues("geoserver.service.wms.enabled=true")
        .withConfiguration(AutoConfigurations.of(YourAutoConfiguration.class));
```

For testing the class condition aspect, you can use `FilteredClassLoader` to verify conditional behavior when a class is not available:

```java
contextRunner
    .withClassLoader(new FilteredClassLoader(RequiredClass.class))
    .withPropertyValues("required.property=true")
    .run(context -> {
        // Component should not be created when class is unavailable
        assertThat(context).doesNotHaveBean(YourComponent.class);
    });
```

## Maven Dependencies

Extensions that depend on specific GeoServer components should declare them as follows:

```xml
<dependency>
  <groupId>org.geoserver</groupId>
  <artifactId>gs-wms</artifactId>
  <scope>provided</scope>
</dependency>
```

For test dependencies, use:

```xml
<dependency>
  <groupId>org.geoserver</groupId>
  <artifactId>gs-wms</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <scope>test</scope>
</dependency>
```