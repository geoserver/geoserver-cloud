# Helm

The [`camptocamp/helm-geoserver-cloud`](https://github.com/camptocamp/helm-geoserver-cloud) chart packages all GeoServer Cloud services for Kubernetes. This page explains **how you're meant to consume it**. If you want to jump straight to a working example, head to [Example — pgconfig](./example-pgconfig.md).

## Consumption pattern: wrap it in your own chart

You do not install `camptocamp/helm-geoserver-cloud` directly. Instead, you create your own Helm chart that lists it as a **sub-chart dependency** and provides the values overlay for your environment. The minimal shape of such a wrapper chart looks like this:

```yaml title="Chart.yaml"
apiVersion: v2
name: my-geoserver-cloud
description: My GeoServer Cloud deployment
version: 0.1.0
appVersion: '3.0.0-RC'
dependencies:
  - name: geoservercloud
    version: 3.0.0-rc
    repository: https://camptocamp.github.io/helm-geoserver-cloud
  - name: postgresql
    version: 15.5.2
    repository: oci://ghcr.io/georchestra/bitnami-helm-charts
    condition: postgresql.enabled
  - name: rabbitmq
    version: 14.4.0
    repository: oci://ghcr.io/georchestra/bitnami-helm-charts
    condition: rabbitmq.enabled
```

Your `values.yaml` then targets the `geoservercloud` sub-chart under its name:

```yaml title="values.yaml"
geoservercloud:
  global:
    profile: standalone,pgconfig
  geoserver:
    services:
      wms:
        replicaCount: 2
```

### Why wrap it

- **Pinned versions.** Your `Chart.lock` captures exact versions of the app chart and its infra deps so installs are reproducible.
- **A values overlay you own.** You never have to edit upstream files — your `values.yaml` is the only knob you operate, and it lives in your Git history.
- **Room for your own resources.** Put ConfigMaps, Secrets, Jobs, and NetworkPolicies under your chart's `templates/` directory. The bundled example uses this to ship a `ConfigMap` that installs the PostGIS extension at first Postgres startup.
- **Co-located infra deps.** Add RabbitMQ, PostgreSQL, or anything else your deployment needs as additional `dependencies:` entries, so `helm upgrade --install` brings the whole stack up together.

## PostgreSQL and RabbitMQ images: use the `bitnamilegacy` mirror

!!! warning "Bitnami image availability"

    Bitnami's official container images are no longer freely redistributable. The examples on this site pull the chart from GeOrchestra's OCI mirror (`oci://ghcr.io/georchestra/bitnami-helm-charts`) **and** override the image to point at `docker.io/bitnamilegacy/postgresql` and `docker.io/bitnamilegacy/rabbitmq`, which remain publicly pullable. If you omit these overrides you will see `ImagePullBackOff` on the `postgresql` and `rabbitmq` pods.

Minimal override in `values.yaml`:

```yaml
postgresql:
  image:
    registry: docker.io
    repository: bitnamilegacy/postgresql

rabbitmq:
  image:
    registry: docker.io
    repository: bitnamilegacy/rabbitmq
```

## Values structure cheat sheet

The upstream chart exposes three top-level keys you'll touch most often:

| Key | Purpose |
| --- | --- |
| `geoservercloud.global.*` | Settings shared across all services — active Spring profile, image pull policy, common annotations. |
| `geoservercloud.geoserver.services.<name>.*` | Per-service tuning: `replicaCount`, `resources`, `volumes`, `volumeMounts`, `containers.spring.env`, etc. Services include `gateway`, `webui`, `rest`, `wms`, `wfs`, `wcs`, `wps`, `gwc`, and optionally `acl`. |
| `geoservercloud.geoserver.ingress.*` | Ingress hostnames, TLS, and per-host routing rules. |

Because the same `env`, `volumes`, and `volumeMounts` blocks usually apply to every service, the bundled example uses [YAML anchors](https://yaml.org/spec/1.2.2/#692-node-anchors) to define them once and reference them across services. This is a Helm-side convention, not a chart feature — you can expand them inline if you prefer. The upstream [`values.yaml`](https://github.com/camptocamp/helm-geoserver-cloud/blob/master/values.yaml) is the authoritative reference for the full schema.

## Where to go next

- **[Example — pgconfig](./example-pgconfig.md)** — the walkthrough for the chart bundled with these docs. Start here.
- **Upstream [`examples/`](https://github.com/camptocamp/helm-geoserver-cloud/tree/master/examples) directory** — additional patterns: `datadir` (shared filesystem), `gwcStatefulSet` (dedicated tile cache), `pgconfig-acl` (with the Access Control List service), `pgconfig-wms-hpa` (horizontal pod autoscaler for WMS).
