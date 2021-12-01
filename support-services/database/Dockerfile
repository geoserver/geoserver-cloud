FROM postgres:13-alpine
LABEL maintainer="GeoServer PSC <geoserver-users@lists.sourceforge.net>"

VOLUME [ "/var/lib/postgresql/data" ]

ADD ./postgresql_initdb /docker-entrypoint-initdb.d

COPY --chown=postgres ./postgresql_initdb/* /docker-entrypoint-initdb.d/

HEALTHCHECK --interval=10s --timeout=1s --retries=5 CMD pg_isready -d $POSTGRES_DB -U $POSTGRES_USER
