# GeoServer Cloud WebMVC Starter

This starter provides essential auto-configuration for servlet/webmvc-based GeoServer microservices. It serves as the foundational building block for all GeoServer Cloud service applications.

## Features

- Spring Boot Web MVC framework integration
- GeoServer catalog backend auto-configuration
- Spring Cloud service discovery (Eureka)
- Spring Cloud Config client integration
- Load balancing support
- Retry mechanisms for resilient configuration
- Spring Boot Actuator for monitoring and management
- Prometheus metrics exposure
- Data format support through the data-formats starter

## Dependencies

This starter aggregates several key dependencies:

- `gs-cloud-spring-boot-starter`: Core Spring Boot integration for GeoServer
- `gs-cloud-starter-catalog-backend`: GeoServer catalog backend configuration
- `gs-cloud-starter-data-formats`: Support for GeoServer data formats
- Spring Boot Web MVC components
- Spring Cloud Netflix Eureka client
- Spring Cloud Config client
- Spring Cloud Load Balancer
- Spring Retry and AOP support
- Spring Boot Actuator with Prometheus metrics

## Usage

This starter is automatically included in all GeoServer service applications through the parent POM. Individual services don't need to add it explicitly since it's already included in the parent dependency graph.

```xml
<!-- In the parent pom.xml of GeoServer microservices -->
<dependencies>
  <dependency>
    <groupId>org.geoserver.cloud</groupId>
    <artifactId>gs-cloud-starter-webmvc</artifactId>
  </dependency>
</dependencies>
```

## Configuration

The starter enables various Spring Boot auto-configurations but doesn't require specific properties. However, you can customize its behavior through standard Spring Boot and Spring Cloud properties in your application's configuration files.

Common configurations include:

```yaml
spring:
  application:
    name: my-geoserver-service
  cloud:
    config:
      uri: http://config-server:8888
    loadbalancer:
      ribbon:
        enabled: false

eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka-server:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## Integration with Other Starters

This starter is designed to work with other GeoServer Cloud starters:

- It's included by default in all GeoServer microservices
- It provides the foundation for the extensions starter
- It's required by the output-formats starter
- It works seamlessly with the security starter