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
and set the configuration properties as explained bellow.

### Configuring JDBCConfig and JDBCStore

During this proof of concept phase, the catalog and resource store implementations in use is based on GeoServer's 
[jdbcconfig](https://docs.geoserver.org/latest/en/user/community/jdbcconfig/) and [jdbcstore](https://docs.geoserver.org/latest/en/user/community/jdbcstore/)
community modules, respectively.

Note, however, the following differences apply against the regular `jdbcconfig` and `jdbcstore` configuration in a monolithic GeoServer deployment:

* The catalog and resource store configuration is provided through Spring properties (i.e. in `application.properties` or `application.yml`), 
instead of through `.properties` files in the data directory, or environment variables indicating the location of such files;
* The configuration settings for the catalog and the resource store are merged into a single set of properties under the `geoserver.jdbcconfig` namespace;
* There's a single `java.sql.DataSource` configured for both the catalog and the resource store, instead of separate ones;
* If using the H2 embedded database (should be the case only for testing), the JDBC driver version is `1.4.200` as opposed to the `1.1.119` version that
comes as a transitive dependency with GeoServer;
* Database schema initialization is not attempted by default, and should only be used in test code. The database is expected to have the correct schema, and such
is the case for the `gs-clould-database` PostgreSQL Docker image.

When a GeoServer microservice uses externalized configuration (e.g. through consul, or spring-cloud-config), the configuration properties reside in the config service's
resource store for the microservice in question, or the config service's global properties (for example, for service `foo-service`, they could be either at
`config/foo-service.yml` or at the root `config/application.yml`, where the former contains all properties that are service-specific, and the later
all properties that are shared among all microservices).

The following are the possible configuration properties:

`application.yml`:

```yaml
geoserver:
  jdbcconfig:
    enabled: true
    initdb: false
    datasource:
      jdbcUrl: "jdbc:postgresql://database:5432/geoserver_config"
      username: geoserver
      password:
      driverClassname: org.postgresql.Driver
```

`application.properties`:

```properties
geoserver.jdbcconfig.enabled=true
geoserver.jdbcconfig.initdb=false
geoserver.jdbcconfig.datasource.jdbc-url=jdbc\:postgresql\://database\:5432/geoserver_config
geoserver.jdbcconfig.datasource.username=sa
geoserver.jdbcconfig.datasource.password=
geoserver.jdbcconfig.datasource.driverClassname=org.postgresql.Driver
```
