# Kubernetes

GeoServer Cloud is a suite of microservices (WMS, WFS, WCS, WPS, GWC, REST, WebUI, and a Gateway) that share a catalog backend and a message bus for cross-service events. A Kubernetes deployment runs each service as its own workload, scales them independently, and fronts them with a single HTTP ingress.

## What you need

- A Kubernetes cluster (local or remote).
- An ingress controller.
- A **catalog backend** — one of:
    - **`datadir`** — shared file system (e.g. an NFS PersistentVolume).
    - **`pgconfig`** — a PostgreSQL database (recommended for anything beyond simple testing).
- A **message bus** — RabbitMQ. The chart provisions it for you if you don't bring your own.
- Optionally, the [GeoServer ACL](https://github.com/geoserver/geoserver-acl) service for advanced authorization.

See the [Externalized configuration guide](../../configuration/index.md) for the profiles and environment variables that select a backend.

## Recommended path — Helm

The [`camptocamp/helm-geoserver-cloud`](https://github.com/camptocamp/helm-geoserver-cloud) chart is the supported way to deploy GeoServer Cloud on Kubernetes. It is designed to be consumed **as a dependency of your own chart**, not installed directly — your chart owns the values overlay, pins the upstream version, and bundles any supporting resources (ConfigMaps, Secrets, Jobs).

The Helm section walks through that pattern and provides a working example.

## Getting started

1. Set up a local cluster and ingress — see [Prerequisites](./prerequisites.md).
2. Walk through the [Helm example](./helm/example-pgconfig.md) to deploy GeoServer Cloud with a `pgconfig` backend.
3. Read the [Helm overview](./helm/index.md) to understand the consumption pattern before adapting the example to your own environment.
