ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM $REPOSITORY/gs-cloud-base-spring-boot:$TAG

# WORKDIR already set to /opt/app/bin

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder application/ ./

# Either 'git' or 'native'.
ENV SPRING_PROFILES_ACTIVE=native,standalone
# 'native' profile config, use the default config embedded in the Docker image.
# Feel free to override with a mounted volume
ENV CONFIG_NATIVE_PATH=/etc/geoserver

# Config for the 'git' profile
# Uses the default sample git repository to download the services configuration from
ENV CONFIG_GIT_URI: https://github.com/geoserver/geoserver-cloud-config.git
# Override to get the config from the appropriate release TAG or branch
ENV SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL: master
# where to store the cloned repository, if unset, it'll use /tmp/config-repo-<randomid>
ENV CONFIG_GIT_BASEDIR: /tmp/git_config
# avoid stack trace due to jgit not being able of creating a .config dir at $HOME
ENV XDG_CONFIG_HOME: /tmp

# Execute the CDS training run
RUN java -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefreshed org.springframework.boot.loader.JarLauncher
RUN rm -rf /tmp/*

ENV JAVA_TOOL_OPTIONS="${DEFAULT_JAVA_TOOL_OPTIONS} -XX:SharedArchiveFile=application.jsa"
