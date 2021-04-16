FROM bellsoft/liberica-openjdk-alpine-musl:11 as builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

# see https://github.com/Eficode/wait-for
RUN wget -q https://raw.githubusercontent.com/eficode/wait-for/master/wait-for -O /usr/local/bin/wait-for \
    && chmod +x /usr/local/bin/wait-for

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM bellsoft/liberica-openjdk-alpine-musl:11

LABEL maintainer="GeoServer PSC <geoserver-users@lists.sourceforge.net>"

COPY --from=builder /usr/local/bin/wait-for /usr/local/bin/wait-for

RUN mkdir -p /opt/app \ 
    && addgroup -S -g 630 geo \
    && adduser -s /bin/sh -h /opt/app/bin -D -u 630 -G geo geo \
    && chown geo:geo /opt/app

WORKDIR /opt/app/bin
ENV JAVA_OPTS=
EXPOSE 8080
HEALTHCHECK CMD wait-for -t 0 http://localhost:8080/actuator/health || exit 1
#RUN apk add fontconfig ttf-dejavu

USER geo:geo
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

#ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
CMD exec wait-for -t 120 http://config:8080/wfs-service/default -- java $JAVA_OPTS org.springframework.boot.loader.JarLauncher
