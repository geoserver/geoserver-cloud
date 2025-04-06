# GeoServer Cloud Vector Formats Starter

This Spring Boot starter provides fine-grained control over which GeoTools `DataAccessFactory` implementations are available in a GeoServer Cloud application.

## Features

- Filtering system to selectively enable/disable vector data formats
- Configurable through YAML/properties files using user-friendly format names
- Support for placeholder resolution for format enabled/disabled values
- Direct deregistration of disabled DataAccessFactory instances
- Detailed logging of which formats are enabled/disabled

## Usage

Simply include this starter in your GeoServer Cloud application's dependencies:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-starter-vector-formats</artifactId>
</dependency>
```

## Configuration

Configure which data formats are enabled by adding configuration to your `application.yml` or `application.properties` file:

```yaml
geotools:
  data:
    filtering:
      # Master switch for the filtering system
      enabled: true
      # Configure individual vector formats
      vector-formats:
        "[PostGIS]": true
        "[Shapefile]": true
        "[Oracle NG]": ${oracle.enabled:false}
        "[Web Feature Server (NG)]": true
```

The configuration supports Spring property placeholders, allowing you to reference other configuration properties.

## How it Works

The starter uses the following components:

1. **DataAccessFactoryFilterProcessor**: Identifies and deregisters disabled DataAccessFactory implementations
2. **DataAccessFactoryFilterConfigProperties**: Manages configuration properties for format filtering
3. **DataAccessFactoryFilteringAutoConfiguration**: Spring Boot auto-configuration for the filtering system

The filtering system runs before GeoServerBackendAutoConfiguration to ensure formats are filtered before catalog initialization.