# Observability starter

Spring-Boot starter project to treat application observability (logging, metrics, tracing) as a cross-cutting concern.

## Dependency

Add dependency

```xml
    <dependency>
      <groupId>org.geoserver.cloud</groupId>
      <artifactId>gs-cloud-starter-observability</artifactId>
      <version>${project.verison}</version>
    </dependency>
```

## Logstash formatted JSON Logging

The `net.logstash.logback:logstash-logback-encoder` dependency allows to write logging entries as JSON-formatted objects in Logstash scheme.

The application must be run with the `json-logs` Spring profile, as defined in [logback-spring.xml](src/main/resources/logback-spring.xml).
