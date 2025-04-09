# GeoServer Cloud Extensions Starter

This starter module provides a convenient way to include all supported GeoServer Cloud extensions in your applications. It aggregates multiple extension starters, offering a comprehensive set of additional functionality with a single dependency.

## Overview

The Extensions Starter:

- Groups all GeoServer Cloud extensions into a single, easy-to-use starter
- Provides consistent configuration across different extension types
- Allows common extensions to be included without enumerating them individually
- Maintains compatibility with future extensions through modular design

## Included Extension Types

This starter includes the following extension categories:

1. **Styling Extensions**
     - CSS Styling - Alternative CSS-based styling language
     - MapBox Styling - MapBox Vector Tiles styling support

2. **Security Extensions** (through `gs-cloud-starter-security`)
     - Auth Key - Authentication using API keys
     - Gateway Shared Auth - Authentication sharing through Gateway
     - GeoNode OAuth2 - Integration with GeoNode authentication
     - GeoServer ACL - Fine-grained access control
     - JDBC Security - Database-backed security configuration
     - LDAP - LDAP/ActiveDirectory authentication
     - Environment Admin - Admin credentials via environment variables

3. **Output Format Extensions** (through `gs-cloud-starter-output-formats`)
     - Vector Tiles - Support for vector tile formats (Mapbox, GeoJSON, TopoJSON)

4. **Data Format Extensions**
     - App Schema - Complex feature types support

## Usage

To include all extensions in your GeoServer Cloud application, add the following dependency to your pom.xml:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-starter-extensions</artifactId>
</dependency>
```

All GeoServer microservice applications automatically include this starter through their parent POM.

## Configuration

Each extension can be individually enabled or disabled through configuration properties. Most extensions are disabled by default and must be explicitly enabled.

Example configuration in `application.yml` or `geoserver.yml`:

```yaml
geoserver:
  extension:
    css-styling:
      enabled: true
    mapbox-styling:
      enabled: true
    vector-tiles:
      enabled: true
      mapbox: true
      geojson: true
    security:
      auth-key:
        enabled: false
      environment-admin:
        enabled: true
```

## Extension Management

This starter follows these principles:

1. **Modularity**: Each extension can function independently
2. **Selective Activation**: Extensions are loaded but inactive until explicitly enabled
3. **Conditional Processing**: Auto-configurations use Spring Boot's conditional processing
4. **Consistent Configuration**: All extensions use the same configuration prefix pattern

## For Extension Developers

When adding a new extension to GeoServer Cloud:

1. Create the extension in the appropriate category under `src/extensions/`
2. Add it to the respective starter module (e.g., `output-formats`, `security`)
3. The extensions starter will automatically include it through dependencies on other starters

For more details, see the [Adding Extensions Guide](../../docs/develop/extensions/adding_extensions.md).