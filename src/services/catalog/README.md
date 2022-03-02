# Cloud Native GeoServer Catalog service

Spring Webflux reactive microservice that exposes the GeoServer *catalog*, *global*, and *resources* configuration
objects through a RESTful API to other microservices, in order to abstract out the microservices that require access
to the catalog from the actual catalog backend and implementation.

Follow the service [documentation](../../docs/develop/services/catalog-service.md) and keep it up to date .
