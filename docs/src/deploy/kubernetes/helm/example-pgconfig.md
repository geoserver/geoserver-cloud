# Example — pgconfig

This example deploys GeoServer Cloud with the `pgconfig` catalog backend (PostgreSQL) and the `standalone` Spring profile (no Consul/Config Server — services discover each other via Kubernetes DNS). The Helm chart in this directory is a minimal wrapper that depends on the upstream `camptocamp/helm-geoserver-cloud` chart plus PostgreSQL and RabbitMQ.

!!! tip "New to Kubernetes?"
    Before starting, make sure you have a local cluster with an ingress controller and a `gscloud.localhost` DNS alias. See [Prerequisites](../prerequisites.md).

## Files in this example

- `Chart.yaml` — wrapper chart metadata and dependencies.
- `values.yaml` — values overlay for the bundled and upstream charts.
- `templates/install-postgis-cm.yml` — a `ConfigMap` that installs the PostGIS extension on first Postgres startup.

## 1. Pull chart dependencies

From this directory (`docs/src/deploy/kubernetes/helm/`):

```bash
helm dependency update
```

This downloads three charts into `./charts/`:

- `geoservercloud` (the upstream GSC chart, pinned to `3.0.0-rc`).
- `postgresql` and `rabbitmq` from `ghcr.io/georchestra/bitnami-helm-charts` — a mirror that still publishes free images.

Expected last lines:

```text
Saving 3 charts
Downloading geoservercloud from repo https://camptocamp.github.io/helm-geoserver-cloud
Downloading rabbitmq from repo oci://ghcr.io/georchestra/bitnami-helm-charts
Downloading postgresql from repo oci://ghcr.io/georchestra/bitnami-helm-charts
Deleting outdated charts
```

## 2. Install the chart

```bash
helm upgrade --install gs-cloud-pgconfig .
```

Expected: `STATUS: deployed`. Warnings about `hostIP set without hostPort` are harmless (from the upstream chart) and can be ignored.

## 3. Wait for all pods to be ready

```bash
kubectl wait --for=condition=ready pod --all --timeout=180s
```

Then verify:

```bash
kubectl get pods
```

Expected: 10 pods, all `Running` with `1/1` under `READY`:

```text
NAME                                             READY   STATUS    RESTARTS   AGE
gs-cloud-pgconfig-gsc-gateway-…                  1/1     Running   0          1m
gs-cloud-pgconfig-gsc-gwc-…                      1/1     Running   0          1m
gs-cloud-pgconfig-gsc-rest-…                     1/1     Running   0          1m
gs-cloud-pgconfig-gsc-wcs-…                      1/1     Running   0          1m
gs-cloud-pgconfig-gsc-webui-…                    1/1     Running   0          1m
gs-cloud-pgconfig-gsc-wfs-…                      1/1     Running   0          1m
gs-cloud-pgconfig-gsc-wms-…                      1/1     Running   0          1m
gs-cloud-pgconfig-gsc-wps-…                      1/1     Running   0          1m
gs-cloud-pgconfig-postgresql-0                   1/1     Running   0          1m
gs-cloud-pgconfig-rabbitmq-0                     1/1     Running   0          1m
```

## 4. Confirm the DNS alias

You should already have this from [Prerequisites](../prerequisites.md):

```bash
grep gscloud.localhost /etc/hosts
```

Expected: a line mapping `gscloud.localhost` to `127.0.0.1`.

## 5. Tear down

```bash
helm uninstall gs-cloud-pgconfig
```

To also remove the cluster, see the teardown step in [Prerequisites](../prerequisites.md#5-tear-down-when-youre-done).

---

## Troubleshooting

??? failure "RabbitMQ `Connection refused` in early logs"
    GeoServer Cloud services start in parallel with RabbitMQ. For the first ~30 seconds you'll see log lines like:

    ```text
    AmqpConnectException: java.net.ConnectException: Connection refused
    ```

    The services retry automatically. If the message persists after a minute, check that `gs-cloud-pgconfig-rabbitmq-0` is `1/1 Running`.

??? failure "`ImagePullBackOff` on `postgresql` or `rabbitmq` pods"
    Bitnami's official images are no longer freely pullable. This example overrides both charts to use `docker.io/bitnamilegacy/*` in `values.yaml`. Check that those overrides are present:

    ```bash
    grep -A1 bitnamilegacy values.yaml
    ```

