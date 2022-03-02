# Advanced Message Queuing Protocol catalog notification support

Dependency management to enable AMQP transport layer for spring-cloud-bus based catalog and configuration event notifications.

Add this dependency to use AMQP as the transport layer for inter-service event notification and configure the application's `spring-cloud-stream` binder:

`pom.xml`:

```xml
    <dependency>
      <groupId>org.geoserver.cloud</groupId>
      <artifactId>gs-cloud-catalog-event-bus-amqp</artifactId>
    </dependency>
```

`application.yml`/`config/service-name.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
  cloud:
    bus.enabled: true
    stream:
      bindings:
        springCloudBusOutput:
          destination: gscatalog
        springCloudBusInput:
          destination: gscatalog
```