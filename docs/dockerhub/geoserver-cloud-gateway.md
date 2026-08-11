# GeoServer Cloud — API Gateway

Single entry point for a [GeoServer Cloud](https://github.com/geoserver/geoserver-cloud) deployment. Routes client requests to the WFS, WMS, WCS, WPS, REST, GWC, and WebUI microservices.

- **Project:** [github.com/geoserver/geoserver-cloud](https://github.com/geoserver/geoserver-cloud)
- **Documentation:** [geoserver.org/geoserver-cloud](https://geoserver.org/geoserver-cloud/)
- **License:** [GPL-2.0](https://github.com/geoserver/geoserver-cloud/blob/main/LICENSE.txt)
- **Source:** [`src/apps/infrastructure/gateway-webmvc`](https://github.com/geoserver/geoserver-cloud/tree/main/src/apps/infrastructure/gateway-webmvc)

## About GeoServer Cloud

[GeoServer](https://geoserver.org/) is an open-source server for sharing geospatial data. **GeoServer Cloud** repackages GeoServer as a set of independently deployable and scalable microservices built on Spring Boot and Spring Cloud.

## What this image provides

This image runs the **Gateway** service, built on [Spring Cloud Gateway Server MVC](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html) (servlet model with virtual threads). It is the only service that should be exposed publicly in a GeoServer Cloud deployment; all OWS requests (`/wms`, `/wfs`, `/wcs`, `/wps`), the REST API (`/rest`), the tile cache (`/gwc`), and the Web UI (`/web`) flow through it.

Key features:

- Service discovery routing via Eureka (`lb://` URIs) or static host targets
- Custom filters: `StripBasePath`, `SharedAuth`, `SecureHeaders`, `RouteProfile`
- `X-Forwarded-*` header propagation for reverse proxy deployments
- CORS configuration via `globalcors`
- Prometheus metrics and Spring Boot Actuator health endpoints

Starting with GeoServer Cloud 3.0.0 this replaces the previous reactive (WebFlux) gateway. The [`geoservercloud/geoserver-cloud-gateway-webflux`](https://hub.docker.com/r/geoservercloud/geoserver-cloud-gateway-webflux) image was published for 3.0.0 only and was removed in 3.1.0.

## Tags

- `<version>` — stable release (e.g. `3.0.0`)
- `<version>-RC<n>` — release candidate (e.g. `3.0.0-RC1`)
- `<version>-SNAPSHOT` — development snapshot from `main`

No `latest` tag is published — always pin to a specific version. Images are multi-arch (`linux/amd64`, `linux/arm64`) and signed with [cosign](https://docs.sigstore.dev/).

## Quick start

The gateway requires backend services to route to. Use one of the supported deployment paths:

- **Docker Compose:** [examples in the project repo](https://github.com/geoserver/geoserver-cloud/tree/main/compose)
- **Kubernetes:** the [`camptocamp/helm-geoserver-cloud`](https://github.com/camptocamp/helm-geoserver-cloud) Helm chart

A minimal standalone run (no config server, no service discovery):

```bash
docker run --rm -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=standalone \
  geoservercloud/geoserver-cloud-gateway:3.0.0
```

Visit `http://localhost:9090` — without backends behind it the routes will return 503, which is expected.

## Configuration

| Setting | Default | Notes |
| --- | --- | --- |
| HTTP port | `9090` | override with `SERVER_PORT` |
| Config file | `/etc/geoserver/gateway.yml` | mount a volume to override routes |
| Profiles | `default` | common overlays: `standalone`, `acl` |
| Health | `GET /actuator/health` | liveness & readiness |
| Metrics | `GET /actuator/prometheus` | Prometheus scrape target |

See the [Gateway Service developer guide](https://github.com/geoserver/geoserver-cloud/blob/main/docs/src/developer-guide/services/gateway-service.md) for full configuration details and route definitions.

## Support

- Issues: [github.com/geoserver/geoserver-cloud/issues](https://github.com/geoserver/geoserver-cloud/issues)
- Discussions: [geoserver.org/comm](https://geoserver.org/comm/)
