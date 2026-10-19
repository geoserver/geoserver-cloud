# GeoServer Cloud Helm example: pgconfig

This chart deploys GeoServer Cloud 3.0.2 with the `pgconfig` catalog backend.
It wraps the [camptocamp/helm-geoserver-cloud](https://github.com/camptocamp/helm-geoserver-cloud)
chart and adds PostgreSQL and RabbitMQ.
[Example - pgconfig](example-pgconfig.md) explains each step.

## Requirements

- A Kubernetes cluster with an ingress controller, and `gscloud.localhost`
  resolving to it. [Prerequisites](../prerequisites.md) sets up a local one.
- `kubectl` and `helm`.

## Run

From this directory:

```bash
helm dependency update
helm upgrade --install gs-cloud-pgconfig .
kubectl wait --for=condition=ready pod --all --timeout=180s
```

Then open <http://gscloud.localhost/geoserver-cloud/web/>.

## Remove

```bash
helm uninstall gs-cloud-pgconfig
```

The PostgreSQL volume stays behind, and a new install reuses its catalog.
Delete it to start from scratch:

```bash
kubectl delete pvc data-gs-cloud-pgconfig-postgresql-0
```
