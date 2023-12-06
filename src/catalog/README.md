# Catalog

Dependency graph:

```
                      (pluggable-catalog-support) -----> [gs-main]
                                    ^
                                    |
 (catalog-event-bus) <-------- (catalog-backend-starter) ------> (catalog-cache)
                                 /  |           \
                                /   |            \--> [gs-jdbcconfig]
                               /    |             \
                              /     |              \--> <other catalog backends>...
                             /      | 
                            /       +--> (pgconfig)
                           /    
                   (data-directory)
```

## pluggable-catalog-support

Replaces the typical catalog facade (DAO) by a version that supports easy extension through DDD-like repository abstractions for the different kinds of catalog objects. This allows all the logic related to catalog business rules that exist in `CatalogImpl` and `DefaultCatalogFacade` to remain untouched, and only having to provide `CatalogInfo` repository implementations to plug-in a different backend, without caring of `ModificationProxy` and event-dispatching logic; breaking the need duplicating that logic on alternate catalog facade implementations when a new backend needs to be implemented.

## catalog-event-bus

Implements `spring-could-bus` based event notification of catalog and configuration files changes. Acts both as an emiter of remote catalog events and a conveyor of incoming events to the local spring `ApplicationContext`. By default just clears out the catalog resource pool. `catalog-backend-starter` must take care of providing auto-configuration for each supported backend's own set of `RemoteCatalogEvent` listeners in order react accordingly to their needs.


## catalog-backend-starter

Provides spring atuo-configuration for several catalog back-ends. Namely: traditional file based data directory, jdbcconfig, and pgconfig. More can be added as implementations are developed.
 
 depends on: 
  * gs-cloud-catalog-events
  * gs-cloud-catalog-cache
  * gs-cloud-catalog-backend-datadir
  * gs-cloud-catalog-backend-jdbcconfig
  * gs-cloud-catalog-backend-pgsql

## catalog-cache

Based on `spring-boot-starter-cache`, decorates the application catalog's backend repositories to cache `CatalogInfo` objects and `ResourceStore` files. Listens to `RemoteCatalogEvent` and `RemoteResourceChangeEvent` notifications to evict entries from the spring cache. The spring cache can be any of the spring-cache [supported providers](https://docs.spring.io/spring-boot/docs/1.3.0.M1/reference/html/boot-features-caching.html#_supported_cache_providers).

 depends on: 
  * catalog-event-bus

# Common configuration properties
The following configuration properties apply to all *GeoServer* microservices (i.e. not edge services):

```
geoserver.security.enabled=true #flag to turn off geoserver security auto-configuration
geoserver.proxy-urls.enabled=true #flag to turn off proxyfing response URL's based on gateway's provided HTTP request headers (X-Forwarded-*)
geoserver.web.resource-browser.enabed=true
geoserver.servlet.enabled=true #flag to turn off auto-configuration of geoserver servlet context
geoserver.servlet.filter.session-debug.enabled=true #flag to disable the session debug servlet filter
geoserver.servlet.filter.flush-safe.enabled=true #flag to disable the flush-safe servlet filter

geoserver.jdbcconfig.enabled=true
geoserver.jdbcconfig.web.enabled=true
geoserver.jdbcconfig.initdb=false
geoserver.jdbcconfig.datasource.jdbc-url=jdbc\:postgresql\://database\:5432/geoserver_config
geoserver.jdbcconfig.datasource.username=sa
geoserver.jdbcconfig.datasource.password=
geoserver.jdbcconfig.datasource.driverClassname=org.postgresql.Driver
```
