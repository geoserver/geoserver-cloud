ARG REPOSITORY=geoservercloud
ARG TAG=latest
FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM $REPOSITORY/gs-cloud-base-spring-boot:$TAG
ENV JAVA_TOOL_OPTIONS="${DEFAULT_JAVA_TOOL_OPTIONS} \
-Duser.projections.file=/etc/geoserver/user_projections/epsg.properties"

# Set HOME to a generic home directory that will be writable by any user
# Since deployers will assign their own user/group IDs that may not have real home directories,
# this ensures DuckDB extensions can be installed regardless of which non-root user runs the container
ENV HOME=/opt/app/home

# Create a proper HOME directory with necessary subdirectories 
# Sticky bit (1777) ensures only owners can delete their own files for better security
RUN mkdir -p ${HOME}/.duckdb && chmod 1777 ${HOME} && chmod 777 ${HOME}/.duckdb

# init
RUN apt update \
&& apt -y upgrade \
&& apt install -y --no-install-recommends \
fonts-dejavu \
fonts-dejavu-extra \
fonts-deva \
fonts-font-awesome \
fonts-freefont-ttf \
fonts-material-design-icons-iconfont \
fonts-materialdesignicons-webfont \
fonts-roboto \
&& apt clean \
&& apt purge -y  \
&& apt autoremove --purge -y \
&& rm -rf /var/cache/apt/* \
&& rm -rf /var/lib/apt/lists/*

#COPY --from=builder /tmp/YourKit-JavaProfiler-2023.9 /usr/local/YourKit-JavaProfiler-2023.9
RUN mkdir -p /opt/app/data_directory /data/geowebcache \
&& chmod 0777 /opt/app/data_directory /data/geowebcache

WORKDIR /opt/app/bin
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder application/ ./

# -----------------------------------------------------
# Pre-install DuckDB extensions in the base image to create a shared layer for all GeoServer microservices
# This ensures that all derived container images will have the necessary DuckDB extensions
# already installed and available
RUN java -cp `find /opt/app/bin/BOOT-INF/lib/duckdb_jdbc*.jar`:/opt/app/bin/BOOT-INF/classes \
    org.geoserver.cloud.InstallDuckDBExtensions
# -----------------------------------------------------

