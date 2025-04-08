# GeoServer Cloud Extensions Core

This module provides common infrastructure for GeoServer Cloud extensions, including conditional annotations that help activate components when specific GeoServer services are available.

## Conditional Annotations

The extensions core module defines several conditional annotations that control when extension components should be activated. These annotations follow a consistent pattern:

1. `@ConditionalOnClass` - Verifies that a specific GeoServer class is on the classpath
2. `@ConditionalOnBean` - Checks for the presence of a named bean in the Spring context 
3. `@ConditionalOnGeoServer` - Base condition that verifies GeoServer core is available

### Conditional Annotation Mappings

Each conditional annotation pairs a class check with a bean name check, ensuring both are satisfied:

| Annotation | Class Condition | Bean Condition | Source JAR |
|------------|----------------|----------------|------------|
| `@ConditionalOnGeoServer` | `org.geoserver.config.GeoServer` | GeoServer bean | gs-main |
| `@ConditionalOnGeoServerWMS` | `org.geoserver.wms.DefaultWebMapService` | wmsServiceTarget bean | gs-wms |
| `@ConditionalOnGeoServerWFS` | `org.geoserver.wfs.DefaultWebFeatureService` | wfsServiceTarget bean | gs-wfs |
| `@ConditionalOnGeoServerWCS` | `org.geoserver.wcs.responses.CoverageResponseDelegateFinder` | coverageResponseDelegateFactory bean | gs-wcs |
| `@ConditionalOnGeoServerWPS` | `org.geoserver.wps.DefaultWebProcessingService` | wpsServiceTarget bean | gs-wps |
| `@ConditionalOnGeoServerREST` | `org.geoserver.rest.security.RestConfigXStreamPersister` | restConfigXStreamPersister bean | gs-restconfig |
| `@ConditionalOnGeoServerWebUI` | `org.geoserver.web.GeoServerApplication` | webApplication bean | gs-web-core |

### Testing with Conditionals

When writing tests for components that use these conditionals, you need to satisfy both the class and bean requirements. The recommended approach is to:

1. Add the appropriate dependency with `<scope>test</scope>` to your module's pom.xml
2. Use Spring's `ApplicationContextRunner` or `WebApplicationContextRunner` in tests
3. Provide mock implementations of the required beans

Example:

```java
private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        // @ConditionalOnGeoServer
        .withBean(GeoServer.class, () -> mock(GeoServer.class))
        // @ConditionalOnGeoServerWMS
        .withBean("wmsServiceTarget", DefaultWebMapService.class, () -> mock(DefaultWebMapService.class))
        .withConfiguration(AutoConfigurations.of(YourAutoConfiguration.class));
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