# Authentication Key Extension

Auto-configuration for GeoServer Authentication Key extension.

## Features

This extension:
- Enables authentication via URL parameters or HTTP header tokens
- Supports property file, user property, and web service authentication key mapping
- Allows for custom authentication key filters in GeoServer

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    security:
      auth-key:
        enabled: true  # Enable/disable Auth Key extension (default: true)
```

Backward compatibility is maintained with legacy properties:
```yaml
geoserver:
  security:
    authkey: true
```

## Implementation Details

The extension uses Spring Boot auto-configuration to register the Authentication Key provider:

- `AuthKeyAutoConfiguration`: Main auto-configuration class
- `AuthKeyConfigProperties`: Configuration properties
- `ConditionalOnAuthKey`: Composite conditional annotation

## Usage

Once enabled, the Auth Key extension allows GeoServer to authenticate users with tokens passed via:
- URL parameters
- HTTP headers
- Cookies

This is particularly useful for machine-to-machine authentication and OGC service integrations.

## Web UI Integration

When the GeoServer Web UI is available, the extension also provides configuration panels
for setting up authentication key filters and providers.
