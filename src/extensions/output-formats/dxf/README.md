# DXF Extension

This module provides support for the DXF output format in GeoServer Cloud. DXF (Drawing Exchange Format) is a CAD data file format developed by Autodesk for enabling data interoperability between AutoCAD and other programs.

## Features

The DXF extension adds:
- Support for DXF as a WFS output format
- Integration with WebUI for layer preview and WFS admin page
- Export vector data in CAD-compatible format
- Demonstrates a module required by multiple services (WFS and WebUI)

## Configuration

The DXF extension is enabled by default but can be controlled through the following configuration property:

```yaml
geoserver:
  extension:
    dxf:
      enabled: true  # Set to false to disable
```

## Multi-Service Usage

This extension demonstrates how a single module can be required by multiple GeoServer services:

### WFS Service
DXF can be requested directly from the WFS service:
```
outputFormat=dxf
```

Example WFS request:
```
http://localhost:8080/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeName=topp:states&outputFormat=dxf
```

### Web UI Integration
The WebUI service uses DXF in multiple ways:
- Shows DXF as an available format in the Layer Preview page
- Provides configuration options in the WFS admin page
- Enables proper UI display of DXF capabilities

This multi-service integration demonstrates how GeoServer Cloud's modular architecture allows extensions to be used across different services while maintaining separation of concerns. The implementation uses service-specific inner configuration classes that are conditionally activated based on which services are present in the application.

## Implementation Details

The extension uses Spring Boot auto-configuration with service-specific configurations:
- `DxfOutputFormatConfiguration` - Activates support in the WFS service
- `WebUIConfiguration` - Activates support in the WebUI service

Each configuration is conditionally enabled based on which services are available in the deployment, allowing the extension to adapt to different service combinations.

## Dependencies

This extension depends on:
- `org.geoserver:gs-dxf-core` - For the GeoServer DXF integration
- GeoServer WFS module (for format support)
- GeoServer WebUI module (for UI integration)