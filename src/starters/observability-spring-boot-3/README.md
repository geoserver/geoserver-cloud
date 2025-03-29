# GeoServer Cloud Observability Spring Boot 3 Starter

This module provides a Spring Boot 3 compatible version of the observability starter (logging, metrics, tracing) for GeoServer Cloud services, with special support for WebFlux applications and the Gateway service.

## Features

- SLF4J/Logback based logging configuration
- MDC (Mapped Diagnostic Context) enrichment for both servlet and WebFlux applications
- Access logging for both servlet and WebFlux applications
- GeoServer OWS MDC integration
- Full Spring Boot 3 compatibility with Jakarta EE
- Reactive context propagation for MDC in WebFlux applications
- Spring Cloud Gateway integration

## Usage

### Maven Dependency

```xml
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-observability-spring-boot-3</artifactId>
</dependency>
```

### Configuration Properties

Various MDC and logging behaviors can be configured through properties:

```yaml
# Access logging configuration
logging:
  accesslog:
    enabled: true
    info-patterns: /api/**, /rest/**
    debug-patterns: /events/**
    trace-patterns: /**

# MDC configuration properties
geoserver:
  mdc:
    authentication:
      id: true
      authorities: true
    http:
      id: true
      remote-addr: true
      remote-host: true
      method: true
      url: true
      query-string: true
      parameters: false
      session-id: true
      headers: false
      cookies: false
    spring:
      application-name: true
      profile: true
      instance-id: true
      version: true
      properties:
        my.custom.property: true
    ows:
      service: true
      service-version: true
      operation: true
```

## MDC Propagation in WebFlux

This module includes special support for MDC propagation in reactive applications using WebFlux. The MDC context is maintained throughout the reactive chain using Reactor's context propagation capabilities.

This means that log messages emitted from anywhere in the processing chain will have access to the same MDC properties, even across asynchronous boundaries. This is particularly important for:

- Request tracing
- User identification
- Correlation IDs
- Request metadata

## Gateway Integration

For Spring Cloud Gateway applications, this module also provides:

- GlobalFilter adapters to ensure MDC context is available to all Gateway filters
- Access logging within the Gateway filter chain
- Proper handling of the dual filter chains in Gateway (WebFilter and GlobalFilter)

To use this in the Gateway service:

```xml
<dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-observability-spring-boot-3</artifactId>
</dependency>
```

The Gateway integration will automatically activate when the Spring Cloud Gateway classes are detected on the classpath.