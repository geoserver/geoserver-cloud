# GeoServer OGC API Features Extension

This module integrates the OGC API Features extension with GeoServer Cloud.

## Overview

OGC API Features provides a modern, web-friendly way to access geospatial data. It follows a RESTful approach and includes:

- Standard paths for data discovery
- GeoJSON as the primary encoding format
- OpenAPI specification for API documentation
- Support for spatial and property filtering
- Pagination and sorting capabilities

## Configuration

The extension is **disabled by default**. To enable it, add the following configuration:

```yaml
geoserver:
  extension:
    ogcapi:
      features:
        enabled: true
```

## Features

- Full compliance with OGC API Features specification
- Integration with GeoServer's existing catalog and security
- Support for complex feature filtering
- Customizable HTML rendering for browser access
- API documentation through OpenAPI

## Implementation Details

This extension adds OGC API Features endpoints to GeoServer Cloud. It is activated in REST-enabled services and provides endpoints following the pattern `/ogc/features`.

The implementation is specifically designed to work with GeoServer Cloud's architecture, ensuring proper integration with the catalog backend and maintaining compatibility with deployment across multiple microservices.

## Components

The extension provides the following components:

1. Core API implementation
2. GeoJSON encoders for feature collections and features
3. HTML views for browser-based exploration
4. Integration with GeoServer's security framework
5. OpenAPI documentation