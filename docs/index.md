# Introduction

*GeoServer Cloud* is a cloud-ready version of [GeoServer](http://geoserver.org/) that leverages dockerized microservices for deployment.

The *GeoServer Cloud* project decomposes GeoServer's geospatial services and API offerings into individually deployable components within a [microservices-based architecture](https://microservices.io/). This project incorporates practical enhancements to GeoServer based on extensive experience with GeoServer clustering solutions.

Built with GeoServer software components, *GeoServer Cloud* adapts and/or extends these components using a [functional decomposition by business capability](https://microservices.io/patterns/decomposition/decompose-by-business-capability.html) approach. As a result, each service (e.g., OWS service, Web UI, REST API) is available as a self-contained, individually deployable, and scalable microservice.

> Q: Does this mean *GeoServer*'s `.war` file is deployed multiple times, with each instance exposing a specific "business capability"?
> 
> Absolutely not. This is not a clustered GeoServer approach and does not use `.war` files. Each microservice is a self-contained application that includes only the GeoServer components necessary for the service. Many GeoServer components provide extensive functionality (such as different output formats). In these cases, care is taken to load only the functionality required for a lightweight experience.

# Contents
{:.no_toc}

* Will be replaced with the ToC, excluding the "Contents" header
{:toc}

# Background and Motivation

GeoServer is the most widely used and deployed open-source geospatial server in the world. It enables the publishing, transformation, and editing of geospatial data from numerous formats through a comprehensive set of user-facing services. In addition to these out-of-the-box capabilities, GeoServer supports over 40 [extensions](http://geoserver.org/release/stable/), along with numerous experimental extensions (known as "community modules").

As a [monolithic application](https://en.wikipedia.org/wiki/Monolithic_application), GeoServer's [installation and configuration](https://docs.geoserver.org/latest/en/user/installation/war.html) are straightforward, as all its components are bundled together, providing a cohesive approach to software delivery.

However, deploying and maintaining GeoServer instances often presents common challenges, such as server dimensioning, configuration, system health monitoring, and corrective actions. These difficulties increase as systems need to handle higher request loads, ensure service availability, and maintain overall performance.

Traditional deployments, while capable of providing [high availability](https://en.wikipedia.org/wiki/High_availability), often require a fixed number of application instances, a single-master/multiple-slave cluster architecture, and/or additional components to keep instances in sync.

Ensuring adequate load capacity often results in over-provisioning of server resources to handle demand peaks, which can negatively impact an organization's budget, especially when deploying on public cloud providers like Amazon AWS, Microsoft Azure, or Google Cloud Platform.

While a gateway/load balancer can make cluster instances [auto-scalable](https://en.wikipedia.org/wiki/Autoscaling), all instances expose the entire set of GeoServer functionalities, regardless of which services are in high demand. This can lead to unnecessary resource consumption, open attack vectors, and the loss of unrelated capabilities when a component fails.

# Vision

*GeoServer Cloud* is a sibling project to GeoServer, targeting organizations and system administrators seeking a reliable and prescribed way of provisioning GeoServer capabilities in a dynamic environment. In this environment, each business capability can be enabled, configured, dimensioned, and deployed independently.

*GeoServer Cloud* is not a re-write of GeoServer. Instead, it builds on existing GeoServer software components, adapting and/or extending them, and contributing back to GeoServer's mainline development. Traditional GeoServer installations are still recommended for simple deployments that need to expose all capabilities.

This project reformulates how these capabilities are bundled together, aiming to achieve [functional decomposition by business capability](https://microservices.io/patterns/decomposition/decompose-by-business-capability.html). This means each OWS service, the Web UI, the REST API, and other components such as the *Catalog and Configuration subsystem* become self-contained, individually deployable, and scalable microservices, loosely coupled through an [event-based architecture](https://en.wikipedia.org/wiki/Event-driven_architecture).

These containerized applications support various deployment strategies, ranging from single-server Docker compositions to multi-node deployments using [Docker Swarm](https://docs.docker.com/engine/swarm/) or [Kubernetes](https://kubernetes.io/).

# Goals and Benefits

* Provide guidelines for proper dimensioning of services based on their resource needs and performance characteristics.
* Enable independent evolution of services.
* Externalize and centralize configuration of services and their sub-components.
* Isolate services to ensure system continuity in the event of specific service unavailability.
* Implement per-service auto-scaling.
* Support continuous delivery workflows.
* Achieve location transparency of service instances in a dynamic environment. A "gateway" service acts as a single entry point, dispatching requests to the appropriate microservices using a round-robin load-balanced configuration.
* Centralize service logging, request tracing, and monitoring.

# Quick Start

The following instructions provide a quick start guide to running the system as a Docker composition on a single machine. For advanced deployment options and strategies, refer to the [Deployment Guide](deploy/index.md).

These instructions assume you have Docker Engine, Docker CLI, and the [Compose Plugin](https://docs.docker.com/compose/install/linux/) installed.

> GeoServer Cloud can run with various GeoServer catalog and configuration storage backends. For scalability, we recommend using our PostgreSQL backend, `pgconfig`.

Download the Docker Compose file to a directory on your computer:

```bash
wget "http://geoserver.org/geoserver-cloud/deploy/docker-compose/stable/pgconfig/compose.yml"
```

Open a terminal, navigate to the directory where you downloaded the file, and run docker compose pull to fetch the Docker images from Docker Hub:

```bash
docker compose pull
Pulling rabbitmq  ... done
Pulling database  ... done
Pulling discovery ... done
Pulling config    ... done
Pulling gateway   ... done
Pulling catalog   ... done
Pulling wfs       ... done
Pulling wms       ... done
Pulling wcs       ... done
Pulling gwc       ... done
Pulling rest      ... done
Pulling webui     ... done
Pulling acl       ... done
```

Then start the services with this command:

```bash
docker compose up -d
```

Wait for the services to start. Check their status with:

```bash
docker compose ps
```

If any service has exited, re-run docker compose up -d. Initial startup may cause timeouts as services compete for resources on a single machine.

Then browse to:

- [http://localhost:8761](http://localhost:8761) to access the Discovery service registry page.
- [http://localhost:9090](http://localhost:9090) for the GeoServer UI. The Gateway service will proxy requests to the appropriate microservice.

From a usability perspective, there should be no significant differences compared to a traditional GeoServer deployment.

In this local-machine cluster, each GeoServer business capability runs as a separate process in a dedicated Docker container. These containers synchronize in a loosely coupled manner using a message-driven event bus.

Experiment with dynamic service scaling and registration using `docker compose scale <service>=<instances>`. For example:

```bash
docker compose scale wfs=3 wcs=0
Starting gscloud_wfs_1 ... done
Creating gscloud_wfs_2 ... done
Creating gscloud_wfs_3 ... done
Stopping and removing gscloud_wcs_1 ... done
```

# Technology Overview

Given *GeoServer* is a traditional, [Spring Framework](https://spring.io/) based, monolithic Servlet/WebMVC application, *GeoServer Cloud* microservices are logically based on the Spring Boot framework.

Additionally, [Spring Cloud](https://spring.io/projects/spring-cloud) technologies enable crucial capabilities such as [dynamic service discovery](https://spring.io/projects/spring-cloud-netflix), [externalized configuration](https://spring.io/projects/spring-cloud-config), [distributed events](https://spring.io/projects/spring-cloud-bus), [API gateway](https://spring.io/projects/spring-cloud-gateway), and more.

Only a curated list of the [vast amount](http://geoserver.org/release/stable/) number of *GeoServer* extensions will be supported, as they are verified and potentially adapted to work with this project's architecture.

# System Architecture Overview

The following diagram depicts the system's general architecture:

![GeoServer Cloud Architecture Diagram](img/gs_cloud_architecture_diagram.svg  "GeoServer Cloud Architecture Diagram")

> - Hexagons represent microservices.
> - Colored rectangles represent logical groupings of components.
> - Lines connecting a group to another component indicate that the connector applies to all services of the outgoing end and to all components of the incoming end.
> - White rectangles represent components that are platform/deployment choices. For example:
>     - "Event bus" could be a cloud infrastructure provider's native service (event queue) or a microservice implementing a distributed event broker.
>     - "Catalog/Config backend" is the software component used to access the catalog and configuration. It might be a microservice itself, a catalog/config provider for a "data directory", a database, or another external service store, depending on the available catalog/config backend implementations and how they're configured and provisioned.
>     - "Catalog/Config storage" is the storage mechanism that backs the catalog/config software component. It might a shared "data  directory" or database, a "per instance" data directory or database, and so on, depending on the available catalog/config backend implementations, and how they're configured and provisioned.
>     - "Geospatial data sources" is whatever method is used to access the actual data served up by the microservices.

# Project Status

Version `1.8.9` has been released against a slightly customized GeoServer `2.25.2`
with some important fixes to allow starting up several GeoServer instances from an empty
directory or database. We will make sure to contribute those fixes upstream before the final
release.

> Q: So, is this **production ready**?
> GeoServer Cloud is **production ready**. It is currently used by several private companies and public authorities in Europe. It has been deployed successfully on AWS, Azure, GKE, OpenShift and Scaleway.

# Developer's Guide

Follow the [Developer's Guide](develop/index.md) to learn more about the System's design and how to get started contributing to it.

# Deployment Guide

Check out the [Deployment Guide](deploy/index.md) to learn about deployment options, configuration, and target platforms.

# Configuration guide

Check out the [Externalized configuration guide](configuration/index.md) to learn about fine tuning GeoServer-Cloud applications.
