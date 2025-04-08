# Vector Data Formats Auto-configuration

This module provides Spring Boot auto-configuration for GeoTools vector data format implementations.

## Features

This auto-configuration module:
- Provides configuration properties for vector data format filtering
- Implements a mechanism to filter available DataAccessFactory implementations
- Enables conditional configuration based on available classes and properties

## Usage

This module is typically used indirectly through the `gs-cloud-starter-vector-formats` starter.

For direct usage, add this module as a dependency:

```xml
<dependency>
  <groupId>org.geoserver.cloud.extensions</groupId>
  <artifactId>gs-cloud-extension-vector-formats</artifactId>
</dependency>
```

Vector data format implementations must be provided separately.

## Configuration Properties

The following YAML shows **example** configuration (not defaults). By default, all formats are enabled when no configuration is provided:

```yaml
geotools.data.filtering:
  # Master switch for filtering, enabled by default
  enabled: true
  
  # EXAMPLE: Configure specific vector formats by their display names
  vector-formats:
    # Use display names with proper escaping for special characters
    "[Shapefile]": true
    "[PostGIS]": true
    "[GeoPackage]": true
    "[Oracle NG]": false
    "[Web Feature Server (NG)]": false
    "[Generalizing data store]": ${geoserver.extension.pregeneralized.enabled:false}
```

## Auto-configuration Classes

- `DataAccessFactoryFilteringAutoConfiguration`: Main auto-configuration class
- `DataAccessFactoryFilterConfigProperties`: Configuration properties
- `DataAccessFactoryFilterProcessor`: Implementation of the filtering mechanism