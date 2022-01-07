#!/bin/bash

# read GSCLOUD_VERSION from ./env
export $(cat ./env)

podman pull docker.io/library/rabbitmq:3.9-management

for i in discovery config gateway rest webui wms wfs wcs admin-server
do
  podman pull docker-daemon:geoservercloud/geoserver-cloud-$i:$GSCLOUD_VERSION
done
