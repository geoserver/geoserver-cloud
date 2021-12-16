FROM adoptopenjdk:11-jre-openj9 as builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM adoptopenjdk:11-jre-openj9

LABEL maintainer="GeoServer PSC <geoserver-users@lists.sourceforge.net>"

RUN mkdir -p /opt/app \ 
    && groupadd --system -g 630 geo \
    && useradd -u 630 -g geo --home-dir /opt/app/bin geo \
    && chown geo:geo /opt/app

WORKDIR /opt/app/bin
ENV JAVA_OPTS=
EXPOSE 8080

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

HEALTHCHECK \
--interval=10s \
--timeout=5s \
--start-period=30s \
--retries=5 \
CMD java -Xms4M -Xmx16M -jar BOOT-INF/lib/gs-cloud-docker-healthcheck-*-bin.jar || exit 1

CMD exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher
