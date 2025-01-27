#!/bin/bash

# read GSCLOUD_VERSION from ./.env
export $(cat ./.env)

podman pull docker.io/library/rabbitmq:4-management-alpine

for i in discovery config gateway rest webui wms wfs wcs gwc
do
  podman pull docker-daemon:geoservercloud/geoserver-cloud-$i:$GSCLOUD_VERSION
done
