ARG REPOSITORY=geoservercloud
ARG TAG=latest

FROM $REPOSITORY/gs-cloud-base-jre:$TAG AS builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

RUN apt update && apt install -y --no-install-recommends unzip

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

#RUN wget -q https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2023.9-docker.zip -P /tmp/ && \
#  unzip /tmp/YourKit-JavaProfiler-2023.9-docker.zip -d /tmp && \
#  rm /tmp/YourKit-JavaProfiler-2023.9-docker.zip

##########
FROM $REPOSITORY/gs-cloud-base-spring-boot:$TAG

ENV JAVA_TOOL_OPTIONS="${DEFAULT_JAVA_TOOL_OPTIONS} \
-Duser.projections.file=/etc/geoserver/user_projections/epsg.properties"

# init
RUN apt update \
&& apt -y upgrade \
&& apt install -y --no-install-recommends \
fonts-dejavu \
fonts-dejavu-extra \
fonts-deva \
fonts-font-awesome \
fonts-freefont-ttf \
fonts-material-design-icons-iconfont \
fonts-materialdesignicons-webfont \
fonts-roboto \
&& apt clean \
&& apt purge -y  \
&& apt autoremove --purge -y \
&& rm -rf /var/cache/apt/* \
&& rm -rf /var/lib/apt/lists/*

#COPY --from=builder /tmp/YourKit-JavaProfiler-2023.9 /usr/local/YourKit-JavaProfiler-2023.9

RUN mkdir -p /opt/app/data_directory /data/geowebcache \
&& chmod 0777 /opt/app/data_directory /data/geowebcache

WORKDIR /opt/app/bin

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./

