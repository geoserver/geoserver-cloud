# FlatGeobuf Extension

This module provides support for the FlatGeobuf output format in GeoServer Cloud. FlatGeobuf is an efficient binary encoding format for geographic data based on Google's FlatBuffers.

## Features

The FlatGeobuf extension adds:
- Support for FlatGeobuf as a WFS output format
- Integration with WebUI for layer preview and WFS admin page
- Efficient binary encoding for geographic data with random access capabilities
- Demonstrates a module required by multiple services (WFS and WebUI)

## Configuration

The FlatGeobuf extension is enabled by default but can be controlled through the following configuration property:

```yaml
geoserver:
  extension:
    flatgeobuf:
      enabled: true  # Set to false to disable
```

## Multi-Service Usage

This extension demonstrates how a single module can be required by multiple GeoServer services:

### WFS Service
FlatGeobuf can be requested directly from the WFS service:
```
outputFormat=flatgeobuf
```

Example WFS request:
```
http://localhost:8080/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeName=topp:states&outputFormat=flatgeobuf
```

### Web UI Integration
The WebUI service uses FlatGeobuf in multiple ways:
- Shows FlatGeobuf as an available format in the Layer Preview page
- Provides configuration options in the WFS admin page
- Enables proper UI display of FlatGeobuf capabilities

This multi-service integration demonstrates how GeoServer Cloud's modular architecture allows extensions to be used across different services while maintaining separation of concerns. The implementation uses service-specific inner configuration classes that are conditionally activated based on which services are present in the application.

## Implementation Details

The extension uses Spring Boot auto-configuration with service-specific configurations:
- `FlatGeobufOutputFormatConfiguration` - Activates support in the WFS service
- `WebUIConfiguration` - Activates support in the WebUI service

Each configuration is conditionally enabled based on which services are available in the deployment, allowing the extension to adapt to different service combinations.

## Dependencies

This extension depends on:
- `org.geotools:gt-flatgeobuf` - For the FlatGeobuf data format
- `org.geoserver:gs-flatgeobuf` - For the GeoServer FlatGeobuf integration
- GeoServer WFS module (for format support)
- GeoServer WebUI module (for UI integration)