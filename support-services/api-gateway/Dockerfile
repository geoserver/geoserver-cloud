FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/gateway-service.jar

CMD exec java $JAVA_OPTS -jar /opt/gateway-service.jar