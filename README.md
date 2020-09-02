# Cloud Native GeoServer

*Cloud Native GeoServer* is GeoServer ready to use in the cloud through dockerized microservices.

This project is an opinionated approach over how to split [GeoServer](http://geoserver.org/)'s services and API offerings into
a microservices based architecture.

## Services Architecture

* Discovery
* Config
* Event bus
* Gateway
* config
* OWS services
* REST API service
* Web-UI service


Only a curated list of the [vast amount](http://geoserver.org/release/stable/) of GeoServer extensions will be supported, as they are verified to work with this project's architecture, or adapted to do so.

## License

*CN GeoServer* licensed under the [GPL](LICENSE.txt).

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

## Bugs

*CN GeoServer*'s issue tracking is at this [GitHub](https://github.com/camptocamp/geoserver-microservices/issues) repository.

## Roadmap

Follow the development progress on these [GitHub Kanban boards](https://github.com/camptocamp/geoserver-microservices/projects)


## Contributing

Please read [the contribution guidelines](CONTRIBUTING.md) before contributing pull requests to the CN GeoServer project.
