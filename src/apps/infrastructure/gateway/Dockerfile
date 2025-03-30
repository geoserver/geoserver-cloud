ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM $REPOSITORY/gs-cloud-base-spring-boot3:$TAG
# WORKDIR already set to /opt/app/bin

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
#see https://github.com/moby/moby/issues/37965
RUN true
COPY --from=builder application/ ./

# Generate CDS archive for faster startup
# Execute the CDS training run
RUN java -XX:ArchiveClassesAtExit=application.jsa \
-Dspring.context.exit=onRefreshed \
-Dspring.profiles.active=standalone \
-Dserver.port=0 -Dmanagement.server.port=0 \
org.springframework.boot.loader.launch.JarLauncher

RUN rm -rf /tmp/*

# Enable CDS for faster startup
ENV JAVA_TOOL_OPTIONS="${DEFAULT_JAVA_TOOL_OPTIONS} -XX:SharedArchiveFile=application.jsa"
