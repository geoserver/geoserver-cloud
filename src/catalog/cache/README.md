# GeoServer Cloud Catalog Cache

This module provides caching decorators for the GeoServer Catalog and Configuration facades to improve performance in distributed deployments.

## Overview

The module implements two main caching facades:

- **CachingCatalogFacade**: Caches catalog objects (workspaces, namespaces, stores, resources, layers, styles, layer groups)
- **CachingGeoServerFacade**: Caches configuration objects (GeoServerInfo, LoggingInfo, ServiceInfo, SettingsInfo)

Both facades use Spring Cache abstraction with [Caffeine](https://github.com/ben-manes/caffeine) as the underlying cache implementation.

## Cache Invalidation

The caching facades listen to catalog and configuration change events to maintain cache consistency across distributed GeoServer Cloud instances. When a remote event is received (indicating a change made by another instance), the affected cache entries are evicted.

## Configuration

Caching is disabled by default. To enable it, set the following property:

```yaml
geoserver:
  catalog:
    caching:
      enabled: true
```

## Cache Names

- `gs-catalog`: Used by `CachingCatalogFacade` for catalog objects
- `gs-config`: Used by `CachingGeoServerFacade` for configuration objects
