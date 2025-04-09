# Spring Cloud Integration Starter

This starter provides comprehensive Spring Cloud integration for GeoServer Cloud microservices, including service discovery, distributed configuration, and event bus capabilities.

## Features

- **Service Discovery**: Integration with Netflix Eureka for service registration and discovery
- **Configuration Management**: Spring Cloud Config client for centralized, externalized configuration
- **Event Bus Integration**: Spring Cloud Bus for distributed messaging
- **Catalog Event Bridging**: Connects GeoServer catalog events to Spring Cloud Bus

## Usage

Add this dependency to enable Spring Cloud capabilities in a GeoServer microservice:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-spring-cloud-starter</artifactId>
</dependency>
```

Most GeoServer microservices already include this dependency through their parent POM.

## Configuration

### Deployment Scenarios

#### Docker Compose Deployment

For Docker Compose deployments, you'll typically use the full Spring Cloud infrastructure:

- Eureka service for service discovery and client-side load balancing
- Config Server for centralized configuration
- RabbitMQ for the event bus

```yaml
spring:
  application:
    name: my-service
  cloud:
    discovery:
      enabled: true
    config:
      enabled: true
      uri: http://config:8888
      failFast: true
eureka:
  client:
    enabled: true
    serviceUrl:
      defaultZone: http://discovery:8761/eureka
  instance:
    preferIpAddress: true
```

#### Kubernetes Deployment

For Kubernetes deployments, it's recommended to use the `standalone` Spring profile and rely on Kubernetes native capabilities:

- Kubernetes Services for service discovery and load balancing
- ConfigMaps or Secrets for configuration
- RabbitMQ (or other message broker) for the event bus

```yaml
spring:
  application:
    name: my-service
  profiles:
    active: standalone
  cloud:
    discovery:
      enabled: false
    config:
      enabled: false
eureka:
  client:
    enabled: false
```

In Kubernetes, you should define appropriate Services for each GeoServer microservice, which will enable automatic load balancing, while in Docker Compose eureka provides client-side load balancing.

### Event Bus

```yaml
spring:
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: guest
  cloud:
    bus:
      enabled: true
    stream:
      bindings:
        springCloudBusOutput:
          destination: gscatalog
        springCloudBusInput:
          destination: gscatalog
```

## Scope

This starter provides a complete Spring Cloud integration package, including all dependencies required for a GeoServer service to function as a full Spring Cloud microservice. 

Event bus functionality, which bridges GeoServer catalog events to Spring Cloud Bus events, is a core part of this starter but is complemented with other Spring Cloud features like service discovery and configuration.