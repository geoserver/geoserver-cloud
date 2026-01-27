# Migrating from GeoServer Cloud 2.28.x to 3.0.0

This guide outlines the key configuration changes when upgrading from GeoServer Cloud 2.28.x to 3.0.0.

> **Important Note**: All configuration changes described in this document are already incorporated as the new defaults in each GeoServer Docker image's `/etc/geoserver` externalized configuration files. You only need to adjust these settings if you were using custom configurations that differ from the defaults.

## Overview

This is a major version upgrade with significant changes:

| Component | 2.28.x | 3.0.0 |
|-----------|--------|-------|
| Spring Boot | 2.7.x | 3.5.x |
| GeoServer | 2.28.x | 3.0.x |
| Java (minimum) | 11 | 17+ |

## Breaking Changes

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

### Spring Cloud Gateway Properties

Spring Boot 3 includes a major upgrade to Spring Cloud Gateway. The gateway configuration namespace has changed:

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

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
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

1. **Update Java Version**: Ensure your deployment environment uses Java 17 or later.

2. **Add Config-First Profile** (if using Spring Cloud Config Server): Add `SPRING_PROFILES_INCLUDE: config-first` to all GeoServer service environment variables in your Docker Compose or Kubernetes configuration.

3. **Migrate from JDBC Config** (if applicable): Export your configuration and migrate to either `datadir` or `pgconfig` backend.

4. **Update Gateway Configurations** (if customized): Move gateway route configurations from `spring.cloud.gateway` to `spring.cloud.gateway.server.webflux`.

5. **Update ACL Configuration** (if customized): Rename `geoserver.acl.enabled` to `geoserver.acl.client.enabled`.

6. **Update HTTP Client Code** (if customized): Migrate any custom HTTP client code from commons-httpclient to Apache HttpClient 5.

7. **Test Your Deployment**: Verify all services start correctly and OWS endpoints respond as expected.

## Known Issues

### REST API Style Uploads

REST API style uploads using path extensions (`.sld`, `.css`, etc.) may not work correctly due to Spring Boot 3 removing suffix pattern matching by default. This requires an upstream GeoServer fix.

**Workaround**: Use the `Content-Type` header instead of file extensions when uploading styles via the REST API.

## Developer Notes

For developers building custom extensions or running tests:

### Test Framework Changes

Spring Boot 3 has deprecated `@MockBean` in favor of `@MockitoBean`:

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
- All Spring Cloud dependencies have been upgraded to be compatible with Spring Boot 3.5
- If using custom externalized configuration files, review the [geoserver-cloud-config](https://github.com/geoserver/geoserver-cloud-config) repository for the complete set of changes
