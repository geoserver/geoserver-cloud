FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/restconfig-service.jar

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/restconfig-service.jar