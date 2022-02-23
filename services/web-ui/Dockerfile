FROM eclipse-temurin:17-jre as builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

RUN apt update && \ 
apt install -y fonts-deva \
fonts-font-awesome \
fonts-freefont-ttf \
fonts-material-design-icons-iconfont \
fonts-materialdesignicons-webfont \
fonts-roboto

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM eclipse-temurin:17-jre

LABEL maintainer="GeoServer PSC <geoserver-users@lists.sourceforge.net>"

COPY --from=builder /usr/share/fonts/truetype/* /usr/share/fonts/truetype/

# REVISIT: ideally webui shouldn't even have access to the geowebcache
# directory, but since it needs to run a lot of gwc and gwc-gs integration
# services, it might still need it.
RUN mkdir -p /opt/app/bin /opt/app/data_directory /data/geowebcache
RUN chmod 0777 /opt/app/data_directory /data/geowebcache

VOLUME /opt/app/data_directory
VOLUME /data/geowebcache

WORKDIR /opt/app/bin
ENV JAVA_TOOL_OPTS=\
--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.text=ALL-UNNAMED \
--add-opens=java.desktop/java.awt.font=ALL-UNNAMED \
--add-opens=java.desktop/sun.awt.image=ALL-UNNAMED \
--add-opens=java.naming/com.sun.jndi.ldap=ALL-UNNAMED
ENV JAVA_OPTS=
EXPOSE 8080
EXPOSE 8081

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

HEALTHCHECK \
--interval=10s \
--timeout=5s \
--start-period=30s \
--retries=5 \
CMD curl -f -s -o /dev/null localhost:8081/actuator/health || exit 1

CMD exec env USER_ID="$(id -u)" USER_GID="$(id -g)" java $JAVA_OPTS $JAVA_TOOL_OPTS org.springframework.boot.loader.JarLauncher
