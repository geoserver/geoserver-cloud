## Running GeoServer Cloud

This documentation describes how to run GeoServer Cloud with podman withouth pods (traditional way) based on 

### Creating network

```bash
podman network create gs-cloud-network
```

### Creating volumes

Persistent storage for Rabbitmq
```bash
podman volume create rabbitmq_data
```

Creating a shared directory for the different GeoServer containers, e.g. webui, wms, etc.
```bash
podman volume create shared_data_directory
```

### Downloading images

In order to speed up the "starting" part of the documentation we are going to download the images as the first stop

```bash
podman pull docker.io/library/rabbitmq:3.9-management 
podman pull docker.io/geoservercloud/geoserver-cloud-discovery:1.0-RC1 
podman pull docker.io/geoservercloud/geoserver-cloud-config:1.0-RC1 
podman pull docker.io/geoservercloud/geoserver-cloud-gateway:1.0-RC1 
podman pull docker.io/geoservercloud/geoserver-cloud-rest:1.0-RC1 
podman pull docker.io/geoservercloud/geoserver-cloud-webui:1.0-RC1 
podman pull docker.io/geoservercloud/geoserver-cloud-wms:1.0-RC1 
podman pull docker.io/geoservercloud/geoserver-cloud-wfs:1.0-RC1 
podman pull docker.io/geoservercloud/geoserver-cloud-wcs:1.0-RC1
```

### Creating primary containers

Following containers are required as the "base system".

Please start the containers in the described order:

#### Rabbitmq
```bash
podman run -d --name=rabbitmq --network gs-cloud-network -v rabbitmq_data:/var/lib/rabbitmq --restart always rabbitmq:3.9-management
```
#### Discovery
```bash
podman run -d --name=discovery --network gs-cloud-network -p 8761:8761 -e SERVER_PORT=8761 -e EUREKA_INSTANCE_HOSTNAME=discovery --restart always --healthcheck-command '/bin/sh -c wait-for -t 0 http://localhost:8761/actuator/health' --healthcheck-interval 1s --healthcheck-timeout 1s --healthcheck-retries 30 geoservercloud/geoserver-cloud-discovery:1.0-RC1
```
#### Config
```bash
podman run -d --name=config --network gs-cloud-network -e EUREKA_SERVER_URL=http://discovery:8761/eureka -e SPRING_PROFILES_ACTIVE=git -e CONFIG_GIT_URI=https://github.com/geoserver/geoserver-cloud-config.git -e spring.cloud.config.server.git.default-label=r1.0-RC1 -e CONFIG_GIT_BASEDIR=/opt/app/git_config -e CONFIG_NATIVE_PATH=/opt/app/config --healthcheck-command '/bin/sh -c wait-for -t 0 http://localhost:8080/actuator/health' --healthcheck-interval 1s --healthcheck-timeout 1s --healthcheck-retries 30 geoservercloud/geoserver-cloud-config:1.0-RC1
```
#### Gateway
```bash
podman run -d --name=gateway --network gs-cloud-network -p 9090:8080 -e SPRING_PROFILES_ACTIVE=datadir -e EUREKA_SERVER_URL=http://discovery:8761/eureka geoservercloud/geoserver-cloud-gateway:1.0-RC1
```

### Creating optional containers

Depending on your use case you can start any of the following containers.

#### Rest
```bash
podman run -d --name=rest --network gs-cloud-network -v shared_data_directory:/opt/app/data_directory -e SPRING_PROFILES_ACTIVE=datadir -e EUREKA_SERVER_URL=http://discovery:8761/eureka -e GEOSERVER_DATA_DIR=/opt/app/data_directory geoservercloud/geoserver-cloud-rest:1.0-RC1
```
#### Webui
```bash
podman run -d --name=webui --network gs-cloud-network -v shared_data_directory:/opt/app/data_directory -e SPRING_PROFILES_ACTIVE=datadir -e EUREKA_SERVER_URL=http://discovery:8761/eureka -e GEOSERVER_DATA_DIR=/opt/app/data_directory geoservercloud/geoserver-cloud-webui:1.0-RC1
```
#### WMS
```bash
podman run -d --name=wms --network gs-cloud-network -v shared_data_directory:/opt/app/data_directory -e SPRING_PROFILES_ACTIVE=datadir -e EUREKA_SERVER_URL=http://discovery:8761/eureka -e GEOSERVER_DATA_DIR=/opt/app/data_directory geoservercloud/geoserver-cloud-wms:1.0-RC1
```
#### WFS
```bash
podman run -d --name=wfs --network gs-cloud-network -v shared_data_directory:/opt/app/data_directory -e SPRING_PROFILES_ACTIVE=datadir -e EUREKA_SERVER_URL=http://discovery:8761/eureka -e GEOSERVER_DATA_DIR=/opt/app/data_directory geoservercloud/geoserver-cloud-wfs:1.0-RC1
```
#### WCS
```bash
podman run -d --name=wcs --network gs-cloud-network -v shared_data_directory:/opt/app/data_directory -e SPRING_PROFILES_ACTIVE=datadir -e EUREKA_SERVER_URL=http://discovery:8761/eureka -e GEOSERVER_DATA_DIR=/opt/app/data_directory geoservercloud/geoserver-cloud-wcs:1.0-RC1
```

