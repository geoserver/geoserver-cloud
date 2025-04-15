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
        |    |_ base-images/ ......................... Base Docker images for containerization
        |    |     |_ geoserver/ ..................... Base image for GeoServer services
        |    |     |_ jre/ ........................... Base JRE image
        |    |     |_ spring-boot/ ................... Base Spring Boot image (Boot 2.x)
        |    |     |_ spring-boot3/ .................. Base Spring Boot image (Boot 3.x)
        |    |
        |    |_ infrastructure/ ...................... Infrastructure services
        |    |     |_ admin/ ......................... Spring-cloud admin service
        |    |     |_ config/ ........................ Spring-cloud config service
        |    |     |_ discovery/...................... Spring-cloud discovery service
        |    |     |_ gateway/ ....................... Spring-cloud gateway service
        |    |
        |    |_ geoserver/ ........................... Root directory for geoserver based microservices
        |          |_ gwc/ ........................... GeoWebcache Service
        |          |_ restconfig/ .................... GeoServer REST config API Service
        |          |_ wcs/ ........................... Web Coverage Service
        |          |_ webui/ ......................... GeoServer administration Web User Interface
        |          |_ wfs/ ........................... Web Feature Service
        |          |_ wms/ ........................... Web Map Service
        |          |_ wps/ ........................... Web Processing Service
        |
        |_ catalog/ .................................. Root directory for GeoServer Catalog and Config libraries
        |    |_ backends/ ............................ Spring Boot AutoConfigurations for specific catalog back-ends
        |    |     |_ common/ ........................ Basic catalog and config bean wiring common to all back-ends
        |    |     |_ datadir/ ....................... Shared "data directory" catalog back-end
        |    |     |_ jdbcconfig/ .................... "jdbcconfig" catalog back-end
        |    |     |_ pgconfig/ ...................... PostgreSQL catalog back-end
        |    |
        |    |_ cache/ ............................... Spring Boot JCache support and auto-configurations for the Catalog
        |    |_ events/ .............................. No-framework object model and runtime for catalog and config application events
        |    |_ event-bus/ ........................... Integration layer for events with Spring Cloud Bus
        |    |_ jackson-bindings/ .................... Libraries to encode and decode configuration objects as JSON
        |    |     |_ geoserver/ ..................... Jackson bindings for GeoServer Catalog and Config object model
        |    |     |_ geotools/ ...................... Jackson bindings for JTS Geometries and org.opengis.filter.Filter
        |    |     |_ starter/ ....................... Spring Boot starter module to automate GeoTools and GeoServer Jackson bindings
        |    |
        |    |_ plugin/ .............................. Core Catalog and Config implementation and extensions
        |
        |_ extensions/ ............................... GeoServer Cloud extension modules 
        |    |_ app-schema/ .......................... Application Schema extension
        |    |_ core/ ................................ Core extension module
        |    |_ css-styling/ ......................... CSS Styling extension
        |    |_ importer/ ............................ Importer extension
        |    |_ mapbox-styling/ ...................... MapBox Styling extension
        |    |_ input-formats/ ....................... Input format extensions
        |    |     |_ raster-formats/ ................ Raster formats extensions
        |    |     |_ vector-formats/ ................ Vector formats extensions
        |    |
        |    |_ ogcapi/ ............................... OGC API extensions
        |    |     |_ features/ ...................... OGC API Features extension
        |    |
        |    |_ security/ ............................ Security extensions
        |    |     |_ auth-key/ ...................... Auth Key security extension
        |    |     |_ gateway-shared-auth/ ........... Gateway Shared Auth security extension
        |    |     |_ geonode-oauth2/ ................ GeoNode OAuth2 security extension
        |    |     |_ geoserver-acl/ ................. GeoServer ACL security extension
        |    |     |_ jdbc/ .......................... JDBC security extension
        |    |     |_ ldap/ .......................... LDAP security extension
        |    |
        |    |_ output-formats/ ....................... Output format extensions
        |          |_ vector-tiles/ ................... Vector Tiles extension
        |          |_ dxf/ ........................... DXF Vector format extension
        |
        |_ gwc ....................................... GeoWebCache modules
        |    |_ autoconfigure/ ....................... Auto configurations for all GWC functionalities
        |    |_ backends/ ............................ Integration of GWC storage backends (File, S3, Azure, etc.)
        |    |     |_ pgconfig/ ...................... PostgreSQL GWC backend
        |    |
        |    |_ blobstores/ .......................... Blobstore implementations
        |    |_ core/ ................................ Basic integration for GeoWebCache with GeoServer Cloud
        |    |_ integration-bus/ ..................... Integration layer for GWC application events with Spring Cloud Bus
        |    |_ services/ ............................ Support for GWC services integration (TMS, WMTS, etc.)
        |    |_ starter/ ............................. Spring Boot starter for integrating GWC aspects with services
        |
        |_ library/ .................................. Common library modules
        |    |_ spring-boot-simplejndi/ .............. Simple JNDI implementation for Spring Boot
        |    |_ spring-factory/ ...................... Spring Factory utility
        |
        |_ starters .................................. Spring Boot starters for GeoServer microservices
        |    |_ catalog-backend/ ..................... Groups all supported catalog back-ends
        |    |_ input-formats/ ....................... Unified starter for all vector and raster input data formats
        |    |_ event-bus/ ........................... Binds Spring Cloud Bus integration layer with a concrete event-bridge
        |    |_ extensions/ .......................... Common extension functionality
        |    |_ output-formats/ ....................... Output format extensions starter
        |    |_ observability/ ....................... Observability support (Boot 2.x)
        |    |_ observability-spring-boot-3/ ......... Observability support (Boot 3.x)
        |    |_ security/ ............................ Security extensions and configurations
        |    |_ spring-boot/ ......................... Basic Spring Boot integration (Boot 2.x)
        |    |_ spring-boot3/ ........................ Basic Spring Boot integration (Boot 3.x)
        |    |_ webmvc/ .............................. Spring Cloud and GeoServer integrations for web applications
        |    |_ wms-extensions/ ...................... WMS extensions autoconfigurations
        |
        |_ integration-tests ......................... Integration tests
