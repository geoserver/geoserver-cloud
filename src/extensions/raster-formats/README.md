# Raster Data Formats Auto-configuration

This module provides Spring Boot auto-configuration for GeoTools raster data format implementations and Cloud Optimized GeoTIFF (COG) support.

## Features

This auto-configuration module:
- Provides configuration properties for raster data format filtering
- Implements a mechanism to filter available GridFormatFactory implementations
- Auto-configures COG format support when dependencies are available
- Enables conditional configuration based on available classes and properties

## Usage

This module is typically used indirectly through the `gs-cloud-starter-raster-formats` starter.

For direct usage, add this module as a dependency:

```xml
<dependency>
  <groupId>org.geoserver.cloud.extensions</groupId>
  <artifactId>gs-cloud-extension-raster-formats</artifactId>
</dependency>
```

Raster data format implementations must be provided separately.

## Configuration Properties

The following YAML shows **example** configuration (not defaults). By default, all formats are enabled when no configuration is provided:

```yaml
geotools.data.filtering:
  # Master switch for filtering, enabled by default
  enabled: true
  
  # EXAMPLE: Configure specific raster formats by their display names
  raster-formats:
    # Use display names with proper escaping for special characters
    "[GeoTIFF]": true
    "[ArcGrid]": true
    "[ImageMosaic]": true
    "[WorldImage]": false
```

## Auto-configuration Classes

- `GridFormatFactoryFilteringAutoConfiguration`: Main auto-configuration class for raster formats
- `GridFormatFactoryFilterConfigProperties`: Configuration properties
- `GridFormatFactoryFilterProcessor`: Implementation of the filtering mechanism
- `COGAutoConfiguration`: Auto-configuration for Cloud Optimized GeoTIFF support
- `COGWebUIAutoConfiguration`: Auto-configuration for COG UI components when web-ui is present