ARG TAG=latest
FROM geoservercloud/gs-cloud-base-jre:$TAG as builder
ARG JAR_FILE=target/gs-cloud-*-bin.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

##########
FROM geoservercloud/gs-cloud-base-spring-boot:$TAG

# init
RUN apt update \
&& apt -y upgrade \
&& apt install -y --no-install-recommends \
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

RUN mkdir -p /opt/app/data_directory /data/geowebcache \
&& chmod 0777 /opt/app/data_directory /data/geowebcache

VOLUME /opt/app/data_directory
VOLUME /data/geowebcache

WORKDIR /opt/app/bin

COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
