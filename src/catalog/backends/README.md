# GeoServer Catalog and ResourceStore for microservices

This is a `jar` module all geoserver micro-services shall depend on in order for their
`org.geoserver.catalog.Catalog` and `org.geoserver.platform.resource.ResourceStore`
spring beans be configured according to whatever strategy is commonly adopted.

## Build

Add the following dependency to each micro-service `pom.xml`:

```
    <dependency>
      <groupId>org.geoserver.cloud</groupId>
      <artifactId>gs-cloud-catalog-backend-starter</artifactId>
    </dependency>
```

## Configuration

Spring-boot's autoconfiguration SPI is used in order to automatically engage the correct `Catalog` implementation and bean wiring depending on
what's available in the class path. Hence, independently of which storage backend is used, it's only required to include this module as a dependency
and set the configuration properties as explained below.

