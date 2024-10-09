ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM eclipse-temurin:21-jre-noble AS builder

ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM $REPOSITORY/spring-boot-common:$TAG

# WORKDIR already set to /opt/app/bin

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

# Where jgit will try to create a .config directory
ENV XDG_CONFIG_HOME=/tmp

