# GeoServer Cloud — GeoWebCache Service

[GeoWebCache](https://www.geowebcache.org/) tile-cache microservice for [GeoServer Cloud](https://github.com/geoserver/geoserver-cloud). Accelerates WMS and map-viewer rendering by caching pre-rendered tiles.

- **Project:** [github.com/geoserver/geoserver-cloud](https://github.com/geoserver/geoserver-cloud)
- **Documentation:** [geoserver.org/geoserver-cloud](https://geoserver.org/geoserver-cloud/)
- **License:** [GPL-2.0](https://github.com/geoserver/geoserver-cloud/blob/main/LICENSE.txt)
- **Source:** [`src/apps/geoserver/gwc`](https://github.com/geoserver/geoserver-cloud/tree/main/src/apps/geoserver/gwc)

## About GeoServer Cloud

[GeoServer](https://geoserver.org/) is an open-source server for sharing geospatial data. **GeoServer Cloud** repackages GeoServer as a set of independently deployable and scalable microservices built on Spring Boot and Spring Cloud.

## What this image provides

A self-contained GeoWebCache microservice, supporting:

- WMTS, WMS-C, TMS, and KML tile protocols
- Vector tiles (MVT/PBF)
- Truncation and seeding operations
- Pluggable blob stores: local filesystem, S3, Azure Blob, Google Cloud Storage

The image is built on a GeoServer base layer and includes an **AOT cache** for fast JVM startup. It runs behind the [GeoServer Cloud Gateway](https://hub.docker.com/r/geoservercloud/geoserver-cloud-gateway), which routes `/gwc` requests to it.

## Tags

- `<version>` — stable release (e.g. `3.0.0`)
- `<version>-RC<n>` — release candidate (e.g. `3.0.0-RC1`)
- `<version>-SNAPSHOT` — development snapshot from `main`

No `latest` tag is published — always pin to a specific version. Images are multi-arch (`linux/amd64`, `linux/arm64`) and signed with [cosign](https://docs.sigstore.dev/).

## Quick start

Deploy together with the other GeoServer Cloud services via:

- **Docker Compose:** [examples in the project repo](https://github.com/geoserver/geoserver-cloud/tree/main/compose)
- **Kubernetes:** the [`camptocamp/helm-geoserver-cloud`](https://github.com/camptocamp/helm-geoserver-cloud) Helm chart

Standalone smoke test (no config server, no catalog, no data):

```bash
docker run --rm -p 9107:9107 \
  -e SPRING_PROFILES_ACTIVE=standalone,datadir \
  -e GEOSERVER_DATA_DIR=/tmp/datadir \
  geoservercloud/geoserver-cloud-gwc:3.0.0
```

## Configuration

| Setting | Default | Notes |
| --- | --- | --- |
| HTTP port | `9107` | override with `SERVER_PORT` |
| Management port | `8107` | actuator endpoints |
| Profiles | `standalone,datadir` | swap `datadir` for `pgconfig` or `jdbcconfig` to pick a catalog backend |
| Data directory | `${GEOSERVER_DATA_DIR}` | only used with `datadir` profile |
| Tile cache dir | `${GWC_CACHE_DIR}` | persistent volume recommended for disk blobstore |
| Health | `GET :8107/actuator/health` | liveness & readiness |
| Metrics | `GET :8107/actuator/prometheus` | Prometheus scrape target |

See the [Externalized configuration guide](https://github.com/geoserver/geoserver-cloud/blob/main/docs/src/configuration/index.md) for the full list of profiles and options.

## Support

- Issues: [github.com/geoserver/geoserver-cloud/issues](https://github.com/geoserver/geoserver-cloud/issues)
- Discussions: [geoserver.org/comm](https://geoserver.org/comm/)
