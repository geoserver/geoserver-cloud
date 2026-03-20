# Cloud Native GeoServer Deployment Guide

Docker images for all services are available on DockerHub under the [geoservercloud](https://hub.docker.com/u/geoservercloud/) organization.

All Docker images are signed with [cosign](https://docs.sigstore.dev/signing/signing_with_containers/).

## Available Docker images

### Infrastructure

* **[geoservercloud/geoserver-cloud-config](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-config)**:
Spring Cloud Config server for centralized configuration management.
> This service is used in Docker Compose deployments. In Kubernetes, we embed configuration in the images or use ConfigMaps and Secrets.
* **[hashicorp/consul](https://hub.docker.com/_/consul)**:
Service registry for service discovery and health monitoring.
> This service is used in Docker Compose deployments. For Kubernetes deployments, use the `standalone` Spring profile. Rely on Kubernetes Services for service discovery and load balancing.
* **[geoservercloud/geoserver-cloud-gateway](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-gateway)**:
Spring Cloud Gateway reverse proxy providing a single entry point to all GeoServer services.

### GeoServer Services

* **[geoservercloud/geoserver-cloud-wms](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wms)**: GeoServer Web Map Server
* **[geoservercloud/geoserver-cloud-wfs](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wfs)**: GeoServer Web Feature Server
* **[geoservercloud/geoserver-cloud-wcs](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wcs)**: GeoServer Web Coverage Server
* **[geoservercloud/geoserver-cloud-wps](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wps)**: GeoServer Web Processing Server
* **[geoservercloud/geoserver-cloud-gwc](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-gwc)**: GeoServer GeoWebCache service
* **[geoservercloud/geoserver-cloud-rest](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-rest)**: GeoServer REST API service
* **[geoservercloud/geoserver-cloud-webui](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-webui)**: GeoServer configuration Web User Interface service

### Optional security services

* [geoservercloud/geoserver-acl](https://hub.docker.com/repository/docker/geoservercloud/geoserver-acl): Advanced authorization system for GeoServer.

## Docker compose deployments

Refer to the [docker-compose](./docker-compose/index.md) deployment document.

## Kubernetes

Refer to the example [Helm](https://helm.sh/) chart on the [helm-geoserver-cloud](https://github.com/camptocamp/helm-geoserver-cloud) repository.
