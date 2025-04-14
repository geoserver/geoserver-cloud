# FlatGeobuf Extension

This module provides support for the FlatGeobuf output format in GeoServer Cloud. FlatGeobuf is an efficient binary encoding format for geographic data based on Google's FlatBuffers.

## Features

The FlatGeobuf extension adds:
- Support for FlatGeobuf as a WFS output format
- Efficient binary encoding for geographic data with random access capabilities

## Configuration

The FlatGeobuf extension is enabled by default but can be controlled through the following configuration property:

```yaml
geoserver:
  extension:
    flatgeobuf:
      enabled: true  # Set to false to disable
```

## Usage

When enabled, the FlatGeobuf output format will be available for WFS GetFeature requests. You can specify it in requests by using:

```
outputFormat=flatgeobuf
```

For example:
```
http://localhost:8080/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeName=topp:states&outputFormat=flatgeobuf
```

## Dependencies

This extension depends on:
- `org.geotools:gt-flatgeobuf` - For the FlatGeobuf data format
- `org.geoserver:gs-flatgeobuf` - For the GeoServer FlatGeobuf integration
- GeoServer WFS module