# GeoServer ACL Extension

This module integrates the GeoServer ACL (Access Control List) security system with GeoServer Cloud.

## Overview

GeoServer ACL provides a centralized authorization service for controlling access to GeoServer resources. This extension module provides integration between GeoServer Cloud microservices and the GeoServer ACL service.

Key features:
- Centralized authorization for GeoServer resources
- Fine-grained access control to workspaces, layers, and services
- Support for spatial filtering of data
- Integration with Spring Cloud event bus for cache invalidation
- Compatible with all GeoServer Cloud catalog backends

## Configuration

The ACL extension is **disabled by default**. To enable it, add the following configuration to your application:

```yaml
geoserver:
  extension:
    security:
      acl:
        enabled: true
```

For backward compatibility, the extension also supports configuration through the legacy property:

```yaml
geoserver:
  acl:
    enabled: true
```

### Complete Configuration Example

```yaml
geoserver:
  acl:
    enabled: true
    client:
      basePath: http://acl:8080/acl/api
      username: geoserver
      password: s3cr3t
      debug: false
      caching: true
      initTimeout: 10
```

## Using with Spring Profiles

For convenience, you can also enable ACL by activating the `acl` Spring profile, which is preconfigured in `geoserver.yml`:

```
spring.profiles.active=acl
```

This will enable the ACL extension and provide default configuration values.

## Implementation Details

This extension module:
- Provides a consistent configuration interface for the GeoServer ACL integration
- Relies on the GeoServer ACL plugin for the actual implementation
- Integrates with the Spring Cloud event bus for cache invalidation

The extension depends on the following GeoServer ACL components:
- `gs-acl-client-plugin` - The main client plugin for integrating with GeoServer
- `gs-acl-cache` - Cache implementation for ACL rules
- `gs-acl-spring-cloud-bus` - Spring Cloud event bus integration for cache synchronization

## References

- [GeoServer ACL Project](https://github.com/geoserver/geoserver-acl)
- [GeoServer ACL Documentation](https://github.com/geoserver/geoserver-acl/blob/main/README.md)