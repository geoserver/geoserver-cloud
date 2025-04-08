# Vector Tiles Extension

Auto-configuration for GeoServer Vector Tiles extension.

## Features

This extension:
- Provides auto-configuration for Vector Tiles output format support in GeoServer WMS
- Adds support for MapBox, GeoJSON, and TopoJSON vector tile formats
- Conditionally enables or disables individual formats based on configuration properties

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
- Format-specific configurations:
  - `MapBoxConfiguration`: Configuration for MapBox vector tiles
  - `GeoJsonConfiguration`: Configuration for GeoJSON vector tiles
  - `TopoJsonConfiguration`: Configuration for TopoJSON vector tiles

The extension registers each vector tile format when:
1. GeoServer WMS is available in the application context
2. The main extension is enabled (`geoserver.extension.vector-tiles.enabled=true`)
3. The specific format is enabled (e.g., `geoserver.extension.vector-tiles.mapbox=true`)

## Usage

Once enabled, vector tiles can be requested from GeoServer WMS service using the appropriate output format:

- MapBox vector tiles: `format=application/vnd.mapbox-vector-tile`
- GeoJSON vector tiles: `format=application/json;type=geojson`
- TopoJSON vector tiles: `format=application/json;type=topojson`

Example request:
```
http://localhost:8080/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=topp:states&styles=&bbox=-124.731,24.955,-66.97,49.371&width=780&height=330&srs=EPSG:4326&format=application/vnd.mapbox-vector-tile
```

## Related Documentation

- [GeoServer Vector Tiles](https://docs.geoserver.org/latest/en/user/extensions/vectortiles/index.html)
- [MapBox Vector Tile Specification](https://github.com/mapbox/vector-tile-spec)