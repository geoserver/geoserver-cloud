# GeoServer Security Extensions

This module provides auto-configuration for various GeoServer security extensions.

## Extensions

The following security extensions are available:

- **GeoNode OAuth2**: Authentication with GeoNode OAuth2 provider

## Configuration

Each security extension has its own configuration properties. See the individual extension READMEs for details:

- [GeoNode OAuth2](geonode-oauth2/README.md)

## Implementation Details

The security extensions use Spring Boot's auto-configuration mechanism to conditionally enable security features in GeoServer Cloud.

Each extension follows the same pattern:
- Auto-configuration class that imports the necessary beans
- Configuration properties class with the extension's settings
- Conditional annotations to enable/disable the extension