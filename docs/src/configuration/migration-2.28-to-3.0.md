# Migrating from GeoServer Cloud 2.28.x to 3.0.0

This guide outlines the key configuration changes when upgrading from GeoServer Cloud 2.28.x to 3.0.0.

> **Important Note**: All configuration changes described in this document are already incorporated as the new defaults in each GeoServer Docker image's `/etc/geoserver` externalized configuration files. You only need to adjust these settings if you were using custom configurations that differ from the defaults.

## Overview

This is a major version upgrade with significant changes:

| Component | 2.28.x | 3.0.0 |
|-----------|--------|-------|
| Spring Boot | 2.7.x | 4.0.x |
| Spring Cloud | 2022.0.x | 2025.1.x |
| GeoServer | 2.28.x | 3.0.x |
| Java (minimum) | 11 | 25+ |
| RabbitMQ (broker) | 3.x | 4.x |

## Breaking Changes

### Message Broker: RabbitMQ 4.x Required

> **Important**: GeoServer Cloud 3.0.0 requires **RabbitMQ 4.x**. It does not work with RabbitMQ 3.x.

GeoServer Cloud uses RabbitMQ for the Spring Cloud Bus that propagates catalog and configuration changes between services. As of the Spring Boot 4 upgrade, the bus declares its queues with an argument (`x-queue-leader-locator`) that RabbitMQ 3.x rejects with a `406 PRECONDITION_FAILED` error, which prevents the services from starting against a 3.x broker.

- Deploy **RabbitMQ 4.x** before or together with the 3.0.0 services. The provided Docker Compose examples use `rabbitmq:4-management-alpine`.
- No broker data migration is required: the bus uses transient, auto-deleted queues that are recreated on startup. Point the services at the 4.x broker and start them.

### Docker Compose and Service Discovery

We replaced the Eureka discovery service with **Consul**. 
This change only affects Docker Compose deployments.

**Migration Steps:**

1.  **Update Images**: Rebuild or pull the latest 3.0.0 images.
2.  **Replace Discovery Service**: Update your `compose.yml` to use `hashicorp/consul:1.22.5` instead of the GeoServer Cloud discovery image.
3.  **Update Ports**: Map Consul's API/UI to port `8500`.
4.  **Spring Profile**: Ensure the `discovery-consul` profile is active (part of the `config-first` group).

### Docker Compose Profile Configuration

**Breaking Change**: The `config-first` profile is no longer included by default in the base service templates.

If you are using the Spring Cloud Config Server (config-first bootstrap approach) with custom Docker Compose files, you must explicitly add the `config-first` profile:

```yaml
services:
  your-service:
    environment:
      SPRING_PROFILES_INCLUDE: config-first
```

This change affects all GeoServer microservices (wms, wfs, wcs, wps, gwc, restconfig, webui) and the gateway when using centralized configuration.

**Example from the documentation compose files:**

```yaml
x-variables:
  environment: &common_env
    SPRING_PROFILES_INCLUDE: config-first
    SPRING_PROFILES_ACTIVE: datadir
    # ... other environment variables
```

### Removed: JDBC Config Backend

The JDBC Config backend has been completely removed from GeoServer Cloud 3.0.0. If you are currently using the `jdbcconfig` profile, you must migrate to one of the supported backends:

- **Data Directory** (`datadir` profile): Traditional file-based configuration storage
- **PostgreSQL** (`pgconfig` profile): Database-backed configuration with full clustering support

**Migration path:**

1. Export your current configuration using the GeoServer REST API or Web UI
2. Set up your new backend (datadir or pgconfig)
3. Import the configuration into the new backend
4. Update your Docker Compose or Kubernetes configuration to use the new profile

## Configuration Property Changes

### Gateway Implementation Change

GeoServer Cloud 3.0.0 replaces the default gateway implementation from Spring Cloud Gateway Server WebFlux (reactive) to **Spring Cloud Gateway Server MVC** (servlet-based with virtual threads).

**If you have not customized the gateway configuration**, the migration is transparent — the new Docker image `geoservercloud/geoserver-cloud-gateway:3.0.0` works out of the box with the default configuration.

**If you have customized the gateway configuration**, note the following changes:

| Aspect | 2.28.x (WebFlux) | 3.0.0 (WebMVC) |
|--------|------------------|-----------------|
| Docker image | `geoservercloud/geoserver-cloud-gateway` | `geoservercloud/geoserver-cloud-gateway` (unchanged) |
| Config namespace | `spring.cloud.gateway.server.webflux` | `spring.cloud.gateway.server.webmvc` |
| Default filters | Supported via `default-filters` | Not supported; add filters to each route |
| WebSocket routing | Supported | Not supported |

#### Key configuration differences

1. **No `default-filters`**: SCG Server MVC does not support `default-filters`. Filters like `StripBasePath`, `SharedAuth`, `SecureHeaders`, and `DedupeResponseHeader` must be listed explicitly in each route's `filters` section.

