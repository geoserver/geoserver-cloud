# GeoServer Cloud Output Formats Starter

This starter module provides a convenient way to include all output format extensions in your GeoServer Cloud applications.

## Included Extensions

This starter includes the following output format extensions:

- **Vector Tiles**: Provides support for various vector tile formats:
  - Mapbox Vector Tiles
  - GeoJSON Vector Tiles
  - TopoJSON Vector Tiles

## Usage

To include all output format extensions in your GeoServer Cloud application, add the following dependency to your pom.xml:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-starter-output-formats</artifactId>
</dependency>
```

## Configuration

These extensions can be configured in your application's configuration using the following properties:

```yaml
geoserver:
  extension:
    vector-tiles:
      enabled: true  # Enable/disable vector tiles support
      mapbox: true   # Enable Mapbox vector tiles format
      geojson: true  # Enable GeoJSON vector tiles format
      topojson: true # Enable TopoJSON vector tiles format
```

All formats are enabled by default.