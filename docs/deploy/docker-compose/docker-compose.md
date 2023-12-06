# GeoServer Cloud docker-compose

Cloud Native GeoServer can be run as a docker composition.

Make sure you have `docker` and `docker-compose` installed:

 * [Docker](https://docs.docker.com/engine/install/) version >= `19.03.3`
 * [docker-compose](https://docs.docker.com/compose/) version >= `1.26.2`

Here are two docker-compose based deployment examples to try out:

  * A [Shared data-directory](stable/shared_datadir/docker-compose.yml) `docker-compose.yml` file;
  * and a PostgreSQL [jdbcconfig](jdbcconfig/docker-compose.yml) `docker-compose.yml`.

The former will mount a docker volume on all containers for them to share the same "data directory",
while the later will use a PostgreSQL database to store the catalog and other resource configuration
objects instead of the file system.

Open a terminal and enter the directory where you just downloaded that file, 
and run `docker-compose pull` to fetch the docker images from 
[Dockerhub](https://hub.docker.com/u/geoservercloud/):

> Note in order to run the containers as a non root user, all service definitions specify a `user: 1000:1000`,
> which you should change to the appropriate user id and group id especially if using bind volumes.

```bash
$ docker-compose pull
Pulling rabbitmq  ... done
Pulling database  ... done
Pulling discovery ... done
Pulling config    ... done
Pulling gateway   ... done
Pulling wfs       ... done
Pulling wms       ... done
Pulling wcs       ... done
Pulling rest      ... done
Pulling webui     ... done
```

Then start the services with this command:

```bash
$ docker-compose --compatibility up -d
```

> Note the `--compatibility` argument is required when using a v3 docker-compose file, 
> in order to enable resource the CPU and Memory limits configured in the file for each service.

Wait for the services to start up, check with `docker-compose ps` until they're healthy.

Then browse to:

- [http://localhost:8761](http://localhost:8761/) to access the Discovery service registry page.
- [http://localhost:9090](http://localhost:9090/) for the GeoServer UI. The GateWay service 
  will take care of proxying requests to the appropriate microservice.

From a usability point of view, there should be no appreciable differences with a traditional GeoServer deployment.

What's going on inside the local-machine cluster you've just run, is that each GeoServer business
capability is running as a separate process in a separate docker container, and they're all in synch
in a loosely coupled way using a message driven event-bus.

Experiment dynamic service scaling and registration with
`docker-compose scale <service>=<instances>`, for example:

```bash
docker-compose --compatibility scale wfs=3 wcs=0
Starting gscloud_wfs_1 ... done
Creating gscloud_wfs_2 ... done
Creating gscloud_wfs_3 ... done
Stopping and removing gscloud_wcs_1 ... done
```
