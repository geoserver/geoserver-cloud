# Introduction

*Cloud Native GeoServer* is  [GeoServer](http://geoserver.org/) ready to use in the cloud through dockerized microservices.

This project is an opinionated effort to split *GeoServer*'s geospatial services and API offerings as individually deployable components of a [microservices based architecture](https://microservices.io/).

As such, it builds on top of existing *GeoServer* software components, adapting and/or extending them in an attempt to achieve functional decomposition by business capability; which roughly means each OWS service, the Web UI, the REST API, and probably other components such as the *Catalog and Configuration subsystem*, become self-contained, individually deployable and scalable micro-services.


# Contents
{:.no_toc}

* Will be replaced with the ToC, excluding the "Contents" header
{:toc}

# Technology Overview

With *GeoServer* being a traditional, [Spring Framework](https://spring.io/) based, monolithic servlet application, a logical choice has been made to base the *GeoServer* derived microservices in the [Spring Boot](https://spring.io/projects/spring-boot) framework.

Additionally, [Spring Cloud](https://spring.io/projects/spring-cloud) technologies enable crucial capabilities such as [dynamic service discovery](https://spring.io/projects/spring-cloud-netflix), [externalized configuration](https://spring.io/projects/spring-cloud-config), [distributed events](https://spring.io/projects/spring-cloud-bus), [API gateway](https://spring.io/projects/spring-cloud-gateway), and more.

Only a curated list of the [vast amount](http://geoserver.org/release/stable/) of GeoServer extensions will be supported, as they are verified and possibly adapted to work with this project's architecture.


# System Architecture

The following diagram depicts the System's general architecture:

![Cloud Native GeoServer Architecture Diagram](img/gs_cloud_architecture_diagram.svg  "Cloud Native GeoServer Architecture Diagram")

> - Hexagons represent microservices;
> - coloured rectangles, logical groupings of components;
> - lines connecting a group to another component: connector applies to all services of the outgoing end, to all components of the incoming end; 
> - white rectangles, components that are platform/deployment choices. For example:
>     - "Event bus" could be a cloud provider's native service (event queue), or a microservice implementing a distributed event broker;
>     - "Catalog/Config backend" is the software compoent used to access the catalog and configuration. Might be a microservice itself, catalog/config provider for  "data  directory", database, or other kind of external service store, catalog/config backend implementations;
>     - "Catalog/Config storage" is the storage mechanism that backs the catalog/config software component.  Might be a shared "data  directory" or database, a "per instance" data directory or database, and so on, depending on the available catalog/config backend implementations, and how they're configured and provisioned;
>     - "Geospatial data sources" is whatever method is used to access the actual data served up by the microservices.

Does that mean *GeoServer*'s `.war` is deployed several times, with each instance exposing a given "business capability"?
**ABSOLUTELY NOT**.
Each microservice is its own self-contained application, including only the GeoServer dependencies it needs. Moreover, care has been taken so that when a dependency has both required and non-required components, only the required ones are loaded.

> Note the above diagram represents the overall system's architecture. This is not a deployment diagram. Deployment involves choice of platforms, configurations, and more; without affecting the general architecture.
> Some microservices/components, though planned and represented in the architecture diagram, have not yet been developed/integrated. For instance: the logging, tracing, and monitoring components, as well as the GWC and WPS microservices.

    
## Components Overview

* Front services:
    * Gateway
    * Monitoring
* Infrastructure:
    * Discovery
    * Config
    * Event bus
    * Logging
    * Tracing
    * Cache
* GeoServer:
     * Catalog
     * OWS services
     * REST API service
     * Web-UI service
     * GWC service

# Building

## Requirements:

 * Java >= 11 JDK
 * [Maven](https://maven.apache.org/) >= `3.6.3`
 * [Docker](https://docs.docker.com/engine/install/) version >= `19.03.3`
 * [docker-compose](https://docs.docker.com/compose/) version >= `1.26.2`

*CN GeoServer* uses [Apache Maven](http://maven.apache.org/) (included) for a build system.

You need to have [docker](https://www.docker.com/) and [docker-compose](https://docs.docker.com/compose/install/) installed.

## Build

> The main branch follows GeoServer's main branch, currently `2.19-SNAPSHOT`. 
> The stable branch builds against a released GeoServer version.
> Building master against the latest stable version (`2.18.2`) is no longer possible due to binary incompatible between `2.18.x` and `2.19.x`. Once 2.19.0 is released, it'll be possible to build against a stable version activating the `geoserver_stable_version` profile as follows:
>    `./mvnw clean install -P geoserver_stable_version`

To build the applications run the following command from the root project directory:

    ./mvnw clean install

That will compile, run unit and integration tests, install maven artifacts to the local maven repository, and create all microservices docker images.
The maven build uses the `com.spotify:dockerfile-maven-plugin` maven plugin to build the microservice docker images.

The simple build command above creates the following docker images:

```bash
$ docker images
REPOSITORY                                       TAG                 IMAGE ID            CREATED             SIZE
org.geoserver.cloud/gs-cloud-catalog            0.2-SNAPSHOT        cd7159216be8        About an hour ago   406MB
org.geoserver.cloud/gs-cloud-config-service     0.2-SNAPSHOT        28f4f4f9ff35        25 hours ago        332MB
org.geoserver.cloud/gs-cloud-database           0.2-SNAPSHOT        0022bb2d2a1e        6 weeks ago         491MB
org.geoserver.cloud/gs-cloud-discovery-service  0.2-SNAPSHOT        827e3ebde911        25 hours ago        334MB
org.geoserver.cloud/gs-cloud-gateway            0.2-SNAPSHOT        55ecab20b51e        25 hours ago        330MB
org.geoserver.cloud/gs-cloud-restconfig-v1      0.2-SNAPSHOT        d1aa8a3495a1        About an hour ago   432MB
org.geoserver.cloud/gs-cloud-wcs                0.2-SNAPSHOT        580b2336ab02        About an hour ago   416MB
org.geoserver.cloud/gs-cloud-web-ui             0.2-SNAPSHOT        da1e714ff851        About an hour ago   461MB
org.geoserver.cloud/gs-cloud-wfs                0.2-SNAPSHOT        6f296b3ba198        About an hour ago   427MB
org.geoserver.cloud/gs-cloud-wms                0.2-SNAPSHOT        294ab913aaf4        About an hour ago   439MB
org.geoserver.cloud/gs-cloud-wps                0.2-SNAPSHOT        07135b861814        About an hour ago   440MB
```

To run the build without building the docker images, disable the `docker` maven profile:

```bash
$ ./mvnw clean install -P\!docker
```

# Running

## docker-compose

Now run the docker composition as follows, the first time it might need to download some additional images for the `rabbitmq` event broker and the `postgresql` config database:

```bash
$ docker-compose up -d
```

Run `docker-compose logs -f` to watch startup progress of all services.

Watch the output of `docker-compose ps` until all services are healthy:

```bash
$ docker-compose ps
       Name                      Command                  State                   Ports                                                      
-----------------------------------------------------------------------------------------------------------------
gscloud_catalog_1     dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_config_1      dockerize -wait http://dis ...   Up (healthy)                                                                                                                   
gscloud_database_1    docker-entrypoint.sh postgres    Up (healthy)   0.0.0.0:5432->5432/tcp                                                                                          
gscloud_discovery_1   /bin/sh -c exec java $JAVA ...   Up (healthy)   0.0.0.0:8761->8761/tcp                                                                                          
gscloud_gateway_1     dockerize -wait http://con ...   Up (healthy)   0.0.0.0:9090->8080/tcp                                                                                          
gscloud_rabbitmq_1    docker-entrypoint.sh rabbi ...   Up             15671/tcp, 0.0.0.0:15672->15672/tcp, ...
gscloud_rest_1        dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_wcs_1         dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_webui_1       dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_wfs_1         dockerize --timeout 60s -w ...   Up (healthy)                                                                                                                   
gscloud_wms_1         dockerize -wait http://con ...   Up (healthy)                     
```

Now you can access all front-services (`wms`, `wfs`, `wcs`, `rest`, and `webui`) through the `gateway` service at [http://localhost:9090](http://localhost:9090)

## Running a service in development/debug mode

Running a single service in "local" mode (that is, outside the docker composition) can be done either through the command line or through the IDE.

First, make sure at least the essential infrastructure services are running:

```bash
$ docker-compose up -d discovery rabbitmq config database catalog gateway
```

> The `gateway` service is not essential, but useful to check it's correctly proxy'ing requests to your locally running services as well as the ones in the docker composition.

To run a specific service through the command line, for example, `wfs-service`, run:

```bash
$ ./mvnw -f services/wfs spring-boot:run -Dspring-boot.run.profiles=local
```

To run a service through the IDE, execute the specific application class (for example, `org.geoserver.cloud.wfs.app.WfsApplication`), which is a regular Java class with a `main()` method, passing the JVM argument `-Dspring-boot.run.profiles=local`.

The "local" spring profile in each `conifg/<service>.yml` file sets a different hard-coded port for each service, which aids in debugging a locally running service:

* `catalog-service`: [9100](http://localhost:9100)
* `wfs-service`: [9101](http://localhost:9101)
* `wms-service`: [9102](http://localhost:9102)
* `wcs-service`: [9103](http://localhost:9103)
* `wps-service`: [9100](http://localhost:9104)
* `restconfig-v1`: [9105](http://localhost:9105)
* `web-ui`: [9106](http://localhost:9106)

At startup time, as configured in its `src/main/resources/bootstrap.yml` file, the service will contact the `discovery-service` at the default `http://localhost:8761/eureka` location, given there's no `eureka.server.url` configuration property set (which is otherwise provided by `docker-compose.yml`).
Since `docker-compose.yml` exposes the `discovery-service` at the local port `8761`, that's all the service being run locally needs to engage in the cluster. The discovery service will provide it with the location of any other service it needs to contact, starting with the `config-service`, where it will ultimatelly get the rest of the application configuration from.


