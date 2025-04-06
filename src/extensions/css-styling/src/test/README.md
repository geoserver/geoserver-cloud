# CSS Styling Extension Tests

## Test Strategy

The tests for the CSS Styling extension focus on testing the auto-configuration conditions
required for CSS styling support.

### ConditionalOnCssStyling 

The `ConditionalOnCssStyling` annotation includes:
- `@ConditionalOnGeoServerWMS` - Requiring WMS components to be available
- `@ConditionalOnProperty` - Checking if CSS styling is enabled in configuration

### CssStylingAutoConfigurationTest

The test class creates a Spring web application context with all required beans to satisfy
the conditional requirements:

- `extensions` - For GeoServer extensions support
- `geoServer` - Mock GeoServer instance to satisfy `@ConditionalOnGeoServer`
- `wmsServiceTarget` - Mock DefaultWebMapService to satisfy `@ConditionalOnGeoServerWMS`
- `sldHandler` - Required for CSS handler creation

This approach allows testing the CSS styling autoconfiguration under conditions close to
the real deployment environment without needing an actual running GeoServer instance.