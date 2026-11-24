ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/geoserver:$TAG

# The geoserver image has the application. This image adds the main class and the settings of the service.
COPY target/classes/ BOOT-INF/classes/

# LOADER_MAIN is the main class started by the launcher.
# The services run at the root context path, the geoserver image defaults to /geoserver.
ENV LOADER_MAIN=org.geoserver.cloud.wfs.app.WfsApplication \
    GEOSERVER_CONTEXT_PATH=""

# No AOT training here: the AOT cache of the geoserver image is valid for this image.
