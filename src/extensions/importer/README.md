# GeoServer Importer Extension

This module integrates the GeoServer Importer extension with GeoServer Cloud.

## Overview

The Importer extension provides a GUI and a REST API for uploading and configuring new vector and raster data layers in GeoServer.

## Configuration

The extension is **disabled by default**. To enable it, add the following configuration:

```yaml
geoserver:
  extension:
    importer:
      enabled: true
```

## Dependencies

This extension requires the following GeoServer dependencies:

- gs-importer-core
- gs-importer-web (for Web UI support)
- gs-importer-rest (for REST API support)

## Components

The extension provides three main components:

1. **Core Functionality**: The base Importer functionality
2. **Web UI**: Admin interface for importing data
3. **REST API**: REST endpoints for programmatic data import

## Implementation Details

The auto-configuration automatically detects which components are needed based on the GeoServer Cloud microservice:

- In WebUI service, both Core and Web UI components are activated
- In REST service, both Core and REST API components are activated

The auto-configuration uses Spring Boot's conditional activation to ensure that components are only loaded when their dependencies are available and the appropriate service is running.
