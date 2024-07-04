#!/bin/bash

for service in gateway rest webui wms wfs wcs gwc discovery config rabbitmq
do
  podman container stop -i $service
  podman container rm -i $service
done
