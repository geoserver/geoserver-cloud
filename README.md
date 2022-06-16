[![Build and Push Docker images](https://github.com/geoserver/geoserver-cloud/actions/workflows/build-and-push.yaml/badge.svg)](https://github.com/geoserver/geoserver-cloud/actions/workflows/build-and-push.yaml)

# Cloud Native GeoServer

*Cloud Native GeoServer* is  [GeoServer](http://geoserver.org/) ready to use in the cloud through dockerized microservices.

This project is an opinionated effort to split *GeoServer*'s geospatial services and API offerings as individually deployable components of a [microservices based architecture](https://microservices.io/).

As such, it builds on top of existing *GeoServer* software components, adapting and/or extending them in an attempt to achieve functional decomposition by business capability; which roughly means each OWS service, the Web UI, the REST API, and probably other components such as the *Catalog and Configuration subsystem*, become self-contained, individually deployable and scalable micro-services.



https://user-images.githubusercontent.com/207423/144188466-54a1695f-129e-44c2-b6d6-09bf34b96f84.mp4



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

 * Java >= 17 JDK
 * [Maven](https://maven.apache.org/) >= `3.6.3`
 * [Docker](https://docs.docker.com/engine/install/) version >= `19.03.3`
 * [docker-compose](https://docs.docker.com/compose/) version >= `1.26.2`

The simple `./mvnw install` command from the project root directory will
build and install all the required components, including upstream GeoServer
dependencies and GeoServer-Cloud Docker images.

If its your first run, you may want to build without running tests to
speed up the build, not including a full upstream GeoServer build. Read
the sections bellow for more information.

```bash
$ ./mvnw clean install -DskipTests
```

### Custom upstream GeoServer version

*Cloud Native GeoServer* depends on a custom GeoServer branch,
`geoserver-cloud_integration`, which contains patches to upstream
GeoServer that have not yet been integrated into the mainstream
`main` branch. Additionally, the `geoserver-cloud_integration`
GeoServer branch changes the artifact versions from `2.21-SNAPSHOT`
to `2.21.0-CLOUD`, to avoid confusing maven if you also work
with vanilla GeoServer, and to avoid your IDE downloading the
latest `2.21-SNAPSHOT` artifacts from the OsGeo maven repository,
overriding your local maven repository ones, and having
confusing compilation errors that would require re-building
the branch we need.

The `geoserver-cloud_integration` branch is checked out as a
submodule under the `geoserver_submodule/geoserver` directory.

The root `pom.xml` defines a `geoserver` maven profile, active
by default, that includes the module `geoserver_submodule`, which
in turn includes all the required `geoserver` modules for this project.

So in general, you may chose to only eventually build the
`geoserver_submodule` subproject, since it won't change
frequently, with

```bash
./mvnw clean install -f geoserver_submodule -DskipTests
```

### Targeted builds

*Cloud Native GeoServer*-specific modules source code
is under the `src/` directory.

When you already have the `2.21.0-CLOUD` GeoServer artifacts,
you can chose to only build these projects, either by:


```bash
$ ./mvnw clean install -f src/
```

Or 

```bash
$ cd src/
$ ./mvnw clean install
```

## Running

The `docker-compose.yml` file and the accompanying overrides
`docker-compose-shared_datadir.yml`, `docker-compose-jdbcconfig.yml`,
and `docker-compose-standalone.yml` at the project's root
directory are meant for development and testing purposes, not
for production use.

You'll find more production-suitable deployment files for
docker-compose and podman under the [docs/deploy](docs/deploy) folder.

Also, a ready-to-use Helm chart for Kubernetes is available
at the [camptocamp/helm-geoserver-cloud](https://github.com/camptocamp/helm-geoserver-cloud)
Github repository.

### Development runs

To run the development docker composition using a shared data directory.
GeoServer-Cloud can start from an empty directory.

```bash
$ mkdir docker-compose_datadir
$ alias dcd="docker-compose -f docker-compose.yml -f docker-compose-shared_datadir.yml"
$ dcd up -d
```

Verify the services are running with `dcd ps`.
Healthckecks use `curl` hitting the `http:localhost:8081/actuator/health`.
The services run on the `8080` port, and are exposed using different
host ports. The spring-boot-actuator is set up at port `8081`.

The `gateway-service` proxies requests from the `9090` local port:

```
$ curl "http://localhost:9090/geoserver/cloud/ows?request=getcapabilities&service={WMS,WFS,WCS}"
$ curl -u admin:geoserver "http://localhost:9090/geoserver/cloud/rest/workspaces.json"
```

Browse to [http://localhost:9090/geoserver/cloud](http://localhost:9090/geoserver/cloud)

> Note the `/geoserver/cloud` context path is set up in the `gateway-service`'s externalized
> configuration, and enforced through the `GEOSERVER_BASE_PATH` in `docker-compose.yml`.
> You can change it to whatever you want. The default [config/gateway-service.yml](config/gateway-service.yml)
> configuration file does not set up a context path at all, and hence GeoServer will
> be available at the root URL.

## Contributing

Please read [the contribution guidelines](CONTRIBUTING.md) before contributing pull requests to the CN GeoServer project.

Follow the [developer's guide](docs/develop/index.md) to know more about the project's technical details.

## Status

`v1.0-RC21` released against GeoServer `2.21-SNAPSHOT`.

Read the [changelog](https://github.com/geoserver/geoserver-cloud/releases/tag/v1.0-RC14) for more information.

## Bugs

*CN GeoServer*'s issue tracking is at this [GitHub](https://github.com/geoserver/geoserver-cloud/issues) repository.

## Roadmap

Follow the development progress on these [GitHub Kanban boards](https://github.com/geoserver/geoserver-cloud/projects)


