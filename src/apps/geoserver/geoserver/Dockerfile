ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder

ARG JAR_FILE=target/gs-cloud-*-bin.jar

WORKDIR /builder

COPY ${JAR_FILE} application.jar

# --layers takes one comma-separated list, and the destination must be an empty directory of its own
RUN java -Djarmode=tools -jar application.jar extract --launcher --destination extracted \
 --layers dependencies,spring-boot-loader,snapshot-dependencies,application

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
fonts-noto-core \
&& apt clean \
&& apt purge -y  \
&& apt autoremove --purge -y \
&& rm -rf /var/cache/apt/* \
&& rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/app/data_directory /data/geowebcache \
&& chmod 0777 /opt/app/data_directory /data/geowebcache

# Mount point for user-provided fonts, registered with fontconfig, which makes the
# JVM pick them up and list them in the GeoServer web UI's available fonts without
# any startup script. Fontconfig scans the directory lazily at runtime; its per-user
# cache goes to $HOME/.cache, writable by any runtime uid.
RUN mkdir -p /opt/additional_fonts && chmod 1777 /opt/additional_fonts
RUN cat > /etc/fonts/conf.d/05-additional-fonts.conf <<'EOF'
<?xml version="1.0"?>
<!DOCTYPE fontconfig SYSTEM "urn:fontconfig:fonts.dtd">
<fontconfig>
  <dir>/opt/additional_fonts</dir>
</fontconfig>
EOF

WORKDIR /opt/app/bin

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder /builder/extracted/application/ ./

# -----------------------------------------------------
# Pre-install DuckDB extensions at build time. This avoids installing them at container
# startup, which would fail when the container runs as an arbitrary non-root user.
RUN java -cp `find /opt/app/bin/BOOT-INF/lib/duckdb_jdbc*.jar`:/opt/app/bin/BOOT-INF/classes \
    org.geoserver.cloud.InstallDuckDBExtensions
# -----------------------------------------------------

# AOT training run for faster startup (each arch builds natively, no QEMU)
RUN export GEOSERVER_DATA_DIR=/tmp/datadir && \
 mkdir $GEOSERVER_DATA_DIR && \
 java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh \
  -Dgeoserver.acl.client.startupCheck=false \
  -Dgeoserver.bus.enabled=true \
  -Dspring.profiles.active=standalone,datadir,acl \
  org.springframework.boot.loader.launch.PropertiesLauncher && \
 rm -rf /tmp/*

# Standalone monolith defaults: traditional /geoserver context path.
# The service images and cloud deployments override these.
ENV GEOSERVER_CONTEXT_PATH=/geoserver