```

# Building

Check out the [build instructions](build_instructions.md) document.

# Coding Standards

GeoServer Cloud follows specific [coding standards and style guidelines](coding_standards.md) to ensure consistency across the project.

# Creating Extensions

Learn how to create [extensions for GeoServer Cloud](extensions/adding_extensions.md).

## Running for development and testing

The `./compose` folder contains docker-compose files intended only for **development**.

> For instructions on running GeoServer Cloud in your environment, follow the [Quick Start](https://geoserver.org/geoserver-cloud/#quick-start) guide on the user guide.

### Run as non-root

First thing first, edit the `.env` file to set the `GS_USER` variable to the user and group ids the applications should run as.

Usually the GID and UID of your user, such as:

```
echo `id -g`:`id -u`
1000:1000
```

### Choose your Catalog and Configuration back-end

You need to run `compose.yml` and pick one compose override file for a given GeoServer Catalog and Configuration back-end.

#### DataDirectory Catalog back-end

The `datadir` spring boot profile enables the traditional "data directory" catalog back-end,
with all GeoServer containers sharing the same directory. On a k8s deployment you would need a
`ReadWriteMany` persistent volume.

GeoServer-Cloud can start from an empty data directory.

The `catalog-datadir.yml` docker compose override enables the `datadir` profile and
initializes a volume with the default GeoServer release data directory.

Run with:

```bash
$ docker compose -f compose.yml -f catalog-datadir.yml
```

or the more convenient shell script:

```bash
$ ./datadir up -d
```

#### PostgreSQL Catalog back-end

The `pgconfig` spring boot profile enables the PostgreSQL catalog back-end.

This is the preferred Catalog back-end for production deployments,
and requires a PostgreSQL 15.0+ database

The `catalog-pgconfig.yml` docker compose override enables the `pgconfig` profile and
sets up a PostgreSQL container named `pgconfigdb`.

> On a production deployment, it is expected that the database is a provided service
and not part of the GeoServer Cloud deployment.

Run with:

```bash
$ docker compose -f compose.yml -f catalog-pgconfig.yml
```

Or the more convenient shell script:

```
$ ./pgconfig up -d
```

**PGBouncer**:

Given the `pgconfig` catalog back-end will set up a database connection pool on each container,
when scaling out you might run out of available connections in the Postgres server. A good way
to avoid that and make better use of resources is to use a connection pooling service, such
as [pgbouncer](https://www.pgbouncer.org/).

Use the `catalog-pgconfig.yml` in combination with the `pgbouncer.yml` docker compose override. `pgbouncer.yml`
will override the three database containers with separate pgbouncer instances for each:

* `pgconfigdb` becomes a `pgbouncer` container pointing to the `pgconfigdb_pg` container.
* `acldb` becomes a `pgbouncer` container pointing to the `acldb_pg` container, and holds the [GeoServer ACL](https://github.com/geoserver/geoserver-acl) database
* `postgis` becomes a `pgbouncer` container pointing to the `postgis_pg` container.

> The `postgis` is container used to host sample data, it is not required but useful during development.

#### Access GeoServer

Verify the services are running with `docker compose ps` or `docker ps` as appropriate.

```
$ curl "http://localhost:9090/geoserver/cloud/ows?request=getcapabilities&service={WMS,WFS,WCS,WPS}"
$ curl -u admin:geoserver "http://localhost:9090/geoserver/cloud/rest/workspaces.json"
```

Browse to [http://localhost:9090/geoserver/cloud/](http://localhost:9090/geoserver/cloud/)

> Note the `/geoserver/cloud` context path is set up in the `gateway-service`'s externalized
> configuration, and enforced through the `GEOSERVER_BASE_PATH` in `compose.yml`.
> You can change it to whatever you want. The default [gateway-service.yml](https://github.com/geoserver/geoserver-cloud-config/blob/master/gateway-service.yml)
> configuration file does not set up a context path at all, and hence GeoServer will
> be available at the root URL.

---

# Running for development

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

At startup time, as configured in its `src/main/resources/bootstrap.yml` file, the service will contact the `discovery-service` at the default `http://localhost:8761/eureka` location, given there's no `eureka.server.url` configuration property set (which is otherwise provided by `compose.yml`).
Since `compose.yml` exposes the `discovery-service` at the local port `8761`, that's all the service being run locally needs to engage in the cluster. The discovery service will provide it with the location of any other service it needs to contact, starting with the `config-service`, where it will ultimatelly get the rest of the application configuration from.