2. **Header forwarding** uses a different structure:

    ```yaml
    # Old (WebFlux)
    spring.cloud.gateway.server.webflux:
      forwarded.enabled: false
      x-forwarded:
        for-enabled: true

    # New (WebMVC)
    spring.cloud.gateway.server.webmvc:
      forwarded-request-headers-filter:
        enabled: false
      x-forwarded-request-headers-filter:
        enabled: true
        for-enabled: true
    ```

3. **Trusted proxies** are required for X-Forwarded header processing:

    ```yaml
    spring.cloud.gateway.server.webmvc:
      trusted-proxies: ".*"  # restrict in production
    ```

#### Using the deprecated WebFlux gateway

The WebFlux-based gateway remains available as a separate Docker image for 3.0.0 only:

```
geoservercloud/geoserver-cloud-gateway-webflux:3.0.0
```

Its default configuration is embedded in the Docker image at `/etc/geoserver/gateway-webflux.yml`.

> **Warning**: The WebFlux gateway will not be continued for 3.1.0. Migrate to the WebMVC gateway before upgrading to 3.1.0.

To use the deprecated WebFlux gateway, replace the gateway service image in your Docker Compose:

```yaml
services:
  gateway:
    image: geoservercloud/geoserver-cloud-gateway-webflux:3.0.0
    # ... rest of configuration unchanged
```

### Spring Cloud Gateway Route Namespace

Spring Boot 4 includes a major upgrade to Spring Cloud Gateway. The route configuration namespace has changed. This applies whether you use the new WebMVC gateway (default) or the deprecated WebFlux gateway:

#### Old Structure (2.28.x)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: wms
          uri: lb://wms-service
          predicates:
            - Path=/geoserver/wms/**
```

#### New Structure (3.0.0)

For the default WebMVC gateway, routes are under `spring.cloud.gateway.server.webmvc.routes`.
For the deprecated WebFlux gateway, routes are under `spring.cloud.gateway.server.webflux.routes`.

```yaml
# WebMVC (default)
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: wms
              uri: lb://wms-service
              predicates:
                - Path=/geoserver/wms/**
```

### ACL Client Configuration

The GeoServer ACL client configuration property has been renamed for consistency:

| Old Property (2.28.x) | New Property (3.0.0) |
|-----------------------|----------------------|
| `geoserver.acl.enabled` | `geoserver.acl.client.enabled` |

**Example:**

```yaml
# Old (2.28.x)
geoserver:
  acl:
    enabled: true

# New (3.0.0)
geoserver:
  acl:
    client:
      enabled: true
```

### HTTP Client Migration

GeoServer Cloud 3.0.0 has migrated from commons-httpclient to Apache HttpClient 5. If you have custom HTTP proxy configurations or custom HTTP client code, update your imports:

| Old Package | New Package |
|-------------|-------------|
| `org.apache.commons.httpclient.*` | `org.apache.hc.client5.http.*` |

## Migration Steps

To migrate from GeoServer Cloud 2.28.x to 3.0.0:

1. **Update Java Version**: Ensure your deployment environment uses Java 25 or later.

2. **Add Config-First Profile** (if using Spring Cloud Config Server): Add `SPRING_PROFILES_INCLUDE: config-first` to all GeoServer service environment variables in your Docker Compose or Kubernetes configuration.

3. **Migrate from JDBC Config** (if applicable): Export your configuration and migrate to either `datadir` or `pgconfig` backend.

4. **Update Gateway Configurations** (if customized): If you have not modified the default gateway configuration, no action is needed — the new WebMVC gateway works out of the box. If you have custom route configurations, move them from `spring.cloud.gateway` to `spring.cloud.gateway.server.webmvc` and add filters explicitly to each route (see [Gateway Implementation Change](#gateway-implementation-change)).

5. **Update ACL Configuration** (if customized): Rename `geoserver.acl.enabled` to `geoserver.acl.client.enabled`.

6. **Update HTTP Client Code** (if customized): Migrate any custom HTTP client code from commons-httpclient to Apache HttpClient 5.

7. **Test Your Deployment**: Verify all services start correctly and OWS endpoints respond as expected.

## Known Issues

### REST API Style Uploads

REST API style uploads using path extensions (`.sld`, `.css`, etc.) may not work correctly due to Spring Boot 4 removing suffix pattern matching by default. This requires an upstream GeoServer fix.

**Workaround**: Use the `Content-Type` header instead of file extensions when uploading styles via the REST API.

## Developer Notes

For developers building custom extensions or running tests:

### Test Framework Changes

Spring Boot 4 has deprecated `@MockBean` in favor of `@MockitoBean`:

```java
// Old (2.28.x)
@MockBean
private SomeService someService;

// New (3.0.0)
@MockitoBean
private SomeService someService;
```

## Additional Notes

- The Docker images include all configuration changes by default in `/etc/geoserver`
- Service names in gateway routes have been shortened for consistency
- All Spring Cloud dependencies have been upgraded to Spring Cloud 2025.1.x, compatible with Spring Boot 4.0
- If using custom externalized configuration files, review the [geoserver-cloud-config](https://github.com/geoserver/geoserver-cloud-config) repository for the complete set of changes
