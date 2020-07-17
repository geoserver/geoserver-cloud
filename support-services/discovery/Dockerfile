FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/discovery-service.jar

CMD ["java", "-jar", "/opt/discovery-service.jar"]