# Introduction

*GeoServer Cloud* is  [GeoServer](http://geoserver.org/) ready to use in the cloud through dockerized microservices.

The *GeoServer Cloud* project splits the GeoServer geospatial services and API offerings into individually deployable components of a [microservices based architecture](https://microservices.io/). This project provides clear constructive changes to GeoServer when required, based on prior experience with GeoServer clustering solutions.

*GeoServer Cloud* is built with GeoServer software components. Components are adapted and/or extended in a [functional decomposition by business capability](https://microservices.io/patterns/decomposition/decompose-by-business-capability.html). The result is each service (OWS service, the Web UI, the REST API ) is available as a self-contained, individually deployable and scalable micro-services.

> Q: Does that mean *GeoServer*'s `.war` is deployed several times, with each instance exposing a given "business capability"?
> 
> Absolutely not, this is not a clustered GeoServer approach and does not use war files.
>Each microservice is its own self-contained application, including only the GeoServer components needed for the service. Many GeoServer components provide a lot of functionality (such as different output formats). In these cases, care is taken to only load the functionality that is needed for a light-weight experience.

# Contents
{:.no_toc}

* Will be replaced with the ToC, excluding the "Contents" header
{:toc}

# Background and motivation

GeoServer is the most widely used and deployed Open Source geospatial server in the world. It allows publishing, transforming, and editing geospatial data from a wide number of formats, through an extense number of user-facing services. Besides these out-of-the-box capabilities, it also counts with over 40 supported [extensions](http://geoserver.org/release/stable/), plus a bigger number of experimental extensions (so-called "community modules").

Being a [monolithic application](https://en.wikipedia.org/wiki/Monolithic_application), [installation and configuration](https://docs.geoserver.org/latest/en/user/installation/war.html) of GeoServer is very easy, as all its different components are bundled together, offering a simple, cohesive approach to software delivery.

People in charge of deploying and maintaining GeoServer instances usually face some very common challenges when it comes to server dimensioning, configuration, system health monitoring and corrective actions; difficulties that get incremented as systems need to ensure a certain capacity to handle request load, service availability, and overall performance.

Traditional deployments, though feasible of providing [high availability](https://en.wikipedia.org/wiki/High_availability), tend to require a fixed number of application instances, a single-master/multiple-slaves cluster architecture, and/or the installation and configuration of additional components to keep instances in sync.

Ensuring a given load capacity often results in over-provisioning of server resources to handle peaks on demand, which can have a negative impact on an organization's budget, especially if deploying on a public cloud provider such as Amazon AWS, Microsoft Azure, or Google Cloud Platform. 

Though a gateway/load balancer can be put in front and still make the cluster instances [auto-scalable](https://en.wikipedia.org/wiki/Autoscaling), all instances expose the whole set of functionalities GeoServer provides, despite which service(s) are under high demand, which can lead to unnecessary consumption of resources, opening attack vectors, and losing availability of unrelated capabilities when some component fails.

# Vision

*GeoServer Cloud* is a sibling project to GeoServer, targeting organizations and system administrators in seek of a steady, prescribed way of provisioning GeoServer capabilities in a dynamic environment, where each business capability can be enabled, configured, dimensioned, and deployed independently.

*GeoServer Cloud* is not a re-write of GeoServer. On the contrary, it builds on top of existing *GeoServer* software components, adapting and/or extending them, feeding from, and contributing back to GeoServer's mainline development. Traditional GeoServer installations are still recommended for simple deployments that can/need to expose all its capabilities.

This project reformulates how these capabilities are bundled together, in an attempt to achieve [functional decomposition by business capability](https://microservices.io/patterns/decomposition/decompose-by-business-capability.html); which roughly means each OWS service, the Web UI, the REST API, and probably other components such as the *Catalog and Configuration subsystem*, become self-contained, individually deployable and scalable micro-services, loosely coupled through an [event based architecture](https://en.wikipedia.org/wiki/Event-driven_architecture).

These containerized applications allow deployment strategies to vary from single-server docker composition, to multi-node deployments using [docker swarm](https://docs.docker.com/engine/swarm/) or [Kubernetes](https://kubernetes.io/).

# Goals and benefits

* Possibility to assess and provide guidelines for proper dimensioning of services based on each one's resource needs and performance characteristics
* Independent evolvability of services
* Externalized, centralized configuration of services and their sub-components
* Service isolation allows the system to keep working in the event of some specific service becoming unavailable
* Ability to implement per service auto-scaling
* Ability to implement continuous delivery workflows
* Location transparency of service instances in a dynamic environment. A "gateway" service acts as a single entry point to all requests, dispatching them to the appropriate micro-services in a round-robin load balanced automatic configuration
* Centralized service logging, request tracing, and monitoring

<!--
"Cloud Native" applications

- [ ] note: familiarity with GeoServer is assumed.
- [x]  mention: budget factor in cloud providers and how (auto-)scaling and resourcing can reduce the bill
- [x]  mention: possibility to assess and provide guidelines for proper dimensioning of services based on each one's resource needs and capacity
- [x]  mention: Location Transparency achieved by discovery service and dynamic registration/deregistration of services + client side load balancing
- [x]  mention: traditional deployments, though feasible of providing HA and even auto-scaling, tend to require fixed instance/locations. Though a gateway/load balancer can be put in front and still make the cluster instances elastic, there's still the factor of instantiating a full service each time.

- [x] isolation, independent evolvability
- [ ] target platforms
- [x] per-service resourcing (cpu/ram/storage)
- [ ] continuous delivery (cf push?)?
- [ ] per-service (auto-)scaling
- [ ] multi-cloud? (cloud foundry/k8's/docker/swarm) paradigm shift from multi-servlet engine deploy (tomcat/jetty/websphere/etc)

-->

# Quick Start

The following instructions are meant as quick start up guide to run the system as a docker composition on a single machine. For more advanced deployment options and strategies, check out the [Deployment Guide](deploy/index.md).

Make sure you have `docker` and `docker-compose` installed:

 * [Docker](https://docs.docker.com/engine/install/) version >= `19.03.3`
 * [docker-compose](https://docs.docker.com/compose/) version >= `1.26.2`

Here are two docker-compose based deployment examples to try out:

  * A [Shared data-directory](deploy/docker-compose/stable/shared_datadir/docker-compose.yml) `docker-compose.yml` file;
  * and a PostgreSQL [jdbcconfig](deploy/docker-compose/stable/jdbcconfig/docker-compose.yml) `docker-compose.yml`.

The former will mount a docker volume on all containers for them to share the same "data directory",
while the later will use a PostgreSQL database to store the catalog and other resource configuration
objects instead of the file system.

Download either one to a directory in your computer. For example:

```bash
$ wget "http://geoserver.org/geoserver-cloud/deploy/docker-compose/stable/shared_datadir/docker-compose.yml"
```

Or:

```bash
$ wget "http://geoserver.org/geoserver-cloud/deploy/docker-compose/stable/jdbcconfig/docker-compose.yml"
```

Open a terminal and enter the directory where you just downloaded that file, and run `docker-compose pull` to fetch the docker images from [Dockerhub](https://hub.docker.com/u/geoservercloud/):


```bash
$ docker-compose pull
Pulling rabbitmq  ... done
Pulling database  ... done
Pulling discovery ... done
Pulling config    ... done
Pulling gateway   ... done
Pulling catalog   ... done
Pulling wfs       ... done
Pulling wms       ... done
Pulling wcs       ... done
Pulling rest      ... done
Pulling webui     ... done
```

Then start the services with this command:

```bash
$ docker-compose --compatibility up -d
```

> Note the `--compatibility` argument is required when using a v3 docker-compose file, in order to enable resource the CPU and Memory limits configured in the file for each service.

Wait for the services to start up, check with `docker-compose ps` until they're healthy. May some service had exited, re-run `docker-compose up -d`, initial startup may cause timeouts as services compete for resources on a single machine.

Then browse to:

- [http://localhost:8761](http://localhost:8761/) to access the Discovery service registry page.
- [http://localhost:9090](http://localhost:9090/) for the GeoServer UI. The GateWay service will take care of proxying requests to the appropriate microservice.

From a usability point of view, there should be no appreciable differences with a traditional GeoServer deployment.

What's going on inside the local-machine cluster you've just run, is that each GeoServer business capability is running as a separate process in a separate docker container, and they're all in synch in a loosely coupled way using a message driven event-bus.

Experiment dynamic service scaling and registration with `docker-compose scale <service>=<instances>`, for example:

```bash
docker-compose scale wfs=3 wcs=0
Starting gscloud_wfs_1 ... done
Creating gscloud_wfs_2 ... done
Creating gscloud_wfs_3 ... done
Stopping and removing gscloud_wcs_1 ... done
```

# Technology Overview

With *GeoServer* being a traditional, [Spring Framework](https://spring.io/) based, monolithic Servlet/WebMVC application, a logical choice has been made to base the *GeoServer* derived microservices in the [Spring Boot](https://spring.io/projects/spring-boot) framework.

Additionally, [Spring Cloud](https://spring.io/projects/spring-cloud) technologies enable crucial capabilities such as [dynamic service discovery](https://spring.io/projects/spring-cloud-netflix), [externalized configuration](https://spring.io/projects/spring-cloud-config), [distributed events](https://spring.io/projects/spring-cloud-bus), [API gateway](https://spring.io/projects/spring-cloud-gateway), and more.

Only a curated list of the [vast amount](http://geoserver.org/release/stable/) of GeoServer extensions will be supported, as they are verified and possibly adapted to work with this project's architecture.

# System Architecture Overview

The following diagram depicts the System's general architecture:

![GeoServer Cloud Architecture Diagram](img/gs_cloud_architecture_diagram.svg  "GeoServer Cloud Architecture Diagram")

> - Hexagons represent microservices;
> - coloured rectangles, logical groupings of components;
> - lines connecting a group to another component: connector applies to all services of the outgoing end, to all components of the incoming end; 
> - white rectangles, components that are platform/deployment choices. For example:
>     - "Event bus" could be a cloud infrastructure provider's native service (event queue), or a microservice implementing a distributed event broker;
>     - "Catalog/Config backend" is the software component used to access the catalog and configuration. Might be a microservice itself, catalog/config provider for  "data  directory", database, or other kind of external service store, catalog/config backend implementations;
>     - "Catalog/Config storage" is the storage mechanism that backs the catalog/config software component.  Might be a shared "data  directory" or database, a "per instance" data directory or database, and so on, depending on the available catalog/config backend implementations, and how they're configured and provisioned;
>     - "Geospatial data sources" is whatever method is used to access the actual data served up by the microservices.


> Note the above diagram represents the overall system's architecture. This is not a deployment diagram. Deployment involves choice of platforms, configurations, and more; without affecting the general architecture.
> Some microservices/components, though planned and represented in the architecture diagram, have not yet been developed/integrated. For instance: the logging, tracing, and monitoring components, as well as the GWC and WPS microservices.
 
# Project Status

Version `1.6.1` has been released against a slightly customized GeoServer `2.24.2`
with some important fixes to allow starting up several GeoServer instances from an empty
directory or database. We will make sure to contribute those fixes upstream before the final
release.

[Camptocamp](https://camptocamp.com/), as the original author, is in process of donating the project to OsGeo/Geoserver.

> Q: So, is this **production ready**?
> GeoServer Cloud is **production ready**. It is currently used by several private companies and public authorities in Europe. It has been deployed successfully on AWS, Azure, GKE, OpenShift and Scaleway.

# Developer's Guide

Follow the [Developer's Guide](develop/index.md) to learn more about the System's design and how to get started contributing to it.

# Deployment Guide

Check out the [Deployment Guide](deploy/index.md) to learn about deployment options, configuration, and target platforms.

<!--

Things to mention

** Explain decomposition by business capability: microservice defined as the set of components exposing an API to implement a single business capability. E.g. WFS, WMS, catalog, rest-config, web-admin. Further decomposition could be applied to large subsistems. E.g. GWC -> tile serve and seeding services, WPS decomposed into API + lamda per process, etc., WMS into API/rendering engine, or services into per version microservice.

** Gateway: custom filter to adhere to OWS case-insensitive parameter names
-->

# Configuration guide

Check out the [Externalized configuration guide](configuration/index.md) to learn about fine tuning GeoServer-Cloud applications.
