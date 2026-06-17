# GeoServer Cloud — Config Server

[Spring Cloud Config](https://spring.io/projects/spring-cloud-config) server for [GeoServer Cloud](https://github.com/geoserver/geoserver-cloud). Serves centralized, externalized configuration to every other GeoServer Cloud service at bootstrap.

- **Project:** [github.com/geoserver/geoserver-cloud](https://github.com/geoserver/geoserver-cloud)
- **Documentation:** [geoserver.org/geoserver-cloud](https://geoserver.org/geoserver-cloud/)
- **License:** [GPL-2.0](https://github.com/geoserver/geoserver-cloud/blob/main/LICENSE.txt)
- **Source:** [`src/apps/infrastructure/config`](https://github.com/geoserver/geoserver-cloud/tree/main/src/apps/infrastructure/config)

## About GeoServer Cloud

[GeoServer](https://geoserver.org/) is an open-source server for sharing geospatial data. **GeoServer Cloud** repackages GeoServer as a set of independently deployable and scalable microservices built on Spring Boot and Spring Cloud.

## What this image provides

This image runs the **Config Server**, the bootstrap component that publishes configuration to every other GeoServer Cloud service. On startup each service contacts the config server, downloads its configuration, and only then starts its Spring context. This keeps service images generic and deployment-specific settings centralized.

Two modes of serving configuration are supported:

- **`native` profile** (default) — serves configuration from a local directory (a baked-in `/etc/geoserver` inside this image, or a mounted volume).
- **`git` profile** — clones a Git repository that holds the configuration files. Useful for GitOps-style pipelines.

This image is primarily used in **Docker Compose** deployments. In Kubernetes, configuration typically lives in ConfigMaps or Secrets and this service is not deployed — the [`standalone`](https://github.com/geoserver/geoserver-cloud/blob/main/docs/src/configuration/index.md) profile on each service reads its config directly.

## Tags

- `<version>` — stable release (e.g. `3.0.0`)
- `<version>-RC<n>` — release candidate (e.g. `3.0.0-RC1`)
- `<version>-SNAPSHOT` — development snapshot from `main`

No `latest` tag is published — always pin to a specific version. Images are multi-arch (`linux/amd64`, `linux/arm64`) and signed with [cosign](https://docs.sigstore.dev/).

## Quick start

Serve the baked-in default configuration:

```bash
docker run --rm -p 8888:8888 \
  geoservercloud/geoserver-cloud-config:3.0.0
```

Serve configuration from a Git repository:

```bash
docker run --rm -p 8888:8888 \
  -e SPRING_PROFILES_ACTIVE=git \
  -e CONFIG_GIT_URI=https://github.com/your-org/geoserver-cloud-config.git \
  -e SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL=main \
  geoservercloud/geoserver-cloud-config:3.0.0
```

Fetch a config file to verify:

```bash
curl http://localhost:8888/gateway-service/default | jq
```

## Configuration

| Setting | Default | Notes |
| --- | --- | --- |
| HTTP port | `8888` | override with `SERVER_PORT` |
| Profiles | `native,standalone` | switch to `git` for Git-backed config |
| Native path | `/etc/geoserver` | mount here to override the baked-in config |
| `CONFIG_GIT_URI` | `https://github.com/geoserver/geoserver-cloud-config.git` | only used with `git` profile |
| `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL` | `master` | branch or tag to check out |
| Health | `GET /actuator/health` | liveness & readiness |

## Support

- Issues: [github.com/geoserver/geoserver-cloud/issues](https://github.com/geoserver/geoserver-cloud/issues)
- Discussions: [geoserver.org/comm](https://geoserver.org/comm/)
