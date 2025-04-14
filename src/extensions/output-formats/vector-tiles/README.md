# Vector Tiles Extension

Auto-configuration for GeoServer Vector Tiles extension.

## Features

This extension:
- Provides auto-configuration for Vector Tiles output format support in multiple GeoServer services
- Adds support for MapBox, GeoJSON, and TopoJSON vector tile formats
- Conditionally enables or disables individual formats based on configuration properties
- Serves as an example of a module required by multiple services (WMS, WebUI, and GWC)

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    vector-tiles:
      enabled: true   # Enable/disable vector tiles extension (default: true)
      mapbox: true    # Enable/disable MapBox vector tiles (default: true)
      geojson: true   # Enable/disable GeoJSON vector tiles (default: true)
      topojson: true  # Enable/disable TopoJSON vector tiles (default: true)
```

## Implementation Details

The extension uses Spring Boot auto-configuration to conditionally register Vector Tiles support. Key classes:

- `VectorTilesAutoConfiguration`: Main auto-configuration class that controls the overall extension
- `VectorTilesConfigProperties`: Configuration properties class with settings for each format
- `ConditionalOnVectorTiles`: Conditional annotation for enabling the extension
- Service-specific configurations:
  - `WMSConfiguration`: Activates vector tiles formats in the WMS service
  - `WebUIConfiguration`: Activates vector tiles formats in the Web UI for layer previews
  - `GWCConfiguration`: Activates vector tiles formats in GeoWebCache for tile caching
- Format-specific configurations:
  - `MapBoxConfiguration`: Configuration for MapBox vector tiles
  - `GeoJsonConfiguration`: Configuration for GeoJSON vector tiles
  - `TopoJsonConfiguration`: Configuration for TopoJSON vector tiles

The extension registers each vector tile format when:
1. The corresponding GeoServer service is available (WMS, WebUI, or GWC)
2. The main extension is enabled (`geoserver.extension.vector-tiles.enabled=true`)
3. The specific format is enabled (e.g., `geoserver.extension.vector-tiles.mapbox=true`)

## Multi-Service Usage

This extension demonstrates how a single module can be required by multiple GeoServer services:

### WMS Service
Vector tiles can be requested directly from the WMS service:
- MapBox vector tiles: `format=application/vnd.mapbox-vector-tile`
- GeoJSON vector tiles: `format=application/json;type=geojson`
- TopoJSON vector tiles: `format=application/json;type=topojson`

Example WMS request:
```
http://localhost:8080/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=topp:states&styles=&bbox=-124.731,24.955,-66.97,49.371&width=780&height=330&srs=EPSG:4326&format=application/vnd.mapbox-vector-tile
```

### Web UI Integration
The Web UI service uses these formats to:
- Display vector tiles in the Layer Preview page
- Provide format options in the Layer Preview dropdown
- Enable interactive vector tile viewing through OpenLayers

### GeoWebCache Integration
GWC can create and serve cached vector tiles:
- Creates tile caches in vector formats (MapBox, GeoJSON, TopoJSON)
- Serves cached vector tiles with improved performance
- Supports the same formats as direct WMS requests

This multi-service integration demonstrates how GeoServer Cloud's modular architecture allows extensions to be used across different services while maintaining separation of concerns.

## Related Documentation

- [GeoServer Vector Tiles](https://docs.geoserver.org/latest/en/user/extensions/vectortiles/index.html)
- [MapBox Vector Tile Specification](https://github.com/mapbox/vector-tile-spec)