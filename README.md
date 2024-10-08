[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=bugs)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=coverage)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=geoserver_geoserver-cloud&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=geoserver_geoserver-cloud)
[![Docker images](https://github.com/geoserver/geoserver-cloud/actions/workflows/build-and-push.yaml/badge.svg)](https://github.com/geoserver/geoserver-cloud/actions/workflows/build-and-push.yaml)

# GeoServer Cloud

*GeoServer Cloud* is  [GeoServer](http://geoserver.org/) ready to use in the cloud through dockerized microservices.

This project is an opinionated effort to split *GeoServer*'s geospatial services and API offerings as individually deployable components of a [microservices based architecture](https://microservices.io/).

As such, it builds on top of existing *GeoServer* software components, adapting and/or extending them in an attempt to achieve functional decomposition by business capability; which roughly means each OWS service, the Web UI, the REST API, and probably other components such as the *Catalog and Configuration subsystem*, become self-contained, individually deployable and scalable micro-services.

## Architecture

The following diagram depicts the system's general architecture.

<img src="docs/img/gs_cloud_architecture_diagram.svg" alt="GeoServer Cloud Architecture Diagram" width="740" />

*GeoServer Cloud Architecture Diagram*

Does that mean *GeoServer*'s `.war` is deployed several times, with each instance exposing a given "business capability"?
ABSOLUTELY NOT.
Each microservice is its own self-contained application, including only the GeoServer dependencies it needs. Moreover, care has been taken so that when a dependency has both required and non-required components, only the required ones are loaded.

## Technology

With *GeoServer* being a traditional, [Spring Framework](https://spring.io/) based, monolithic servlet application, a logical choice has been made to base the *GeoServer* derived microservices in the [Spring Boot](https://spring.io/projects/spring-boot) framework.

Additionally, [Spring Cloud](https://spring.io/projects/spring-cloud) technologies enable crucial capabilities such as [dynamic service discovery](https://spring.io/projects/spring-cloud-netflix), [externalized configuration](https://spring.io/projects/spring-cloud-config), [distributed events](https://spring.io/projects/spring-cloud-bus), [API gateway](https://spring.io/projects/spring-cloud-gateway), and more.

Only a curated list of the [vast amount](http://geoserver.org/release/stable/) of GeoServer extensions will be supported, as they are verified and possibly adapted to work with this project's architecture. The current version supports the following extensions:
- jdbc config
- jdbc store
- pgraster
- datadir-catalog-loader
- authkey authentication
- web-resource explorer
- css style
- mb style
- GWC S3 Storage
- GWC Azure Blob Storage
- Pregeneralized feature datastore
- vectortiles
- flatgeobuf
- dxf
- cog
- importer
- imagepyramid

Advanced ACL system is available through the project [GeoServer ACL](https://github.com/geoserver/geoserver-acl) which offers the same capacities as GeoFence.

OAuth is available by using the geOrchestra Gateway in replacement of the GeoServer Cloud one.

## License

*GeoServer Cloud* licensed under the [GPLv2](LICENSE.txt).

## Distribution and deployment

Docker images for all the services are available on DockerHub, under the [GeoServer Cloud organization](https://hub.docker.com/u/geoservercloud/).

You can find  production-suitable deployment files for docker-compose and podman under the [docs/deploy](docs/deploy) folder.

Also, a base Helm chart and examples for Kubernetes is available at the [camptocamp/helm-geoserver-cloud](https://github.com/camptocamp/helm-geoserver-cloud) Github repository.

## Contributing

Please read [the contribution guidelines](CONTRIBUTING.md) before contributing pull requests to the GeoServer Cloud project.

Follow the [developer's guide](docs/develop/index.md) to know more about the project's technical details.

## Status

`v1.8.11` released against GeoServer `2.25.3`.

Read the [changelog](https://github.com/geoserver/geoserver-cloud/releases/) for more information.

## Bugs

*GeoServer Cloud*'s issue tracking is at this [GitHub](https://github.com/geoserver/geoserver-cloud/issues) repository.

## Roadmap

Follow the development progress on these [GitHub Kanban boards](https://github.com/geoserver/geoserver-cloud/projects)

## Building

Requirements:

 * Java >= 21 JDK
 * [Maven](https://maven.apache.org/) >= `3.6.3`
 * A recent [Docker](https://docs.docker.com/engine/install/) version with the [Compose](https://docs.docker.com/compose/) plugin.

The simple `make` command from the project root directory will build, test, and install all the project artifacts, and build the GeoServer-Cloud Docker images. So for a full build just run:

```bash
make
```

To build without running tests, run

```bash
make install
```

and run tests with

```bash
make test
```

finally clean the build with

```bash
make clean
```

### Build the docker images

As mentioned above, a `make` with no arguments will build everything.

But to build only the docker images, run:

```bash
make build-image
```

This runs the `build-base-images`, `build-image-infrastructure`, and `build-image-geoserver` targets,
which you can also run individually during development depending on your needs. Usually,
you'd run `make build-image-geoserver` to speed up the process when made a change and want
to test the geoserver containers, without having to rebuild the base and infra images.

### Targeted builds

*GeoServer Cloud*-specific modules source code is under the `src/` directory.

When you already have the `2.23.0-CLOUD` GeoServer artifacts, you can choose to only build these projects, either by:


```bash
$ ./mvnw clean install -f src/
```

Or 

```bash
$ cd src/
$ ../mvnw clean install
```

### Note on custom upstream GeoServer version

*GeoServer Cloud* depends on a custom GeoServer branch, `gscloud/gs_version/integration`, which contains patches to upstream GeoServer that have not yet been integrated into the mainstream `main` branch.

Additionally, this branch changes the artifact versions (e.g. from `2.23-SNAPSHOT` to `2.23.0-CLOUD`), to avoid confusing maven if you also work with vanilla GeoServer, and to avoid your IDE downloading the latest `2.23-SNAPSHOT` artifacts from the OsGeo maven repository, overriding your local maven repository ones, and having confusing compilation errors that would require re-building the branch we need.

The `gscloud/gs_version/integration` branch is checked out as a submodule on the (camptocamp/geoserver-cloud-geoserver)[https://github.com/camptocamp/geoserver-cloud-geoserver] repository, which publishes the custom geoserver maven artifacts to the Github maven package registry.

The root pom adds this additional maven repository, so no further action is required for the geoserver-cloud build to use those dependencies.

## Development runs

The `./compose` folder contains docker-compose files intended only for **development**.

For instructions on running GeoServer Cloud in your environment, follow the [Quick Start](https://geoserver.org/geoserver-cloud/#quick-start) guide on the user guide.

### Run as non-root

First thing first, edit the `.env` file to set the `GS_USER` variable to the user and group ids
the applications should run as.

Usually the GID and UID of your user, such as:

```
echo `id -g`:`id -u`
1000:1000
```

Healthchecks use `curl` hitting the `http://localhost:8081/actuator/health` spring-boot actuator endpoint, which
also provides Kubernetes liveness and readiness probes at `/actuator/health/liveness` and `/actuator/health/readiness`
respectively.

The services run on the `8080` port, and are exposed using different host ports. The spring-boot-actuator is set up at port `8081`.

The `gateway-service` proxies requests from the `9090` local port:

### Choose your Catalog and Configuration back-end

You need to run `compose.yml` and pick one compose override file for a given GeoServer Catalog
and Configuration back-end.

#### DataDirectory Catalog back-end

The `datadir` spring boot profile enables the traditional "data directory" catalog back-end,
with all GeoServer containers sharing the same directory. On a k8s deployment you would need a
`ReadWriteMany` persistent volume.

GeoServer-Cloud can start from an empty data directory.

The `catalog-datadir.yml` docker compose override enables the `datadir` profile and
initializes a volume with the default GeoServer release data directory.

Run with:

```bash
$ alias dcd="docker compose -f compose.yml -f catalog-datadir.yml"
$ dcd up -d
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
$ alias dcp="docker compose -f compose.yml -f catalog-pgconfig.yml"
$ dcp up -d
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

Verify the services are running with `dcd ps` or `dcp ps` as appropriate.

```
$ curl "http://localhost:9090/geoserver/cloud/ows?request=getcapabilities&service={WMS,WFS,WCS,WPS}"
$ curl -u admin:geoserver "http://localhost:9090/geoserver/cloud/rest/workspaces.json"
```

Browse to [http://localhost:9090/geoserver/cloud/](http://localhost:9090/geoserver/cloud/)

> Note the `/geoserver/cloud` context path is set up in the `gateway-service`'s externalized
> configuration, and enforced through the `GEOSERVER_BASE_PATH` in `compose.yml`.
> You can change it to whatever you want. The default [config/gateway-service.yml](config/gateway-service.yml)
> configuration file does not set up a context path at all, and hence GeoServer will
> be available at the root URL.


