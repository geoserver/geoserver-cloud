# Logging

This document explains the logging architecture and implementation choices in GeoServer Cloud, including how logging dependencies are managed and how GeoTools/GeoServer logging is integrated with Spring Boot.

## Logging Architecture: spring-jcl

GeoServer Cloud uses `spring-jcl` (Spring's Jakarta Commons Logging bridge) instead of the traditional `commons-logging` library. This is the recommended approach for Spring Boot applications.

### Why spring-jcl?

Spring Boot applications should use `spring-jcl` because:

1. **Spring Boot's default** - Spring Boot automatically excludes `commons-logging` and provides `spring-jcl` as its replacement
2. **Seamless SLF4J integration** - `spring-jcl` bridges directly to SLF4J/Logback without additional configuration
3. **No extra bridges needed** - With `commons-logging`, you would need `jcl-over-slf4j` to route logs to Logback
4. **No classloader issues** - The original `commons-logging` had notorious classloader discovery problems that `spring-jcl` eliminates

### The Upstream GeoServer Conflict

Upstream GeoServer's `gs-main` module explicitly excludes `spring-jcl`:

```xml
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-core</artifactId>
  <exclusions>
    <exclusion>
      <artifactId>spring-jcl</artifactId>
      <groupId>org.springframework</groupId>
    </exclusion>
  </exclusions>
</dependency>
```

This exclusion exists because traditional GeoServer (non-Spring Boot) uses `commons-logging` directly, and having both libraries causes `LogFactory` class conflicts.

### GeoServer Cloud Solution

Since GeoServer Cloud is a Spring Boot application, we explicitly re-add `spring-jcl` in `src/apps/geoserver/pom.xml` to override the upstream exclusion. This ensures all GeoServer microservices use Spring Boot's preferred logging bridge.

## Logging Initialization

The logging bridge configuration is set up by `GeoServerContextInitializer` (`src/starters/webmvc/src/main/java/org/geoserver/cloud/autoconfigure/context/GeoServerContextInitializer.java`), which is an `ApplicationContextInitializer` that runs before Spring beans are loaded.

This initializer replaces upstream GeoServer's `GeoserverInitStartupListener` (a servlet context listener that runs too late in Spring Boot) and configures the following system properties:

```java
// Tell GeoServer not to control logging, Spring Boot will handle it
System.setProperty("RELINQUISH_LOG4J_CONTROL", "true");

// Tell GeoTools to use Commons Logging for log redirection
System.setProperty("GT2_LOGGING_REDIRECTION", "CommonsLogging");
```

The `GT2_LOGGING_REDIRECTION=CommonsLogging` setting is crucial because:

1. It configures GeoTools to redirect its `java.util.logging` calls through Commons Logging
2. Commons Logging calls are then handled by `spring-jcl`
3. `spring-jcl` bridges to SLF4J
4. SLF4J routes to Logback (Spring Boot's default)

This creates the logging chain: **GeoTools JUL -> Commons Logging -> spring-jcl -> SLF4J -> Logback**

Other redirection options were considered but rejected:
- **Logback**: Incorrectly maps logging levels (e.g., FINE called with INFO doesn't log)
- **Log4J2**: Fails when calling `Logger.setLevel()` due to API changes in newer Log4j2 versions
