# Cloud Native GeoServer

*Cloud Native GeoServer* is  [GeoServer](http://geoserver.org/) ready to use in the cloud through dockerized microservices.

This project is an opinionated effort to split *GeoServer*'s geospatial services and API offerings as individually deployable components of a [microservices based architecture](https://microservices.io/).

As such, it builds on top of existing *GeoServer* software components, adapting and/or extending them in an attempt to achieve functional decomposition by business capability; which roughly means each OWS service, the Web UI, the REST API, and probably other components such as the *Catalog and Configuration subsystem*, become self-contained, individually deployable and scalable micro-services.

## Architecture

The following diagram depicts the System's general architecture.

<img src="docs/img/gs_cloud_architecture_diagram.svg" alt="Cloud Native GeoServer Architecture Diagram" width="740" />

*Cloud Native GeoServer Architecture Diagram*

Does that mean *GeoServer*'s `.war` is deployed several times, with each instance exposing a given "business capability"?
ABSOLUTELY NOT.
Each microservice is its own self-contained application, including only the GeoServer dependencies it needs. Moreover, care has been taken so that when a dependency has both required and non-required components, only the required ones are loaded.

## Technology

With *GeoServer* being a traditional, [Spring Framework](https://spring.io/) based, monolithic servlet application, a logical choice has been made to base the *GeoServer* derived microservices in the [Spring Boot](https://spring.io/projects/spring-boot) framework.

Additionally, [Spring Cloud](https://spring.io/projects/spring-cloud) technologies enable crucial capabilities such as [dynamic service discovery](https://spring.io/projects/spring-cloud-netflix), [externalized configuration](https://spring.io/projects/spring-cloud-config), [distributed events](https://spring.io/projects/spring-cloud-bus), [API gateway](https://spring.io/projects/spring-cloud-gateway), and more.

Only a curated list of the [vast amount](http://geoserver.org/release/stable/) of GeoServer extensions will be supported, as they are verified and possibly adapted to work with this project's architecture.

## License

*CN GeoServer* licensed under the [GPLv2](LICENSE.txt).

## Building

Requirements:

 * Java >= 11 JDK
 * [Maven](https://maven.apache.org/) >= `3.6.3`
 * [Docker](https://docs.docker.com/engine/install/) version >= `19.03.3`
 * [docker-compose](https://docs.docker.com/compose/) version >= `1.26.2`

Build, test, and create docker images:

```bash
$ ./mvnw clean install
```

## Running

```bash
$ docker-compose --compatibility up -d
$ curl "http://localhost:9090/ows?request=getcapabilities&service={WMS,WFS,WCS}"
$ curl -u admin:geoserver "http://localhost:9090/rest/workspaces.json"
```
Browse to [http://localhost:9090](http://localhost:9090)

> Note the `--compatibility` argument is required when using a v3 docker-compose file, in order to enable resource limiting (CPU and Memory).
> Otherwise all services would be competing to use all available CPUs.
> For more information, check the [compatibility mode](https://docs.docker.com/compose/compose-file/compose-versioning/#compatibility-mode) and [resources](https://docs.docker.com/compose/compose-file/compose-file-v3/#resources) sections in the docker-compose v3 documentation.

## Contributing

Please read [the contribution guidelines](CONTRIBUTING.md) before contributing pull requests to the CN GeoServer project.

<!-- Follow the [developer's guide](CONTRIBUTING.md)  to know more about the project's technical details. -->

## Status

`v0.2.0` released against GeoServer `2.18.2`.

Project is being deployed in production since `v0.1.0`.

Read the [changelog](CHANGELOG.md) for more information.

## Bugs

*CN GeoServer*'s issue tracking is at this [GitHub](https://github.com/camptocamp/geoserver-microservices/issues) repository.

## Roadmap

Follow the development progress on these [GitHub Kanban boards](https://github.com/camptocamp/geoserver-microservices/projects)


