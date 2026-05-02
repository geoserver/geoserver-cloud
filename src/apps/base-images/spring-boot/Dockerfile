ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder

ARG JAR_FILE=target/gs-cloud-*-bin.jar

WORKDIR /builder

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM $REPOSITORY/gs-cloud-base-jre:$TAG

RUN apt update \
&& apt -y upgrade \
&& apt install -y --no-install-recommends \
curl \
&& apt clean \
&& apt purge -y  \
&& apt autoremove --purge -y \
&& rm -rf /var/cache/apt/* \
&& rm -rf /var/lib/apt/lists/*

COPY target/config/ /etc/geoserver/

RUN mkdir -p /opt/app/bin

WORKDIR /opt/app/bin

EXPOSE 8080
EXPOSE 8081

COPY --from=builder /builder/dependencies/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder /builder/snapshot-dependencies/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder /builder/spring-boot-loader/ ./

HEALTHCHECK \
--interval=10s \
--timeout=5s \
--start-period=30s \
--retries=5 \
CMD curl -f -s -o /dev/null localhost:8081/actuator/health || exit 1

# JVM AOT cache support: services that run an AOT training step during image build
# produce an app.aot cache file. At startup, -XX:AOTMode=auto tells the JVM to use
# the cache if present, or silently continue without it.
# Override at runtime with -e AOT_MODE=off to disable AOT for a specific container.
ENV AOT_MODE=auto
CMD exec java $JAVA_OPTS -XX:AOTMode=$AOT_MODE -XX:AOTCache=app.aot org.springframework.boot.loader.launch.JarLauncher
