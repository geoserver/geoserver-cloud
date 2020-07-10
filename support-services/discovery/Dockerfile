FROM geoserver-microservices/java-base-image:latest

ARG JAR_FILE=dummy

ADD target/${JAR_FILE} /opt/service.jar

CMD ["java", "-jar", "/opt/service.jar"]