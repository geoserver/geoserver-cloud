# GeoServer Cloud Spring Boot 3 Starter

Spring Boot 3 starter module for GeoServer Cloud applications.

This starter provides common configurations and auto-configurations for GeoServer Cloud applications running on Spring Boot 3.x.

## Features

- Application startup logging
- Service ID filter configuration for web applications
- Support for standard GeoServer Cloud bootstrap profiles
- Jakarta EE compatibility (vs javax.* in Spring Boot 2.x)
- Spring Boot 3.2.x compatibility
- Automatic reactor context propagation

## Usage

Add this starter as a dependency to your Spring Boot 3 application:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-spring-boot3-starter</artifactId>
</dependency>
```