ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

WORKDIR /builder

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM $REPOSITORY/gs-cloud-base-spring-boot:$TAG
# WORKDIR already set to /opt/app/bin

COPY --from=builder /builder/dependencies/ ./
COPY --from=builder /builder/snapshot-dependencies/ ./
COPY --from=builder /builder/spring-boot-loader/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder /builder/application/ ./

# AOT training run for faster startup (each arch builds natively, no QEMU)
RUN java -XX:AOTCacheOutput=app.aot \
-Dspring.context.exit=onRefresh \
-Dspring.profiles.active=standalone \
org.springframework.boot.loader.launch.JarLauncher; \
rm -rf /tmp/*
