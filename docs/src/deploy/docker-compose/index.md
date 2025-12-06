# GeoServer Cloud deployment with docker compose

GeoServer Cloud can be run in a docker composition.

These instructions assume you have Docker Engine, Docker CLI, and the [Compose Plugin](https://docs.docker.com/compose/install/linux/) installed.

> GeoServer Cloud can run with various GeoServer catalog and configuration storage backends. For scalability, we recommend using our PostgreSQL backend, `pgconfig`.

Here are three `docker compose` based deployment examples to try out:

  * Our preferred option, the [pgconfig](stable/pgconfig/compose.yml) Catalog back-end, specifically developed for GeoServer Cloud with scalability in mind, storing all Catalog and GeoServer configuration in a PostgreSQL database.
  * A shared [data-directory](stable/datadir/compose.yml) option, using a mounted volume to share a traditional GeoServer data directory across all services.

Open a terminal and enter the directory where you just downloaded that file, and run `docker compose pull` to fetch the docker images from [Dockerhub](https://hub.docker.com/u/geoservercloud/):

> Note in order to run the containers as a non root user, all service definitions specify a `user: 1000:1000`,
> which you should change to the appropriate user id and group id especially if using bind volumes.

```bash
$ docker compose pull
Pulling rabbitmq  ... done
Pulling database  ... done
Pulling discovery ... done
Pulling config    ... done
Pulling gateway   ... done
Pulling wfs       ... done
Pulling wms       ... done
Pulling wcs       ... done
Pulling rest      ... done
Pulling gwc       ... done
Pulling webui     ... done
```

Then start the services with this command:

```bash
$ docker compose up -d
```

Wait for the services to start up, check with `docker compose ps` until they're healthy.

Then browse to:

- [http://localhost:8761](http://localhost:8761/) to access the Discovery service registry page.
- [http://localhost:9090](http://localhost:9090/) for the GeoServer UI. The GateWay service 
  will take care of proxying requests to the appropriate microservice.

From a usability point of view, there should be no appreciable differences with a traditional GeoServer deployment.

What's going on inside the local-machine cluster you've just run is that each GeoServer business
capability is running as a separate process in a separate docker container, and they're all in sync
in a loosely coupled way using a message-driven event-bus.

In this Docker Compose deployment, the following Spring Cloud components are used:
- **Eureka Discovery Service**: Provides service discovery and client-side load balancing
- **Config Server**: Centralizes configuration management
- **RabbitMQ**: Enables event bus communication between services

Experiment with dynamic service scaling and registration with
`docker compose scale <service>=<instances>`, for example:

```bash
docker compose scale wfs=3 wcs=0
Starting gscloud_wfs_1 ... done
Creating gscloud_wfs_2 ... done
Creating gscloud_wfs_3 ... done
Stopping and removing gscloud_wcs_1 ... done
```

> **Note for Kubernetes Deployments**: For Kubernetes environments, it's recommended to use the `standalone` Spring profile instead of relying on Eureka and Config Server. Kubernetes provides native service discovery and load balancing through Kubernetes Services, and configuration can be managed through ConfigMaps or Secrets. This approach aligns better with Kubernetes architecture and provides more efficient scaling.
