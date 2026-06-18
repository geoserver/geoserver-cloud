# GeoServer Cloud — WMS Service

[Web Map Service (WMS)](https://www.ogc.org/standards/wms) microservice for [GeoServer Cloud](https://github.com/geoserver/geoserver-cloud). Renders geospatial data as map images (PNG, JPEG, etc.) on demand.

- **Project:** [github.com/geoserver/geoserver-cloud](https://github.com/geoserver/geoserver-cloud)
- **Documentation:** [geoserver.org/geoserver-cloud](https://geoserver.org/geoserver-cloud/)
- **License:** [GPL-2.0](https://github.com/geoserver/geoserver-cloud/blob/main/LICENSE.txt)
- **Source:** [`src/apps/geoserver/wms`](https://github.com/geoserver/geoserver-cloud/tree/main/src/apps/geoserver/wms)

## About GeoServer Cloud

[GeoServer](https://geoserver.org/) is an open-source server for sharing geospatial data. **GeoServer Cloud** repackages GeoServer as a set of independently deployable and scalable microservices built on Spring Boot and Spring Cloud.

## What this image provides

A self-contained WMS microservice. It serves the OGC WMS 1.1.1 and 1.3.0 protocols, including:

- `GetCapabilities`, `GetMap`, `GetFeatureInfo`, `GetLegendGraphic`
- SLD, CSS, and MBStyle styling extensions
- Vector tile output
- Raster rendering with Cloud Optimized GeoTIFF, ImagePyramid, and other input sources

The image is built on a GeoServer base layer and includes an **AOT cache** for fast JVM startup. It is intended to run behind the [GeoServer Cloud Gateway](https://hub.docker.com/r/geoservercloud/geoserver-cloud-gateway), which routes `/wms` requests to it.

## Tags

- `<version>` — stable release (e.g. `3.0.0`)
- `<version>-RC<n>` — release candidate (e.g. `3.0.0-RC1`)
- `<version>-SNAPSHOT` — development snapshot from `main`

No `latest` tag is published — always pin to a specific version. Images are multi-arch (`linux/amd64`, `linux/arm64`) and signed with [cosign](https://docs.sigstore.dev/).

## Quick start

The WMS service is typically deployed together with the other GeoServer Cloud services via:

- **Docker Compose:** [examples in the project repo](https://github.com/geoserver/geoserver-cloud/tree/main/compose)
- **Kubernetes:** the [`camptocamp/helm-geoserver-cloud`](https://github.com/camptocamp/helm-geoserver-cloud) Helm chart

Standalone smoke test (no config server, no catalog, no data):

```bash
docker run --rm -p 9102:9102 \
  -e SPRING_PROFILES_ACTIVE=standalone,datadir \
  -e GEOSERVER_DATA_DIR=/tmp/datadir \
  geoservercloud/geoserver-cloud-wms:3.0.0
```

Check it's up:

```bash
curl http://localhost:9102/actuator/health
```

## Configuration

| Setting | Default | Notes |
| --- | --- | --- |
| HTTP port | `9102` | override with `SERVER_PORT` |
| Management port | `8102` | actuator endpoints |
| Profiles | `standalone,datadir` | swap `datadir` for `pgconfig` or `jdbcconfig` to pick a catalog backend |
| Data directory | `${GEOSERVER_DATA_DIR}` | only used with `datadir` profile |
| Health | `GET :8102/actuator/health` | liveness & readiness |
| Metrics | `GET :8102/actuator/prometheus` | Prometheus scrape target |

See the [Externalized configuration guide](https://github.com/geoserver/geoserver-cloud/blob/main/docs/src/configuration/index.md) for the full list of profiles and options.

## Support

- Issues: [github.com/geoserver/geoserver-cloud/issues](https://github.com/geoserver/geoserver-cloud/issues)
- Discussions: [geoserver.org/comm](https://geoserver.org/comm/)
