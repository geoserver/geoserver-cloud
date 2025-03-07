# INSPIRE Extension

Auto-configuration for GeoServer INSPIRE extension.

## Features

This extension:
- Provides auto-configuration for INSPIRE support in GeoServer
- Conditionally enables or disables based on configuration properties

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    inspire:
      enabled: false  # Enable/disable INSPIRE extension (default: false)
```

## Implementation Details

The extension uses Spring Boot auto-configuration to conditionally register INSPIRE support. Key classes:

- `InspireAutoConfiguration`: Main auto-configuration class that controls the INSPIRE extension
- `InspireConfigProperties`: Configuration properties class for INSPIRE settings
- `ConditionalOnInspire`: Composite conditional annotation for enabling INSPIRE support

The extension registers the INSPIRE extension when:
1. The `geoserver.extension.inspire.enabled` property is `true` (default is false)

## Related Documentation

- [GeoServer INSPIRE User Guide](https://docs.geoserver.org/main/en/user/extensions/inspire/index.html)
