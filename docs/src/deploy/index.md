# Deploying GeoServer Cloud

GeoServer Cloud is a suite of independent services, not a single binary. How you run it depends on whether you're **evaluating it on your laptop** or **running it for real users**.

## Which deployment path should I use?

| | Docker Compose | Kubernetes (Helm) |
| --- | --- | --- |
| **Best for** | Evaluation, demos, local development | Staging and production |
| **Tooling** | Docker + `docker compose` | Kubernetes cluster + `kubectl` + `helm` |
| **Scaling** | Single host | Horizontal across nodes, per-service |
| **High availability** | No | Yes |
| **Persistence** | Host volumes | PersistentVolumes / external DB |
| **Learning curve** | Low | Moderate |

- **[Docker Compose](./docker-compose/index.md)** — the fastest path to a running GeoServer Cloud on your machine. Recommended for evaluation and local development.
- **[Kubernetes](./kubernetes/index.md)** — production-oriented deployment via Helm. Includes a walkthrough you can run on a local `kind` cluster.

Both paths use the same container images, published on DockerHub under the [`geoservercloud`](https://hub.docker.com/u/geoservercloud/) organization. All images are signed with [cosign](https://docs.sigstore.dev/signing/signing_with_containers/).

## Container image reference

The full set of images GeoServer Cloud uses in production.

### Infrastructure

- **[`geoservercloud/geoserver-cloud-gateway`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-gateway)** — Spring Cloud Gateway reverse proxy; single entry point to all GeoServer services.
- **[`geoservercloud/geoserver-cloud-config`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-config)** — Spring Cloud Config server.
    > Used in Docker Compose deployments. In Kubernetes, configuration lives in the images or in ConfigMaps/Secrets, and this service is not deployed.
- **[`hashicorp/consul`](https://hub.docker.com/_/consul)** — service registry.
    > Used in Docker Compose deployments. For Kubernetes, the `standalone` Spring profile uses Kubernetes DNS for service discovery instead.

### GeoServer services

- **[`geoservercloud/geoserver-cloud-wms`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-wms)** — Web Map Server.
- **[`geoservercloud/geoserver-cloud-wfs`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-wfs)** — Web Feature Server.
- **[`geoservercloud/geoserver-cloud-wcs`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-wcs)** — Web Coverage Server.
- **[`geoservercloud/geoserver-cloud-wps`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-wps)** — Web Processing Server.
- **[`geoservercloud/geoserver-cloud-gwc`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-gwc)** — GeoWebCache tile service.
- **[`geoservercloud/geoserver-cloud-rest`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-rest)** — REST configuration API.
- **[`geoservercloud/geoserver-cloud-webui`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-webui)** — configuration Web User Interface.

### Optional security services

- **[`geoservercloud/geoserver-acl`](https://hub.docker.com/r/geoservercloud/geoserver-acl)** — Advanced authorization system for GeoServer.
