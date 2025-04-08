# CSS Styling Extension

Auto-configuration for GeoServer CSS Styling extension.

## Features

This extension:
- Provides auto-configuration for CSS Styling format support in GeoServer
- Adds support for styling maps using CSS syntax
- Conditionally enables or disables based on configuration properties

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    css-styling:
      enabled: true  # Enable/disable CSS styling (default: true)
```

## Implementation Details

The extension uses Spring Boot auto-configuration to conditionally register CSS styling support. Key classes:

- `CssStylingAutoConfiguration`: Main auto-configuration class that imports application context resources based on conditions
- `CssStylingConfigProperties`: Configuration properties class for CSS styling settings
- `ConditionalOnCssStyling`: Composite conditional annotation for enabling CSS styling

The extension registers the required beans for CSS styling when:
1. GeoServer WMS is available in the application context
2. The `geoserver.extension.css-styling.enabled` property is `true` (default)

## Usage

Once enabled, CSS stylesheets can be used to style layers in GeoServer. CSS provides a more intuitive styling syntax compared to SLD.

Example CSS style:
```css
* {
  fill: #3399CC;
  stroke: #000000;
  stroke-width: 0.5;
}
```

## Related Documentation

- [GeoServer CSS Styling](https://docs.geoserver.org/latest/en/user/styling/css/index.html)