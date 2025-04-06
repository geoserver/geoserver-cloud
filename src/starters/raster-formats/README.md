# GeoServer Cloud Raster Formats Starter

This Spring Boot starter provides fine-grained control over which GeoTools `GridFormatFactorySpi` implementations are available in a GeoServer Cloud application.

## Features

- Filtering system for raster format factories using a custom FactoryCreator wrapper
- Configurable through YAML/properties files using user-friendly format names
- Support for placeholder resolution for format enabled/disabled values
- Filtering persists even when formats are reloaded via GridFormatFinder.scanForPlugins()
- Detailed logging of which formats are enabled/disabled

## Usage

Simply include this starter in your GeoServer Cloud application's dependencies:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-starter-raster-formats</artifactId>
</dependency>
```

## Configuration

Configure which raster formats are enabled by adding configuration to your `application.yml` or `application.properties` file:

```yaml
geotools:
  data:
    filtering:
      # Master switch for the filtering system
      enabled: true
      # Configure individual raster formats
      raster-formats:
        "[GeoTIFF]": true
        "[ImageMosaic]": ${mosaic.enabled:true}
        "[ArcGrid]": false
        "[WorldImage]": true
        "[ImagePyramid]": true
```

The configuration supports Spring property placeholders, allowing you to reference other configuration properties.

## How it Works

The starter uses the following components:

1. **GridFormatFactoryFilterProcessor**: Intercepts and wraps the GridFormatFinder registry
2. **FilteringFactoryCreator**: Wraps the original FactoryCreator to filter out disabled formats
3. **GridFormatFactoryFilterConfigProperties**: Manages configuration properties for format filtering
4. **GridFormatFactoryFilteringAutoConfiguration**: Spring Boot auto-configuration for the filtering system

The filtering system runs before GeoServerBackendAutoConfiguration to ensure formats are filtered before catalog initialization.