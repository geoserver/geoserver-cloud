FROM adoptopenjdk/openjdk11:slim

LABEL maintainer="GeoServer PSC <geoserver-users@lists.sourceforge.net>"

# Add dockerize tool -------------------
RUN apt-get update && apt-get install -y \
    wget \
    sudo \
&& chmod u+s /usr/bin/sudo

# Installed sudo so the 'geo' user can run admin commands if you ever need to enter a running container.
# Beware if the host's /var/lib/docker directory (or the configured docker graph directory) is on a filesystem 
# mounted with nosuid, trying to run a sudo command inside the container it will fail with the following error:
# "sudo: effective uid is not 0, is /usr/bin/sudo on a file system with the 'nosuid' option set or an NFS file system without root privileges?".
# For example, my docker graph (/etc/docker/daemon.json) is `{"graph": "/data2/docker_files"}`, and `/data2` is a nosuid filesystem.
# If that happens, you can remount the host filesystem with `sudo mount -n -o remount,suid /data2` (replace /data2 by your mount point)

ENV DOCKERIZE_VERSION v0.6.1
RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

RUN groupadd -r -g 630 geo && \
    useradd -r -g geo -G sudo -u 630 geo&& \
    mkdir /opt/app && \
    chown geo:geo /opt/app && \
    echo "geo ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

USER geo

ENV JAVA_OPTS=