FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy
ADD target/${JAR_FILE} /opt/wfs-service.jar

#data directory, remove once we get rid of it
VOLUME /tmp/data_directory
ARG DATADIR=target/release.tar.gz
ADD ${DATADIR} /tmp/data_directory
ENV GEOSERVER_DATA_DIR=/tmp/data_directory

CMD exec java $JAVA_OPTS -jar /opt/wfs-service.jar