ARG TAG
FROM org.geoserver.cloud/gs-cloud-docker-base-java:${TAG}

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/app/config-service.jar

# set working dir to /opt/app so it finds the /opt/app/config directory, if it's present
WORKDIR /opt/app
CMD exec java $JAVA_OPTS -jar ./config-service.jar
