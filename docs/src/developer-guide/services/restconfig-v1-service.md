# Cloud Native GeoServer REST configuration API v1 service

Spring Boot/Cloud microservice that exposes GeoServer [REST API](https://docs.geoserver.org/stable/en/user/rest/).

**Docker image**: `geoservercloud/gs-cloud-restconfig-v1`.

**Service name**: `restconfig-v1`.

Logical service name by which the [gateway-service] will get the actual instances addresses from the [discovery-service] and perform client-side load balancing against when interacting with the service.

The service runs the GeoWebCache catalog integration: creating, renaming, or
deleting layers and layer groups through the REST API creates, renames, or
deletes the corresponding GWC tile layers, honoring the "automatically cache
new layers" setting, and truncates caches affected by style changes. Changes
propagate to the other services through the event bus and the shared
tile-layer configuration storage.

## Configuration

Like the `wms`, `webui`, and `gwc` services, this service needs access to the
GeoWebCache cache directory (`gwc.cache-directory`, usually set through the
`GEOWEBCACHE_CACHE_DIR` environment variable) in order to apply tile-layer
deletions and cache truncations.

## Developing

