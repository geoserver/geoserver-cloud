# Cloud Native GeoServer

*Cloud Native GeoServer* is GeoServer ready to use in the cloud through dockerized microservices.

This project is an opinionated approach over how to split [GeoServer](http://geoserver.org/)'s services and API offerings into
a microservices based architecture.

Only a curated list of the [vast amount](http://geoserver.org/release/stable/) of GeoServer extensions will be supported, as they
are verified to work with this project's architecture, or adapted to do so.

## License

*CN GeoServer* licensed under the [GPLv2](LICENSE.txt).

## TL;DR

```bash
$ ./mvnw install
$ docker-compose up -d
$ curl "http://localhost:9090/ows?request=getcapabilities&service={WMS,WFS,WCS}"
$ curl -u admin:geoserver "http://localhost:9090/rest/workspaces.json"
```
Browse to [http://localhost:9090/web](http://localhost:9090/web)

**Note** when accessing the web-ui through the gateway service as in the above URL, once you log in, it will redirect you to the service's internal address instead of staying at `localhost:9090`. There's a [known bug](https://github.com/camptocamp/geoserver-microservices/issues/14) for it and it'll be solved as soon as possible.

## Services Architecture

![Cloud Native GeoServer Architecture Diagram](gs_cloud_architecture_diagram.svg  "Architecture Diagram")
*Cloud Native GeoServer Architecture Diagram*

The above diagram depicts the overall system's architecture. This is not a deployment diagram. Deployment involves choice of platforms, configurations, and more; without affecting the general architecture.

- Hexagons represent microservice components;
- lines connecting a group to another component: connector applies to all services of the outgoing end, to all components of the incoming end; 
- coloured rectangles, logical groupings of components;
- white rectangles, components that are platform/deployment choices. For example:
    - "Event bus" could be a cloud provider's native service (event queue), or a microservice implementing a distributed event broker;
    - "Catalog/Config backend" is the software compoent used to access the catalog and configuration. Might be a microservice itself, a shared "data directory" or database, a "per instance" data directory or database, and so on, depending on the available catalog/config backend implementations;
    - "Catalog/Config storage" is the storage mechanism that backs the catalog/config software component. 
    - "Geospatial data sources" is whatever method is used to access the actual data served up by the microservices.
    
Note: so "Discovery" and "Config" could be cloud provider's native services or microservices. Might need updating the diagram...

TODO: provide more detail?

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

## Building

### Requirements
 * Java 11 JDK
 * Maven `3.6.3`+ (included through the `mvnw` wrapper in the root folder)
 * Docker version `19.03.3`+
 * docker-compose version `1.26.2`+
 
 
*CN GeoServer* uses [Apache Maven](http://maven.apache.org/) (included) for a build system.

You need to have [docker](https://www.docker.com/) and [docker-compose](https://docs.docker.com/compose/install/) installed, the maven build uses the `com.spotify:dockerfile-maven-plugin` maven plugin to build the microservice docker images.

To build the application run maven from the root project directory run

    ./mvnw clean install

The main branch follows GeoServer's main branch, currently `2.18-SNAPSHOT`. It's also possible to build against the latest stable version, as follows:

    ./mvnw clean install -Dgs.version=2.17.2

## Running

The simple build command above created the docker images:

```bash
$ docker images
REPOSITORY                                       TAG                 IMAGE ID            CREATED             SIZE
org.geoserver.cloud/gs-cloud-web-ui              0.2-SNAPSHOT        3ee022e04cd9        4 minutes ago       542MB
org.geoserver.cloud/gs-cloud-wcs                 0.2-SNAPSHOT        ec6491202f64        4 minutes ago       406MB
org.geoserver.cloud/gs-cloud-wms                 0.2-SNAPSHOT        4af7d13eee37        4 minutes ago       406MB
org.geoserver.cloud/gs-cloud-wfs                 0.2-SNAPSHOT        1c294efe758d        4 minutes ago       404MB
org.geoserver.cloud/gs-cloud-restconfig-v1       0.2-SNAPSHOT        b40943fc5ce5        4 minutes ago       407MB
org.geoserver.cloud/gs-cloud-wps                 0.2-SNAPSHOT        e798434ae567        4 minutes ago       421MB
org.geoserver.cloud/gs-cloud-config-service      0.2-SNAPSHOT        6d17aa0d548f        5 minutes ago       317MB
org.geoserver.cloud/gs-cloud-gateway             0.2-SNAPSHOT        a41cb65d49cd        5 minutes ago       315MB
org.geoserver.cloud/gs-cloud-discovery-service   0.2-SNAPSHOT        d7ddfabfc652        5 minutes ago       318MB

```

Now run the docker composition as follows, the first time it might need to download some additional images for the `rabbitmq` event broker and the `postgresql` config database:

```bash
$ docker-compose up -d
```

Run `docker-compose logs -f` to watch startup progress of all services, in the end, `docker-compose ps`'s output should look like:

```bash
       Name                      Command                  State                                                            Ports                                                      
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
gscloud_config_1      dockerize -wait http://dis ...   Up (healthy)                                                                                                                   
gscloud_database_1    docker-entrypoint.sh postgres    Up (healthy)   0.0.0.0:5432->5432/tcp                                                                                          
gscloud_discovery_1   /bin/sh -c exec java $JAVA ...   Up (healthy)   0.0.0.0:8761->8761/tcp                                                                                          
gscloud_gateway_1     dockerize -wait http://con ...   Up (healthy)   0.0.0.0:9090->8080/tcp                                                                                          
gscloud_rabbitmq_1    docker-entrypoint.sh rabbi ...   Up             15671/tcp, 0.0.0.0:15672->15672/tcp,...
gscloud_rest_1        dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_wcs_1         dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_webui_1       dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_wfs_1         dockerize -wait http://con ...   Up (healthy)                                                                                                                   
gscloud_wms_1         dockerize -wait http://con ...   Up (healthy)
```

Now you can access all front-services (`wms`, `wfs`, `wcs`, `rest`, and `webui`) through the `gateway` service at [http://localhost:9090](http://localhost:9090)

## Bugs

*CN GeoServer*'s issue tracking is at this [GitHub](https://github.com/camptocamp/geoserver-microservices/issues) repository.

## Roadmap

Follow the development progress on these [GitHub Kanban boards](https://github.com/camptocamp/geoserver-microservices/projects)


## Contributing

Please read [the contribution guidelines](CONTRIBUTING.md) before contributing pull requests to the CN GeoServer project.


## Status

Still an early stage, working towards release [0.2.0](https://github.com/camptocamp/geoserver-microservices/projects/1).

Passed the feasibility exploration prototype phase by `0.1.0` and we're confident this is a good path towards the main objective of having cloud-native,
independently scalable GeoServer microservices.

`0.2.0` will focus on wrapping up integration improvements related to catalog and configuration back-end pluggability, including a microservice to serve as the catalog and resource store for all front-end services.

We're currently using GeoServer's `jdbcconfig` and `jdbcstore` community modules to host the catalog and resources on a PostgreSQL database, included in the docker-composition provided at the root directory. If you spawn too many instances (e.g. `docker-compose scale wfs=10`), some will fail to connect as the maximum number of allowed database server connections is reached by the aggregated connection pools from all service instances. The `catalog-service` will solve this situation, providing a unified abstraction layer for the rest of services, so that the actual catalog backend needs to be configured only on the `catalog-service`, while its clients use a catalog and resource store plugin that talks to the `catalog-service`.

The use `jdbcconfig` and `jdbcstore` has been customized to allow for spring-boot style configuration through `application.yml` or `application.properties` files, served up by the `config-service` from the `config/` directory. That said, it is envisioned to develop alternate, more scalable, catalog backend plugins.

We're using [spring-cloud-bus](https://cloud.spring.io/spring-cloud-static/spring-cloud-bus/3.0.0.M1/reference/html/) to coordinate

By [1.0.0](https://github.com/camptocamp/geoserver-microservices/milestone/2) at the end of September 2020 we should be able to deploying to Kubernetes using Kubernetes native services for service discovery and externalized configuration.

Nonetheless the chosen architecture aims to allow for a number of containerized application platforms such as:

- Single server docker-compose 
- Kubernetes
- Docker swarm

And deployment/configuration choices like:

- Shared data directory
- Per-service data directory with event-based synchronization
- Per-service access to catalog/config backend (e.g. all of them connecting directly to the config database)
- Catalog microservice: all services use the `catalog-service` client as their catalog, and this in turn talks to the actual backend, which, since the `catalog-service` itself can be scaled out, its backend can be any of the above options.

Feel free to try it out following the above instructions and report bugs [here](https://github.com/camptocamp/geoserver-microservices/issues).

