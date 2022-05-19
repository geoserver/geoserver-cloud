ARG TAG=latest
FROM geoservercloud/gs-cloud-base-jre:$TAG as builder

ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM geoservercloud/gs-cloud-base-geoserver-image:$TAG

# WORKDIR already set to /opt/app/bin

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./
