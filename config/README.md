# Centralized microservices configuration 

This directory contains the spring configuration properties or yaml files for all
GeoServer microservices, as used by the config-service, following 
[spring-cloud-config](https://cloud.spring.io/spring-cloud-config/reference/html/) guidelines.

The default docker composition at the project's root directory sets
this directory up as a bound volume to the `config-service` container(s).