# Vector Data Formats Auto-configuration

This module provides Spring Boot auto-configuration for GeoTools vector data format implementations.

## Features

This auto-configuration module:
- Provides configuration properties for vector data format filtering
- Implements a mechanism to filter available DataAccessFactory implementations
- Enables conditional configuration based on available classes and properties
- Supports specialized vector data formats like Graticule and FlatGeobuf

## Supported Formats

The module includes support for several vector data formats:

### Standard Data Formats
- Shapefile
- PostGIS
- GeoPackage
- Oracle
- SQL Server
- WFS

### Specialized Data Formats
- Pre-generalized Features (provides simplified geometries for different scale levels)
- Graticule (generates latitude/longitude grid lines)
- FlatGeobuf (efficient binary format with random access)

## Graticule Extension

The Graticule extension allows creating a data store that produces latitude/longitude graticule lines.

### Features

The graticule extension provides:
- A data store type for creating latitude/longitude grid lines
- Support for customizable grid spacing
- Integration with GeoServer Web UI for configuring graticule stores

### Configuration

The Graticule data store is integrated with the vector formats filtering mechanism. It's available by default unless specifically disabled:

```yaml
geotools.data.filtering:
  enabled: true
  vector-formats:
    "[Graticule]": false  # Set to false to disable
```

The UI components for the Graticule data store are automatically enabled when the GeoServer Web UI is present and the data store is available.

### Usage

When available, the graticule store type will be listed when adding a new data store in the GeoServer Web UI:

1. Navigate to "Stores" > "Add new Store"
2. Select "Graticule" from the list of vector data sources
3. Configure the graticule properties (spacing, etc.)
4. Use the resulting layer in your maps

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
    "[Graticule]": true
    "[FlatGeobuf]": true
    "[Generalizing data store]": ${geoserver.extension.pregeneralized.enabled:false}
```

## Auto-configuration Classes

- `DataAccessFactoryFilteringAutoConfiguration`: Main auto-configuration class
- `DataAccessFactoryFilterConfigProperties`: Configuration properties
- `DataAccessFactoryFilterProcessor`: Implementation of the filtering mechanism