# GeoNode OAuth2 Extension

Auto-configuration for GeoServer GeoNode OAuth2 authentication extension.

## Features

This extension:
- Provides auto-configuration for GeoNode OAuth2 authentication in GeoServer
- Integrates with GeoServer's security subsystem
- Enables authentication using GeoNode as an OAuth2 provider

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    security:
      geonode-oauth2:
        enabled: true  # Enable/disable GeoNode OAuth2 authentication (default: true)
```

## Implementation Details

The extension uses Spring Boot auto-configuration to register GeoNode OAuth2 authentication support. Key classes:

- `GeoNodeOAuth2AutoConfiguration`: Main auto-configuration class that imports application context resources
- `GeoNodeOAuth2ConfigProperties`: Configuration properties class for GeoNode OAuth2 settings (planned)
- `ConditionalOnGeoNodeOAuth2`: Composite conditional annotation for enabling GeoNode OAuth2 support (planned)

## Usage

Once enabled, you can configure GeoNode OAuth2 authentication in GeoServer through the web UI or REST API.

## Related Documentation

- [GeoServer OAuth2 Documentation](https://docs.geoserver.org/latest/en/user/security/oauth2/index.html)