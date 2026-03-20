# GeoServer Cloud deployment with docker compose

GeoServer Cloud can be run in a docker composition.

These instructions assume you have Docker Engine, Docker CLI, and the Compose Plugin installed.

> GeoServer Cloud can run with various GeoServer catalog and configuration storage backends. For scalability, we recommend using our PostgreSQL backend, `pgconfig`.

Here are three `docker compose` based deployment examples to try out:

  * Our preferred option, the [pgconfig](stable/pgconfig/compose.yml) Catalog back-end, specifically developed for GeoServer Cloud with scalability in mind, storing all Catalog and GeoServer configuration in a PostgreSQL database.
  * A shared [data-directory](stable/datadir/compose.yml) option, using a mounted volume to share a traditional GeoServer data directory across all services.

Open a terminal and enter the directory where you just downloaded that file, and run `docker compose pull` to fetch the docker images from Dockerhub:

> Note in order to run the containers as a non root user, all service definitions specify a `user: 1000:1000`,
> which you should change to the appropriate user id and group id especially if using bind volumes.

```bash
$ docker compose pull
Pulling rabbitmq  ... done
Pulling database  ... done
Pulling consul    ... done
Pulling config    ... done
...
```

Then start the services:

```bash
$ docker compose up -d
```

Wait for the services to start. 
Check status with `docker compose ps` until they are healthy.

Access the system:

- [http://localhost:8500](http://localhost:8500/) for the Consul UI.
- [http://localhost:9090](http://localhost:9090/) for the GeoServer UI. 

The Gateway service proxies requests to the appropriate microservice.

Containers synchronize using a message-driven event bus.

In this Docker Compose deployment, the following components are used:
- **Consul**: Provides service discovery and health monitoring
- **Config Server**: Centralizes configuration management
- **RabbitMQ**: Enables event bus communication

Experiment with dynamic service scaling using `docker compose scale <service>=<instances>`.

> **Note for Kubernetes Deployments**: For Kubernetes environments, use the `standalone` Spring profile. Leverage Kubernetes Services for service discovery and load balancing instead of Consul. Kubernetes provides native service discovery and load balancing.
