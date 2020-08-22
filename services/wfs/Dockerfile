FROM org.geoserver.cloud/gs-cloud-docker-base-java:latest

ARG JAR_FILE=dummy
ADD target/${JAR_FILE} /opt/app/wfs-service.jar

#add test data, remove once we get rid of it
ARG TESTDATA=target/test_data.tar.gz
ADD ${TESTDATA} /opt/app/data
RUN sudo chown -R geo:geo /opt/app/data

#set up an empty geoserver data directory
RUN mkdir /opt/app/data_directory && sudo chown -R geo:geo /opt/app/data_directory
ENV GEOSERVER_DATA_DIR=/opt/app/data_directory

CMD exec java $JAVA_OPTS -jar /opt/app/wfs-service.jar
