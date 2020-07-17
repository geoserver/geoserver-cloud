FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/config-service.jar

CMD ["java", "-jar", "/opt/config-service.jar"]