# Cloud Native GeoServer Deployment Guide

Docker images for all the services are available on DockerHub, under the [geoservercloud](https://hub.docker.com/u/geoservercloud/) organization.

Starting with version `1.8.6`, all Docker images are signed with [cosign](https://docs.sigstore.dev/signing/signing_with_containers/).

In order to verify the signatures, [install cosign](https://docs.sigstore.dev/system_config/installation/),download the [cosign.pub](../cosign/cosign.pub) file containing the public key, and run

```
cosign verify --key cosign.pub geoservercloud/<image name>:1.8.6
```

The public key can also be an environment variable. For example:

```
export GSC_COSIGN_PUB_KEY=`cat cosign.pub`
cosign verify --key env://GSC_COSIGN_PUB_KEY geoservercloud/<image name>:1.8.6
```

> In any case, replace `1.8.6` with the specific version you're using

## Available Docker images

### Infrastructure

* **[geoservercloud/geoserver-cloud-config](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-config)**:
[Spring Cloud Config](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/) server providing support for [externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) in a distributed system, as a central place to manage external properties for applications across all environments.
> Usually you'd only use the config service in docker compose deployments. In a Kubernetes environment, we prefer to use the [externalized configuration](https://github.com/geoserver/geoserver-cloud-config) embedded in the Docker images under `/etc/geoserver/`.
* **[geoservercloud/geoserver-cloud-discovery](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-discovery)**
* **[geoservercloud/geoserver-cloud-gateway](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-gateway)**:
[Spring Cloud Gateway](https://cloud.spring.io/spring-cloud-gateway/reference/html/) reverse proxy providing a single entry point to all GeoServer services.

### GeoServer Services

* **[geoservercloud/geoserver-cloud-wms](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wms)**
GeoServer Web Map Server
* **[geoservercloud/geoserver-cloud-wfs](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wfs)**
GeoServer Web Feature Server
* **[geoservercloud/geoserver-cloud-wcs](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wcs)**
GeoServer Web Coverage Server
* **[geoservercloud/geoserver-cloud-wps](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-wps)**
GeoServer Web Processing Server
* **[geoservercloud/geoserver-cloud-gwc](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-gwc)**
GeoServer GeoWebCache service
* **[geoservercloud/geoserver-cloud-rest](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-rest)**
GeoServer REST API service
* **[geoservercloud/geoserver-cloud-webui](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-webui)**
GeoServer configuration Web User Interface service

### Optional security services

* [geoservercloud/geoserver-acl](https://hub.docker.com/repository/docker/geoservercloud/geoserver-acl)
[Access Control List](https://github.com/geoserver/geoserver-acl) service is an advanced authorization system for GeoServer.

### Test-only/non-production images

* [geoservercloud/geoserver-cloud-admin-server](https://hub.docker.com/repository/docker/geoservercloud/geoserver-cloud-admin-server): this is a simple [spring-boot-admin](https://github.com/codecentric/spring-boot-admin) image we use during development and it's **not intended for production deployments**.


## Docker compose deployments

Please check out the [docker-compose](./docker-compose/index.md) deployment document.

## Kubernetes

Please check out the example [Helm](https://helm.sh/) chart on this [helm-geoserver-cloud](https://github.com/camptocamp/helm-geoserver-cloud) repository as a starting point to deploy to K8s.

## Podman

Follow the [Podman](https://podman.io/)'s [deployment guide](./podman/index.md) to use Podman's daemonless container engine for Cloud Native GeoServer containers.
