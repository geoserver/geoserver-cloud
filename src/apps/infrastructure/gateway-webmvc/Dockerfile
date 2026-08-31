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
# WORKDIR already set to /opt/app/bin

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder /builder/extracted/application/ ./

# AOT training run for faster startup (each arch builds natively, no QEMU)
RUN java -XX:AOTCacheOutput=app.aot \
-Dspring.context.exit=onRefresh \
-Dspring.profiles.active=standalone \
org.springframework.boot.loader.launch.PropertiesLauncher; \
rm -rf /tmp/*
