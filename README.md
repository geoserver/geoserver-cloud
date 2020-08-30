# GeoServer Microservices

This project splits GeoServer components into microservices.

OWS microservices:

- WFS
- WMS
- WCS

Configuration:

- REST API

others: TBD


# Common configuration properties
The following configuration properties apply to all *GeoServer* microservices (i.e. not edge services):

```
geoserver.security.enabled=true #flag to turn off geoserver security auto-configuration
geoserver.proxy-urls.enabled=true #flag to turn off proxyfing respose URL's based on gateway's provided HTTP request headers (X-Forwarded-*)
geoserver.web.resource-browser.enabed=true
geoserver.servlet.enabled=true #flag to turn off auto-configuration of geoserver servlet context
geoserver.servlet.filter.session-debug.enabled=true #flag to disable the session debug servlet filter
geoserver.servlet.filter.flush-safe.enabled=true #flag to disable the flush-safe servlet filter

geoserver.jdbcconfig.enabled=true
geoserver.jdbcconfig.web.enabled=true
geoserver.jdbcconfig.initdb=false
geoserver.jdbcconfig.datasource.jdbc-url=jdbc\:postgresql\://database\:5432/geoserver_config
geoserver.jdbcconfig.datasource.username=sa
geoserver.jdbcconfig.datasource.password=
geoserver.jdbcconfig.datasource.driverClassname=org.postgresql.Driver
```
