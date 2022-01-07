#!/bin/bash

for service in gateway rest webui wms wfs wcs admin-server discovery config rabbitmq
do
  podman container stop -i $service
  podman container rm -i $service
done
