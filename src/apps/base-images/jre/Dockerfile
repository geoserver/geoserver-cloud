FROM eclipse-temurin:21-jre

LABEL maintainer="GeoServer PSC <geoserver-users@lists.sourceforge.net>"

ENV JAVA_TOOL_OPTIONS=
ENV JAVA_OPTS=

# Install the system CA certificates for the JVM :wqnow that we're root
RUN USE_SYSTEM_CA_CERTS=true /__cacert_entrypoint.sh
