ARG REPOSITORY=geoservercloud
ARG JRE_BASE_IMAGE_NAME=jre:21-tem
ARG TAG=latest

# Build with java 11
FROM eclipse-temurin:11-jdk as builder

ENV GDAL_VERSION 3.8.4
ENV GDAL_PATH /usr/share/gdal
ENV GDAL_DATA $GDAL_PATH/3.8.4
ENV PATH $GDAL_PATH:$PATH
ENV LD_LIBRARY_PATH $LD_LIBRARY_PATH:/usr/lib/jni:/usr/share/java:/usr/local/lib/:/usr/lib/

#
# GDAL INSTALLATION
#
# Manual compilation - https://stackoverflow.com/questions/76913667/unable-to-locate-package-libgdal-java
RUN apt update && apt-get install -y wget build-essential libuv1-dev g++ libstdc++6 make linux-headers-generic \
    libssl-dev swig zlib1g-dev proj-bin proj-data libproj-dev sqlite3 libsqlite3-dev \ 
    libtiff-dev libcurl4-openssl-dev cmake libcrypto++-dev ant libeccodes-dev libnetcdf-dev
# Install GDAL
RUN wget https://download.osgeo.org/gdal/${GDAL_VERSION}/gdal-${GDAL_VERSION}.tar.gz -O /tmp/gdal.tar.gz && \
    tar xzf /tmp/gdal.tar.gz -C /tmp && \
    cd /tmp/gdal-${GDAL_VERSION} && \
    sed -i 's/source="7"/source="8"/' /tmp/gdal-${GDAL_VERSION}/swig/java/build.xml && \
    sed -i 's/target="7"/target="8"/' /tmp/gdal-${GDAL_VERSION}/swig/java/build.xml
RUN cd /tmp/gdal-${GDAL_VERSION} && mkdir build && cd build && cmake .. && cmake --build . && cmake --build . --target install

RUN mkdir /tmp/gdal-java
RUN cp /tmp/gdal-3.8.4/build/swig/java/gdal.jar /tmp/gdal-java/gdal.jar
RUN cp /tmp/gdal-3.8.4/build/swig/java/libgdalalljni.so /tmp/gdal-java/libgdalalljni.so

##########
FROM $REPOSITORY/$JRE_BASE_IMAGE_NAME

ENV GDAL_PATH /usr/share/gdal
ENV GDAL_DATA $GDAL_PATH
ENV LD_LIBRARY_PATH $LD_LIBRARY_PATH:$GDAL_PATH/lib:/usr/local/lib/:/usr/lib/

# see https://docs.docker.com/build/cache/optimize/#use-cache-mounts
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
  --mount=type=cache,target=/var/lib/apt,sharing=locked \
  apt update && apt-get --no-install-recommends install -y gdal-bin \
  && mkdir $GDAL_PATH/lib

COPY --from=builder /tmp/gdal-java/* $GDAL_PATH/lib/
