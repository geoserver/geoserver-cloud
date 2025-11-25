## Running GeoServer Cloud

This documentation describes how to run GeoServer Cloud with podman without pods (traditional way) based on 

### Creating network

```bash
podman network create gs-cloud-network
```

### Creating volumes

Persistent storage for Rabbitmq

```bash
podman volume create rabbitmq_data
```

Creating a shared directory for the different GeoServer containers, e.g. `webui`, `wms`, etc.

```bash
podman volume create shared_data_directory
```

### Downloading images

In order to speed up the "starting" part of the documentation we are going to download the images as the first stop

```bash
podman pull docker.io/library/rabbitmq:4-management-alpine 
export GSCLOUD_VERSION=2.28.1.0

for service in discovery config gateway rest webui wms wfs wcs
do
  podman pull docker.io/geoservercloud/geoserver-cloud-$service:${GSCLOUD_VERSION}
done
```

### Creating primary containers

Following containers are required as the "base system".

Please start the containers in the described order:

#### Rabbitmq

```bash
podman run -d --name=rabbitmq --network gs-cloud-network -v rabbitmq_data:/var/lib/rabbitmq --restart always rabbitmq:4-management-alpine
```

#### Discovery

```bash
podman run -d --name=discovery --hostname=discovery \
  --network gs-cloud-network \
  -p 8761:8761 \
  --restart always \
  geoservercloud/geoserver-cloud-discovery:$GSCLOUD_VERSION
```

Accepted environment variables default values:

    -e SERVER_PORT=8761
    -e EUREKA_INSTANCE_HOSTNAME=discovery 


#### Config

```bash
podman run -d --name=config --hostname=config \
  --network gs-cloud-network \
  -e SPRING_PROFILES_ACTIVE=git \
  -e CONFIG_GIT_URI=https://github.com/geoserver/geoserver-cloud-config.git \
  -e SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL=v2.28.1.0 \
  -e CONFIG_GIT_BASEDIR=/opt/app/git_config \
  geoservercloud/geoserver-cloud-config:$GSCLOUD_VERSION
```

Accepted environment variables default values:

    -e EUREKA_SERVER_URL=http://discovery:8761/eureka
    -e SPRING_PROFILES_ACTIVE=git
    -e CONFIG_GIT_URI=https://github.com/geoserver/geoserver-cloud-config.git
    -e SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL=master
    -e CONFIG_GIT_BASEDIR=/opt/app/git_config
    -e CONFIG_NATIVE_PATH=/opt/app/config

#### Gateway

```bash
podman run -d --name=gateway \
  --network gs-cloud-network \
  -p 9090:8080 \
  geoservercloud/geoserver-cloud-gateway:$GSCLOUD_VERSION
```

Accepted environment variables default values:

    -e EUREKA_SERVER_URL=http://discovery:8761/eureka

### Creating service containers

Depending on your use case you can start any of the following containers.

Accepted environment variables default values:

    -e EUREKA_SERVER_URL=http://discovery:8761/eureka
    -e SPRING_PROFILES_ACTIVE=datadir|jdbcconfig
    -e GEOSERVER_DATA_DIR=/opt/app/data_directory

Accepted environment variables default values for webui:

    -e EUREKA_SERVER_URL=http://discovery:8761/eureka
    -e SPRING_PROFILES_ACTIVE=datadir|jdbcconfig
    -e GEOSERVER_DATA_DIR=/opt/app/data_directory
    -e WEBUI_IMPORTER_ENABLED=false|true

```bash
for service in webui rest wms wfs wcs gwc
do
  podman run -d --name=$service \
    --network gs-cloud-network \
    -e SPRING_PROFILES_ACTIVE=datadir \
    -v shared_data_directory:/opt/app/data_directory \
    geoservercloud/geoserver-cloud-$service:$GSCLOUD_VERSION
done
```


### Integration with systemd

For better integration with your linux distribution which has to be based on systemd.
Of course, the containers will be running for security reasons with a normal user account.

#### Preparing systemd

```bash
mkdir -p ~/.config/systemd/user/
```

#### Creating "base system" systemd files

```bash
for service in rabbitmq discovery config gateway
do
  podman generate systemd --new -n $service > ~/.config/systemd/user/container-$service.service
done
```

##### Removing running containers

```bash
podman rm -f rabbitmq discovery config gateway
```

#### Adjusting dependencies base system

```bash
for service in config gateway
do
  sed -i "/Wants=network-online.target/c\Wants=network-online.target container-discovery.service" ~/.config/systemd/user/container-$service.service
  sed -i "/After=network-online.target/c\After=network-online.target container-discovery.service" ~/.config/systemd/user/container-$service.service
done
```

##### Enabling and starting containers with systemd

```bash
systemctl --user enable --now container-rabbitmq container-discovery container-config container-gateway
```

#### Creating "service containers" systemd files

```bash
for service in rest webui wms wfs wcs gwc
do
  podman generate systemd --new -n $service > ~/.config/systemd/user/container-$service.service
  podman rm -f $service
  sed -i "/Wants=network-online.target/c\Wants=network-online.target container-config.service" ~/.config/systemd/user/container-$service.service
  sed -i "/After=network-online.target/c\After=network-online.target container-config.service" ~/.config/systemd/user/container-$service.service
  systemctl --user enable --now container-$service
done
```