### Integration with systemd

For better integration with your linux distribution which has to be based on systemd.
Of course, the containers will be running for security reasons with a normal user account.

#### Preparing systemd

```bash
mkdir -p ~/.config/systemd/user/
```

#### Creating "base system" systemd files

##### Rabbitmq
```bash
podman generate systemd --new -n rabbitmq > ~/.config/systemd/user/container-rabbitmq.service
```

##### Discovery
```bash
podman generate systemd --new -n discovery > ~/.config/systemd/user/container-discovery.service
```

##### Config
```bash
podman generate systemd --new -n config > ~/.config/systemd/user/container-config.service
```

##### Gateway
```bash
podman generate systemd --new -n gateway > ~/.config/systemd/user/container-gateway.service
```

##### Removing running containers
```bash
podman rm -f rabbitmq discovery config gateway
```

#### Adjusting dependencies base system

##### Config
```bash
sed -i "/Wants=network-online.target/c\Wants=network-online.target container-discovery.service" ~/.config/systemd/user/container-config.service
```
```bash
sed -i "/After=network-online.target/c\After=network-online.target container-discovery.service" ~/.config/systemd/user/container-config.service
```
##### Gateway
```bash
sed -i "/Wants=network-online.target/c\Wants=network-online.target container-discovery.service" ~/.config/systemd/user/container-gateway.service
```
```bash
sed -i "/After=network-online.target/c\After=network-online.target container-discovery.service" ~/.config/systemd/user/container-gateway.service
```

##### Enabling and starting containers with systemd
```bash
systemctl --user enable --now container-rabbitmq container-discovery container-config container-gateway
```

#### Creating "optional containers" systemd files

##### Rest
```bash
podman generate systemd --new -n rest > ~/.config/systemd/user/container-rest.service
```
```bash
podman rm -f rest
```
```bash
sed -i "/Wants=network-online.target/c\Wants=network-online.target container-config.service" ~/.config/systemd/user/container-config.service
```
```bash
sed -i "/After=network-online.target/c\After=network-online.target container-config.service" ~/.config/systemd/user/container-config.service
```
```bash
systemctl --user enable --now container-rest
```

##### Webui
```bash
podman generate systemd --new -n webui > ~/.config/systemd/user/container-webui.service
```
```bash
podman rm -f webui
```
```bash
sed -i "/Wants=network-online.target/c\Wants=network-online.target container-discovery.service" ~/.config/systemd/user/container-rabbitmq.service
```
```bash
sed -i "/After=network-online.target/c\After=network-online.target container-discovery.service" ~/.config/systemd/user/container-rabbitmq.service
```
```bash
systemctl --user enable --now container-webui
```

##### WMS
```bash
podman generate systemd --new -n wms > ~/.config/systemd/user/container-wms.service
```
```bash
podman rm -f wms
```
```bash
sed -i "/Wants=network-online.target/c\Wants=network-online.target container-config.service" ~/.config/systemd/user/container-wms.service
```
```bash
sed -i "/After=network-online.target/c\After=network-online.target container-config.service" ~/.config/systemd/user/container-wms.service
```
```bash
systemctl --user enable --now container-wms
```

##### WFS
```bash
podman generate systemd --new -n wfs > ~/.config/systemd/user/container-wfs.service
```
```bash
podman rm -f wfs
```
```bash
sed -i "/Wants=network-online.target/c\Wants=network-online.target container-config.service" ~/.config/systemd/user/container-wfs.service
```
```bash
sed -i "/After=network-online.target/c\After=network-online.target container-config.service" ~/.config/systemd/user/container-wfs.service
```
```bash
systemctl --user enable --now container-wfs
```

##### WCS
```bash
podman generate systemd --new -n wcs > ~/.config/systemd/user/container-wcs.service
```
```bash
podman rm -f wcs
```
```bash
sed -i "/Wants=network-online.target/c\Wants=network-online.target container-config.service" ~/.config/systemd/user/container-wcs.service
```
```bash
sed -i "/After=network-online.target/c\After=network-online.target container-config.service" ~/.config/systemd/user/container-wcs.service
```
```bash
systemctl --user enable --now container-wcs
```
