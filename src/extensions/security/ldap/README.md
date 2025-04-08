# LDAP Security Extension

Auto-configuration for GeoServer LDAP security extension.

## Features

This extension:
- Enables authentication and authorization using LDAP directories
- Provides LDAP-based user/group and role services
- Supports configuration through Spring properties

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    security:
      ldap:
        enabled: true  # Enable/disable LDAP security extension (default: true)
```

Backward compatibility is maintained with legacy properties:
```yaml
geoserver:
  security:
    ldap: true
```

## Implementation Details

The extension uses Spring Boot auto-configuration to register the LDAP security provider:

- `LDAPSecurityAutoConfiguration`: Main auto-configuration class
- `LDAPConfigProperties`: Configuration properties
- `ConditionalOnLDAP`: Composite conditional annotation

When the Web UI is present, it also registers:
- `LDAPSecurityWebUIAutoConfiguration`: Web UI components configuration

## Usage

Once enabled, the LDAP security extension allows GeoServer to authenticate users against
LDAP directories and manage groups and roles through LDAP.

## Web UI Integration

When the GeoServer Web UI is available, the extension also provides configuration panels
for LDAP-based user/group services, role services, and authentication providers.
