# Centralized microservices configuration 

This directory contains the spring configuration properties or yaml files for all
GeoServer microservices, as used by the config-service, following 
[spring-cloud-config](https://cloud.spring.io/spring-cloud-config/reference/html/) guidelines.

The contents of this directory are copied to the `config-service`'s `.jar` file at build time,
so they're accessible as classpath resources. Nonetheless, it's advisable to not use classpath
configuration, and hence the default docker composition at the project's root directory sets
this directory up as a bound volume to the `config-service` container(s).