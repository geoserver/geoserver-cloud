# Input Data Formats Starter

This module provides a unified starter for all vector and raster input data format extensions in GeoServer Cloud.

## Overview

The `gs-cloud-starter-input-formats` module is a Spring Boot starter that:

- Includes dependencies for all supported vector and raster input data formats
- Integrates with GeoServer's data access architecture
- Provides a single dependency for applications to include all input format support
- Enables selective filtering of available data formats based on configuration

## Included Extensions

This starter includes the following extensions:

### Vector Data Formats
- Shapefile
- PostGIS
- GeoPackage
- Oracle (when enabled)
- SQL Server
- WFS
- FlatGeobuf
- Graticule
- Pre-generalized Features

### Raster Data Formats
- GeoTIFF
- Cloud Optimized GeoTIFF (COG)
- ImageMosaic
- ArcGrid
- WorldImage
- ImagePyramid

## Usage

Add the starter to your GeoServer Cloud application:

```xml
<dependency>
  <groupId>org.geoserver.cloud.starters</groupId>
  <artifactId>gs-cloud-starter-input-formats</artifactId>
</dependency>
```

## Configuration

### Data Format Filtering

GeoServer Cloud provides the ability to filter which input formats are available in the application. This allows you to customize each deployment to only include the formats you need, improving security, reducing the attack surface, and potentially improving startup time.

#### Enable/Disable Filtering

The filtering system is controlled by a master switch:

```yaml
geotools:
  data:
    filtering:
      # Master switch for the entire filtering system
      enabled: true  # Set to false to disable filtering
```

#### Vector Format Configuration

Configure which vector formats (DataAccessFactory implementations) are available:

```yaml
geotools:
  data:
    filtering:
      vector-formats:
        "[PostGIS]": true
        "[Shapefile]": true
        "[GeoPackage]": true
        "[Oracle NG]": ${oracle.enabled:false}
        "[Web Feature Server (NG)]": true
        "[Microsoft SQL Server]": false
        "[FlatGeobuf]": true
        "[Graticule]": true
        # Add more vector format entries as needed
```

#### Raster Format Configuration

Configure which raster formats (GridFormatFactorySpi implementations) are available:

```yaml
geotools:
  data:
    filtering:
      raster-formats:
        "[GeoTIFF]": true
        "[ImageMosaic]": ${mosaic.enabled:true}
        "[ArcGrid]": false
        "[WorldImage]": true
        "[ImagePyramid]": false
        # Add more raster format entries as needed
```

### Format Names

The format names used in the configuration are the user-friendly display names returned by the respective factories:

- For vector formats: The name returned by `DataAccessFactory.getDisplayName()`
- For raster formats: The name returned by `AbstractGridFormat.getName()`

Since these names often contain special characters, they should be properly escaped in the YAML configuration using quotes and brackets.

### Placeholder Resolution

Both vector and raster format configurations support Spring property placeholder resolution, allowing you to create dynamic configurations using environment variables or system properties. For example:

```yaml
vector-formats:
  "[Oracle NG]": ${oracle.enabled:false}
  "[PostGIS]": ${postgis.enabled:true}
```

## Implementation Details

The filtering system uses different approaches for vector and raster formats:

- **Vector formats**: Directly deregisters disabled DataAccessFactory implementations using `DataAccessFinder.deregisterFactory()` and `DataStoreFinder.deregisterFactory()`
- **Raster formats**: Uses a custom FilteringFactoryCreator wrapper around the standard GridFormatFinder registry to filter formats on-the-fly

## Extension Documentation

For more detailed information about specific input format extensions:

- Vector Formats: See the [Vector Formats README](/src/extensions/input-formats/vector-formats/README.md)
- Raster Formats: See the [Raster Formats README](/src/extensions/input-formats/raster-formats/README.md)