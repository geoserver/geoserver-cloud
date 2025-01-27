#!/bin/bash

podman network create gscloud 2>/dev/null

podman volume create rabbitmq_data 2>/dev/null

# read GSCLOUD_VERSION from ./.env
export $(cat ./.env)

STD_OPTS="-d --network gscloud"

# Note rabbitmq and discovery are the only containers addressed
# by hostname inside the cluster, so we're adding the --hostname
# parameter to `podman run` on them. All other container urls are
# resolved by discovery.

echo Starting rabbitmq...
podman run $STD_OPTS --name=rabbitmq --hostname=rabbitmq \
  -v rabbitmq_data:/var/lib/rabbitmq \
  --restart always \
  rabbitmq:4-management-alpine

echo Starting discovery:$GSCLOUD_VERSION...
podman run $STD_OPTS --name=discovery --hostname=discovery \
  -p 8761:8761 \
  --restart always \
  geoservercloud/geoserver-cloud-discovery:$GSCLOUD_VERSION

echo Starting config:$GSCLOUD_VERSION...
podman run $STD_OPTS --name=config \
  --restart always \
  -e SPRING_PROFILES_ACTIVE=native \
  -e CONFIG_NATIVE_PATH=/etc/geoserver \
  geoservercloud/geoserver-cloud-config:$GSCLOUD_VERSION

echo Starting gateway:$GSCLOUD_VERSION...
podman run $STD_OPTS --name=gateway \
  -p 9090:8080 \
  geoservercloud/geoserver-cloud-gateway:$GSCLOUD_VERSION

mkdir -p datadir
#for i in webui wms wfs wcs rest
for i in webui wms wfs wcs rest gwc
do
  echo Starting $i:$GSCLOUD_VERSION...
  podman run $STD_OPTS --name=$i \
    -e SPRING_PROFILES_ACTIVE=datadir \
    -v ./datadir:/opt/app/data_directory:z \
    geoservercloud/geoserver-cloud-$i:$GSCLOUD_VERSION
done
