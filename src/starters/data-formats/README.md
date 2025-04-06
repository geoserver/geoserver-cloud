# GeoServer Cloud Data Formats Starter

This module provides a unified starter for all supported GeoServer data formats, both vector and raster.

## Overview

The `gs-cloud-starter-data-formats` starter combines both vector and raster data format extensions into a single dependency, making it easier for applications to include all supported data formats without having to manually include separate dependencies.

## Usage

To use this starter in your GeoServer Cloud application, add the following dependency:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-starter-data-formats</artifactId>
</dependency>
```

## Included Formats

This starter includes all formats from:

### Vector Formats
- Shapefile
- PostGIS
- GeoPackage
- Web Feature Server (WFS)
- Oracle
- SQL Server
- And more...

### Raster Formats
- GeoTIFF
- ArcGrid
- ImageMosaic
- ImagePyramid
- WorldImage
- GeoPackage (mosaic)
- And more...

## Configuration

Format availability can be controlled through the `geotools.data.filtering` configuration settings in `geoserver.yml`. See the documentation for specific formats for more details.

## Extension Point

This starter is designed to be a convenient way to include all standard data formats. For applications that need to be more selective about which formats to include, the individual starters `gs-cloud-starter-vector-formats` and `gs-cloud-starter-raster-formats` remain available.