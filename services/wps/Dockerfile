FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/wps-service.jar

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/wps-service.jar