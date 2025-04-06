# MapBox Styling Extension

Auto-configuration for GeoServer MapBox Styling extension.

## Features

This extension:
- Provides auto-configuration for MapBox Styling format support in GeoServer
- Adds support for styling maps using MapBox style JSON syntax
- Conditionally enables or disables based on configuration properties

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    mapbox-styling:
      enabled: true  # Enable/disable MapBox styling (default: true)
```

## Implementation Details

The extension uses Spring Boot auto-configuration to conditionally register MapBox styling support. Key classes:

- `MapBoxStylingAutoConfiguration`: Main auto-configuration class that imports application context resources based on conditions
- `MapBoxStylingConfigProperties`: Configuration properties class for MapBox styling settings
- `ConditionalOnMapBoxStyling`: Composite conditional annotation for enabling MapBox styling

The extension registers the required beans for MapBox styling when:
1. GeoServer WMS is available in the application context
2. The `geoserver.extension.mapbox-styling.enabled` property is `true` (default)

## Usage

Once enabled, MapBox styles can be used to style layers in GeoServer. MapBox styling uses JSON format which is familiar to many web developers.

Example MapBox style:
```json
{
  "version": 8,
  "name": "Basic",
  "layers": [
    {
      "id": "landuse",
      "type": "fill",
      "paint": {
        "fill-color": "#f8f4f0"
      }
    }
  ]
}
```

## Related Documentation

- [GeoServer MapBox Styling](https://docs.geoserver.org/latest/en/user/styling/mbstyle/index.html)