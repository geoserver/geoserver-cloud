FROM eclipse-temurin:21-jre

LABEL maintainer="GeoServer PSC <geoserver-users@lists.sourceforge.net>"

# default JVM parameters https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/envvars002.html
# to add JVM parameters use the JAVA_OPTS env variable preferrably
ENV DEFAULT_JAVA_TOOL_OPTIONS="\
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.text=ALL-UNNAMED \
--add-opens=java.desktop/java.awt.font=ALL-UNNAMED \
--add-opens=java.desktop/sun.awt.image=ALL-UNNAMED \
--add-opens=java.desktop/sun.java2d.pipe=ALL-UNNAMED \
--add-opens=java.naming/com.sun.jndi.ldap=ALL-UNNAMED \
-Djava.awt.headless=true \
-XX:MaxRAMPercentage=80 \
-XX:InitialRAMPercentage=30 \
-XX:+UseCompressedOops"

ENV JAVA_TOOL_OPTIONS="${DEFAULT_JAVA_TOOL_OPTIONS}"
ENV JAVA_OPTS=

# Install the system CA certificates for the JVM :wqnow that we're root
RUN USE_SYSTEM_CA_CERTS=true /__cacert_entrypoint.sh
