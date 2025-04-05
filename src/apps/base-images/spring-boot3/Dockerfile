ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder

ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM $REPOSITORY/gs-cloud-base-jre:$TAG

COPY target/config/ /etc/geoserver/

RUN mkdir -p /opt/app/bin

WORKDIR /opt/app/bin

EXPOSE 8080
EXPOSE 8081

COPY --from=builder dependencies/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder snapshot-dependencies/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder spring-boot-loader/ ./

HEALTHCHECK \
--interval=10s \
--timeout=5s \
--start-period=30s \
--retries=5 \
CMD curl -f -s -o /dev/null localhost:8081/actuator/health || exit 1

CMD exec env USER_ID="$(id -u)" USER_GID="$(id -g)" java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher