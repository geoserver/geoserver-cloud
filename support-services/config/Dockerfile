FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/app/config-service.jar

CMD exec java $JAVA_OPTS -jar /opt/app/config-service.jar
