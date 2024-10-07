ARG TAG=latest
FROM geoservercloud/gs-cloud-base-jre:$TAG AS builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM geoservercloud/gs-cloud-base-spring-boot:$TAG

# WORKDIR already set to /opt/app/bin

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

HEALTHCHECK \
--interval=10s \
--timeout=5s \
--start-period=30s \
--retries=5 \
CMD curl -f -s -o /dev/null localhost:8080/actuator/health || exit 1

CMD exec env USER_ID="$(id -u)" USER_GID="$(id -g)" java $JAVA_OPTS org.springframework.boot.loader.JarLauncher
