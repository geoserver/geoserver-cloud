# Developer's Guide

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

![Cloud Native GeoServer Architecture Diagram](../img/gs_cloud_architecture_diagram.svg  "Cloud Native GeoServer Architecture Diagram")

> - Hexagons represent microservices;
> - coloured rectangles, logical groupings of components;
> - lines connecting a group to another component: connector applies to all services of the outgoing end, to all components of the incoming end; 
> - white rectangles, components that are platform/deployment choices. For example:
>     - "Event bus" could be a cloud provider's native service (event queue), or a microservice implementing a distributed event broker;
>     - "Catalog/Config backend" is the software component used to access the catalog and configuration. Might be a microservice itself, catalog/config provider for  "data  directory", database, or other kind of external service store, catalog/config backend implementations;
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

# Project source code structure


```
        src/ ......................................... Project source code root directory
        |_ apps ...................................... Root directory for microservice applications
        |    |_ infrastructure/ ...................... Infrastructure services
        |    |     |_ config/ ........................ Spring-cloud config service
        |    |     |_ gateway/ ....................... Spring-cloud gateway service
        |    |     |_ discovery/...................... Spring-cloud discovery service
        |    |     |_ admin/ ......................... Spring-cloud admin service
        |    |
        |    |_ geoserver/ ........................... Root directory for geoserver based microservices
        |          |_ wms/ ........................... Web Map Service
        |          |_ wfs/ ........................... Web Feature Service
        |          |_ wcs/ ........................... Web Coverage Service
        |          |_ wps/ ........................... Web Processing Service
        |          |_ gwc/ ........................... GeoWebcache Service
        |          |_ restconfig/ .................... GeoServer REST config API Service
        |          |_ webui/ ......................... GeoServer administration Web User Interface
        |
        |_ catalog/ .................................. Root directory for GeoServer Catalog and Config libraries
        |    |_ plugin/ .............................. Core Catalog and Config implementation and extensions
        |    |
        |    |_ backends/ ............................ Spring Boot AutoConfigurations for specific catalog back-ends
        |    |     |_ common/ ........................ Basic catalog and config bean wiring common to all back-ends
        |    |     |_ datadir/ ....................... Shared "data directory" catalog back-end
        |    |     |_ jdbcconfig/ .................... "jdbcconfig" catalog back-end
        |    |
        |    |_ jackson-bindings/ .................... Libraries to encode and decode configuration objects as JSON
        |    |     |_ geotools/ ...................... Jackson bindings for JTS Geometries and org.opengis.filter.Filter
        |    |     |_ geoserver/ ..................... Jackson bindings for GeoServer Catalog and Config object model
        |    |     |_ starter/ ....................... Spring Boot starter module to automate GeoTools and GeoServer Jackson bindings
        |    |
        |    |_ cache/ ............................... Spring Boot JCache support and auto-configurations for the Catalog
        |    |_ events/ .............................. No-framework object model and runtime for catalog and config application events
        |    |_ event-bus/ ........................... Integration layer for events with Spring Cloud Bus
        |
        |_ gwc ....................................... GeoWebCache modules
        |    |_ core/ ................................ Basic integration for GeoWebCache with GeoServer Cloud
        |    |_ backends/ ............................ Integration of GWC storage backends (File, S3, Azure, etc.)
        |    |_ services/ ............................ Support for GWC services integration (TMS, WMTS, etc.)
        |    |_ tiling/ .............................. Support for GWC distributed tile cache seeding, agnostic of distributed events technologies
        |    |_ integration-bus/ ..................... Integration layer for GWC application events with Spring Cloud Bus
        |    |_ autoconfigure/ ....................... Auto configurations for all GWC functionalities
        |    |_ starter/ ............................. Spring Boot starter for integrating GWC aspects with services (webui, gwc-service, wms)
        |
        |_ starters .................................. Spring Boot starters for GeoServer microservices
        |    |_ spring-boot/ ......................... Basic Spring Boot integration and application startup logging
        |    |_ catalog-backend/ ..................... Groups all supported catalog back-ends
        |    |_ security/ ............................ Additional GeoServer security plugins (e.g. Authkey)
        |    |_ webmvc/ .............................. Spring Cloud and basic GeoServer integrations for spring-webmvc based applications
        |    |_ reactive/ ............................ Spring Cloud and basic GeoServer integrations for reactive (WebFlux-based) applications
        |    |_ event-bus/ ........................... Binds Spring Cloud Bus integration layer with a concrete event-bridge (RabbitMQ)
        |    |_ raster-formats/ ...................... Dependencies for all supported GeoSever raster formats (GeoTiff, ImageMosaic, etc.)
        |    |_ vector-formats/ ...................... Dependencies for all supported GeoSever vector formats (PostGIS, Shapefile, etc.)
        |    |_ wms-extensions/ ...................... WMS extensions autoconfigurations needed not only by wms-service (CSS, VectorTiles, etc.)
        |
        |_ integration-tests ......................... Integration tests
```

# Building

## Requirements:

 * Java >= 11 JDK
 * [Maven](https://maven.apache.org/) >= `3.6.3`
 * [Docker](https://docs.docker.com/engine/install/) version >= `19.03.3`
 * [docker-compose](https://docs.docker.com/compose/) version >= `1.26.2`

*CN GeoServer* uses [Apache Maven](http://maven.apache.org/) (included) for a build system.

You need to have [docker](https://www.docker.com/) and [docker-compose](https://docs.docker.com/compose/install/) installed.

## Build

Clone the repository, including submodules:

```bash
git clone --recursive /data2/groldan/git/geoserver-microservices
```

To build the applications run the following command from the root project directory:

```bash
./mvnw clean install
```

That will compile, run unit and integration tests, install maven artifacts to the local maven repository, and create all microservices docker images.
The maven build uses the `com.spotify:dockerfile-maven-plugin` maven plugin to build the microservice docker images.

The simple build command above creates the following docker images:

```bash
$ docker images|grep geoserver-cloud|sort
geoservercloud/geoserver-cloud-config                1.8.12        be987ff2a85e        42 minutes ago      319MB
geoservercloud/geoserver-cloud-discovery             1.8.12        abc5a17cf14c        42 minutes ago      320MB
geoservercloud/geoserver-cloud-gateway               1.8.12        10f267950c15        42 minutes ago      317MB
geoservercloud/geoserver-cloud-rest                  1.8.12        29406a1e1fdb        36 minutes ago      429MB
geoservercloud/geoserver-cloud-wcs                   1.8.12        c77ac22aa522        37 minutes ago      391MB
geoservercloud/geoserver-cloud-webui                 1.8.12        876d6fc3fac0        36 minutes ago      449MB
geoservercloud/geoserver-cloud-wfs                   1.8.12        62960137eb5a        38 minutes ago      410MB
geoservercloud/geoserver-cloud-wms                   1.8.12        6686ca90b552        38 minutes ago      437MB
geoservercloud/geoserver-cloud-wps                   1.8.12        73bae600226c        37 minutes ago      416MB
```

To run the build without building the docker images, disable the `docker` maven profile:

```bash
$ ./mvnw clean install -P-docker
```

# Running

## docker-compose

Now run the docker composition as follows, the first time it might need to download some additional images for the `rabbitmq` event broker and the `postgresql` config database:

```bash
$ docker compose up -d
```

Run `docker compose logs -f` to watch startup progress of all services.

Watch the output of `docker compose ps` until all services are healthy:

```bash
$ docker compose ps
       Name                      Command                  State                   Ports                                                      
-----------------------------------------------------------------------------------------------------------------
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
$ docker compose up -d discovery rabbitmq config database gateway
```

> The `gateway` service is not essential, but useful to check it's correctly proxy'ing requests to your locally running services as well as the ones in the docker composition.

To run a specific service through the command line, for example, `wfs-service`, run:

```bash
$ ./mvnw -f services/wfs spring-boot:run -Dspring-boot.run.profiles=local
```

To run a service through the IDE, execute the specific application class (for example, `org.geoserver.cloud.wfs.app.WfsApplication`), which is a regular Java class with a `main()` method, passing the JVM argument `-Dspring-boot.run.profiles=local`.

The "local" spring profile in each `config/<service>.yml` file sets a different hard-coded port for each service, which aids in debugging a locally running service:

* `wfs-service`: [9101](http://localhost:9101)
* `wms-service`: [9102](http://localhost:9102)
* `wcs-service`: [9103](http://localhost:9103)
* `wps-service`: [9100](http://localhost:9104)
* `restconfig-v1`: [9105](http://localhost:9105)
* `web-ui`: [9106](http://localhost:9106)

At startup time, as configured in its `src/main/resources/bootstrap.yml` file, the service will contact the `discovery-service` at the default `http://localhost:8761/eureka` location, given there's no `eureka.server.url` configuration property set (which is otherwise provided by `docker-compose.yml`).
Since `docker-compose.yml` exposes the `discovery-service` at the local port `8761`, that's all the service being run locally needs to engage in the cluster. The discovery service will provide it with the location of any other service it needs to contact, starting with the `config-service`, where it will ultimatelly get the rest of the application configuration from.


