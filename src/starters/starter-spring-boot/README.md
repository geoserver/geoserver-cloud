# GeoServer Spring-boot starter

This is a `jar` module providing additional spring-boot-starter configuration classes.

- `org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader` allows to
cherry pick which Spring beans to load from specific jar dependencies, using regular expressions. Especially
useful to avoid redefining GeoServer's `applicationContext.xml` configuration files as Java configurations,
but still being able of selecting exactly which beans to load to the application context based
on specific cloud module requirements (i.e. through spring-boot autoconfigurations).

## Usage

Add the following dependency to each micro-service `pom.xml`:

```
    <dependency>
      <groupId>org.geoserver.cloud</groupId>
      <artifactId>gs-cloud-spring-boot-starter</artifactId>
    </dependency>
```
