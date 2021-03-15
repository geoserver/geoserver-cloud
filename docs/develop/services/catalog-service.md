# Cloud Native GeoServer Catalog service

Spring Webflux reactive microservice that exposes the GeoServer *catalog*, *global*, and *resources* configuration
objects through a RESTful API to other microservices, in order to abstract out the microservices that require access
to the catalog from the actual catalog backend and implementation.

**Docker image**: `geoservercloud/gs-cloud-catalog`. 

**Service name**: `catalog-service`. 

This is the logical service name by which web clients will get the actual instances addresses from the [discovery-service](discovery-service.yml) and perform client-side load balancing against when interacting with the service.

## Service Configuration


## Client Configuration


## Developing

