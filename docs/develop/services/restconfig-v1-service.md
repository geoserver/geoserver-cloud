# Cloud Native GeoServer REST configuration API v1 service

Spring Boot/Cloud microservice that exposes GeoServer [REST API](https://docs.geoserver.org/stable/en/user/rest/).

**Docker image**: `geoservercloud/gs-cloud-restconfig-v1`.

**Service name**: `restconfig-v1`.

Logical service name by which the [gateway-service](gateway-service.yml) will get the actual instances addresses from the [discovery-service](discovery-service.yml) and perform client-side load balancing against when interacting with the service.

## Configuration

## Developing

