# JDBC Security Extension

Auto-configuration for GeoServer JDBC security extension.

## Features

This extension:
- Enables authentication and authorization using JDBC databases
- Provides JDBC-based user/group and role services
- Supports configuration through Spring properties

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    security:
      jdbc:
        enabled: true  # Enable/disable JDBC security extension (default: true)
```

Backward compatibility is maintained with legacy properties:
```yaml
geoserver:
  security:
    jdbc: true
```

## Implementation Details

The extension uses Spring Boot auto-configuration to register the JDBC security provider:

- `JDBCSecurityAutoConfiguration`: Main auto-configuration class
- `JDBCConfigProperties`: Configuration properties
- `ConditionalOnJDBC`: Composite conditional annotation

When the Web UI is present, it also registers:
- `JDBCSecurityWebUIAutoConfiguration`: Web UI components configuration

## Usage

Once enabled, the JDBC security extension allows GeoServer to authenticate users against
a database and manage groups and roles through JDBC.

## Web UI Integration

When the GeoServer Web UI is available, the extension also provides configuration panels
for JDBC-based user/group services, role services, and authentication providers.
