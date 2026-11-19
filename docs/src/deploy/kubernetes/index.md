# Kubernetes

GeoServer Cloud is a suite of microservices (WMS, WFS, WCS, WPS, GWC, REST, WebUI, and a Gateway) that share a catalog backend and a message bus for cross-service events. A Kubernetes deployment runs each service as its own workload, scales them independently, and fronts them with a single HTTP ingress.

## What you need

- A Kubernetes cluster (local or remote).
- An ingress controller.
- A **catalog backend** — one of:
    - **`datadir`** — shared file system (e.g. an NFS PersistentVolume). See [Data directory on shared storage](./datadir-shared-storage.md).
    - **`pgconfig`** — a PostgreSQL database (recommended for anything beyond simple testing).
- A **message bus** — RabbitMQ. The chart provisions it for you if you don't bring your own.
- Optionally, the [GeoServer ACL](https://github.com/geoserver/geoserver-acl) service for advanced authorization.

See the [Externalized configuration guide](../../configuration/index.md) for the profiles and environment variables that select a backend.

## Recommended path — Helm

The [`camptocamp/helm-geoserver-cloud`](https://github.com/camptocamp/helm-geoserver-cloud) chart is the supported way to deploy GeoServer Cloud on Kubernetes. It is designed to be consumed **as a dependency of your own chart**, not installed directly — your chart owns the values overlay, pins the upstream version, and bundles any supporting resources (ConfigMaps, Secrets, Jobs).

The Helm section walks through that pattern and provides a working example.

## Gateway connections to the services

Each gateway replica opens at most 500 connections to each upstream address. With the Helm chart, the gateway
reaches a service through its Kubernetes Service, and the pods of that service share these connections. Requests
above the limit wait in the gateway, where the control-flow extension of the services cannot see them, and fail
after 10 seconds. If a service runs enough pods to serve more than 500 concurrent requests, raise the limit to
what the pods admit together: the number of pods times their Tomcat threads, see below. Set it on the gateway
container with an environment variable:

```yaml
SPRING_CLOUD_GATEWAY_SERVER_WEBMVC_HTTPCLIENT_POOL_MAXCONNECTIONS: "2000"
```

The gateway stops reusing a connection to a service after 30 seconds without requests, before the service
closes it. The same rule applies one hop up: the ingress controller must stop reusing an idle connection to the
gateway before the gateway closes it, 60 seconds after its last request.

The [gateway service reference](../../developer-guide/services/gateway-service.md#connection-pool) lists the
other pool settings and the timeouts of the proxied requests.

## Requests admitted by the services

The control-flow extension decides how many requests a pod runs at once, per service and per operation, from
the CPU cores of the pod. A request above a limit waits in a queue for up to 10 seconds, then gets a 503. The
rules and their defaults are in the [configuration guide](../../configuration/index.md#concurrent-requests).

A request needs a Tomcat thread to enter that queue. The service pods start with 500 threads, as many requests as
one gateway replica sends to one address at once. With several gateway replicas in front of one service, give its
pods the number of replicas times the pool size, with the `TOMCAT_THREADS_MAX` environment variable. Beyond its
threads, Tomcat keeps requests waiting with no timeout and no answer: in a burst of 480 requests to a pod with
200 threads, the last request was answered after 28 seconds instead of 10.

The control-flow limits scale with the processors seen by the JVM, equal to the CPU limit of the container. With
a CPU request far below that limit, the pod sizes its limits for CPU not guaranteed to it. Set the CPU request of
the OWS pods equal to their limit, or pass the cores to size for in the Java options: `JAVA_OPTS=-Dcpu.cores=2`.

## Getting started

1. Set up a local cluster and ingress — see [Prerequisites](./prerequisites.md).
2. Walk through the [Helm example](./helm/example-pgconfig.md) to deploy GeoServer Cloud with a `pgconfig` backend.
3. Read the [Helm overview](./helm/index.md) to understand the consumption pattern before adapting the example to your own environment.
