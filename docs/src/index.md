# Introduction

GeoServer Cloud is a cloud-native version of GeoServer using dockerized microservices.

We decompose geospatial services and APIs into components within a microservices architecture. 
This approach incorporates enhancements based on extensive GeoServer clustering experience.

Each service is self-contained and individually scalable. 
We adapt and extend GeoServer components using functional decomposition by business capability.

# Background and Motivation

GeoServer enables publishing and editing geospatial data from numerous formats. 
Standard monolithic installations are straightforward but present scaling and monitoring challenges.

Traditional high-availability deployments often require fixed instance counts or complex synchronization. 
This can lead to over-provisioning and inefficient budget use on public cloud platforms.

GeoServer Cloud addresses these issues by allowing independent enablement and dimensioning of capabilities.

# Vision

GeoServer Cloud targets organizations seeking reliable GeoServer provisioning in dynamic environments. 
We aim for functional decomposition where each OWS service and the Web UI are self-contained microservices. 
Services remain loosely coupled through an event-driven architecture.

We support various deployment strategies, from single-server compositions to Kubernetes clusters.

# Goals and Benefits

* Provide guidelines for dimensioning services based on resource needs.
* Enable independent service evolution.
* Centralize configuration.
* Isolate services to ensure system continuity.
* Implement per-service auto-scaling.
* Achieve location transparency via a gateway service.
* Centralize logging, tracing, and monitoring.

# Quick Start

These instructions cover running the system as a Docker composition on a single machine. 
Refer to the Deployment Guide for advanced strategies.

Ensure Docker Engine and the Compose Plugin are installed.

Download the Docker Compose file:

```bash
wget "http://geoserver.org/geoserver-cloud/deploy/docker-compose/stable/pgconfig/compose.yml"
```

Fetch the Docker images:

```bash
docker compose pull
Pulling rabbitmq  ... done
Pulling database  ... done
Pulling consul    ... done
Pulling config    ... done
Pulling gateway   ... done
Pulling catalog   ... done
...
```

Start the services:

```bash
docker compose up -d
```

Check status:

```bash
docker compose ps
```

Access the system:

- [http://localhost:8500](http://localhost:8500) for the Consul UI.
- [http://localhost:9090](http://localhost:9090) for the GeoServer UI. 

The Gateway service proxies requests to the appropriate microservice.

Containers synchronize using a message-driven event bus.

For Docker Compose deployments, we use:
- **Consul**: For service discovery and health monitoring.
- **Config Server**: For centralized configuration management.
- **RabbitMQ**: For event bus communication.

Experiment with dynamic service scaling using `docker compose scale <service>=<instances>`.

> **Note**: For Kubernetes deployments, use the `standalone` Spring profile. Leverage Kubernetes Services for service discovery instead of Consul. Kubernetes handles load balancing natively and efficiently.

# Technology Overview

GeoServer is a Spring Framework monolithic application. 

GeoServer Cloud microservices use the Spring Boot framework.

Spring Cloud technologies enable dynamic service discovery, externalized configuration, and distributed events.

We support a curated list of GeoServer extensions verified for this architecture.

# System Architecture Overview

The following diagram depicts the system's general architecture:

![GeoServer Cloud Architecture Diagram](assets/images/gs_cloud_architecture_diagram.svg  "GeoServer Cloud Architecture Diagram")

# Project Status

GeoServer Cloud is **production ready**. 
It is deployed successfully on AWS, Azure, GKE, OpenShift, and Scaleway.
