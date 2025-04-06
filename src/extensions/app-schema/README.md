# App-Schema Extension

Auto-configuration for GeoServer App-Schema extension.

## Features

This extension:
- Provides auto-configuration for App-Schema data store support in GeoServer
- Enables complex feature mapping using XML-based application schemas
- Conditionally enables or disables based on configuration properties

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    appschema:
      enabled: false  # Enable/disable App-Schema extension (default: false)
```

## Implementation Details

The extension uses Spring Boot auto-configuration to conditionally register App-Schema support. Key classes:

- `AppSchemaAutoConfiguration`: Main auto-configuration class that controls the App-Schema extension
- `AppSchemaConfigProperties`: Configuration properties class for App-Schema settings
- `ConditionalOnAppSchema`: Composite conditional annotation for enabling App-Schema support

The extension registers the AppSchema data store when:
1. The `geoserver.extension.appschema.enabled` property is `true` (default is false)

## Usage

Once enabled, the AppSchema extension allows GeoServer to serve complex features by mapping simple features to complex schemas.

App-Schema uses XML mapping files to define how simple features (from relational databases or other sources) map to complex features with nested structures.

Example use cases include:
- INSPIRE compliance
- GML complex feature support
- Data model transformation to standard schemas

## Related Documentation

- [GeoServer App-Schema User Guide](https://docs.geoserver.org/latest/en/user/data/app-schema/index.html)
- [App-Schema Tutorial](https://docs.geoserver.org/latest/en/user/data/app-schema/tutorial.html)