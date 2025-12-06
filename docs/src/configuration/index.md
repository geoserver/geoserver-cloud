# Cloud Native GeoServer externalized configuration guide

## Introduction

GeoServer Cloud provides extensive configuration options through Spring Boot's externalized configuration system. This document outlines the various configuration options available for customizing your GeoServer Cloud deployment.

## JNDI Datasources

A custom and very simple JNDI implementation is used through the `org.geoserver.cloud:spring-boot-simplejndi` maven module, allowing to configure JNDI data sources through Spring-Boot's configuration properties.

```yaml
jndi:
  datasources:
    # the data source names (i.e. ds1, ds2) will be bound as java:comp/env/jdbc/ds1 and java:comp/env/jdbc/ds2:
    ds1:
      enabled: true
      wait-for-it: true
      wait-timeout: 60
      url: jdbc:postgresql://host:5432/database
      username: sa
      password: sa
      connection-timeout: 250
      idle-timeout: 60000
```

On Kubernetes, you can for example mount it as a config map in all the services.

## GeoServer configuration properties

## HTTP proxy for cascaded OWS (WMS/WMTS/WFS) Stores

Cascaded OWS stores make use of a SPI (Service Provider Interface) extension point to configure the appropriate GeoTools `HTTPClientFactory`.

We provide a Spring Boot Auto-configuration that can be configured through regular spring-boot externalized properties and only affects GeoTools HTTP clients instead of the whole JVM.

The usual way to set an http proxy is through the `http.proxyHost`, `http.proxyPort`,
`http.proxyUser`, `http.proxyPassword` Java System Properties.

In the context of Cloud Native GeoServer containerized applications, this presents a number of drawbacks:

* Standard Java proxy parameters only work with System properties,
  not OS environment variables, and setting system properties is more
  cumbersome than env variables (you have to modify the container run command).
* `http.proxyUser/Password` are not standard properties, though commonly used, it's kind of
JDK implementation dependent.
* Setting `-Dhttp.proxy* System properties affects all HTTP clients in the container, meaning
requests to the config-service, discovery-service, etc., will also try to go through the proxy,
or you need to go through the extra burden of figuring out how to ignore them.
* If the proxy is secured, and since the http client used may not respect the
http.proxyUser/Password parameters, the apps won't start since they'll get
HTTP 407 "Proxy Authentication Required".

The following externalized configuration properties apply, with these suggested default values:

```yaml
# GeoTools HTTP Client proxy configuration, allows configuring cascaded WMS/WMTS/WFS stores
# that need to go through an HTTP proxy without affecting all the http clients at the JVM level
# These are default settings. The enabled property can be set to false to disable the custom
# HTTPClientFactory altogether.
# The following OS environment variables can be set for easier configuration:
# HTTP(S)_PROXYHOST, HTTP(S)_PROXYPORT, HTTP(S)_PROXYUSER, HTTP(S)_PROXYPASSWORD, HTTP(S)_NONPROXYHOSTS
geotools:
  httpclient:
    proxy:
      enabled: true
      http:
        host: ${http.proxyHost:}
        port: ${http.proxyPort:}
        user: ${http.proxyUser:}
        password: ${http.proxyPassword:}
        nonProxyHosts: ${http.nonProxyHosts:localhost.*}
        # comma separated list of Java regular expressions, e.g.: nonProxyHosts: localhost, example.*
      https:
        host: ${https.proxyHost:${geotools.httpclient.proxy.http.host}}
        port: ${https.proxyPort:${geotools.httpclient.proxy.http.port}}
        user: ${https.proxyUser:${geotools.httpclient.proxy.http.user}}
        password: ${https.proxyPassword:${geotools.httpclient.proxy.http.password}}
        nonProxyHosts: ${https.nonProxyHosts:${geotools.httpclient.proxy.http.nonProxyHosts}}
```

### Configure HTTP proxy with environment variables in compose.yml

As mentioned above, regular JVM proxy configuration works with Java System properties
but not with Operating System environment variables.

The above `geotools.httpclient.proxy` config properties though allow to do so
easily as in the following `compose.yml` snippet:

```yaml
version: "3.8"
...
services:
...
  wms:
    image: geoservercloud/geoserver-cloud-wms:<version>
    environment:
      HTTP_PROXYHOST: 192.168.86.26
      HTTP_PROXYPORT: 80
      HTTP_PROXYUSER: jack
      HTTP_PROXYPASSWORD: insecure
...
```

## Configure admin user through environment variables

For Cloud-Native deployments, an `AuthenticationProvider` exists that allows to set an administrator account (username and password) through environment variables `GEOSERVER_ADMIN_USERNAME`/`GEOSERVER_ADMIN_PASSWORD`,
or Java System Properties `geoserver.admin.username` and `geoserver.admin.password`.

Useful for devOps to set the admin password through a Kubernetes secret,
instead of having to tweak the security configuration XML files with an init container or similar.

This authentication provider will be the first one tested for an HTTP Basic authorization, only
if both the above mentioned username and password config properties are provided,
and regardless of the authentication chain configured in GeoServer.

If only one of the `geoserver.admin.username` and `geoserver.admin.password` config properties
is provided, the application will fail to start.

If enabled (i.e. both admin username and password provided), a failed attempt to log
in will cancel the authentication chain, and no other authentication providers will be tested.

If the default `admin` username is used, it effectively overrides the admin password set in the
xml configuration. If a separate administrator username is given, the regular
`admin` user is **disabled**.

## Change the default geoserver path

By default, GeoServer Cloud services are available under `geoserver/cloud/web` path. You can change it by using the `GEOSERVER_BASE_PATH` or Java System Properties `geoserver.base-path`.

## Use GeoServer ACL

To use GeoServer ACL with GeoServer Cloud, you need to do the following steps:
- Have a database available for storing the GeoServer ACL configuration
- Add a GeoServer ACL instance to your deployment
- Update the GeoServer spring configuration to enable ACL based security (you can use the `acl` spring profile)

## Use OAuth authentication

You can enable OAuth authentication by replacing the default `gateway` image by the geOrchestra gateway (for example `georchestra/gateway:23.1-RC1`).

## Filter vector and raster data formats

GeoServer Cloud provides the ability to filter which GeoTools DataAccessFactory (vector) and GridFormatFactorySpi (raster) implementations are available in the application. This allows you to customize each deployment to only include the formats you need, improving security, reducing the attack surface, and potentially improving startup time.

### Configuration properties

The following configuration properties can be used to control which formats are available:

```yaml
geotools:
  data:
    filtering:
      # Master switch for the entire filtering system
      enabled: true
      
      # Vector format configuration (DataAccessFactory implementations)
      vector-formats:
        "[PostGIS]": true
        "[Shapefile]": true
        "[GeoPackage]": true
        "[Oracle NG]": ${oracle.enabled:false}
        "[Web Feature Server (NG)]": true
        "[Microsoft SQL Server]": false
        # Add more vector format entries as needed
      
      # Raster format configuration (GridFormatFactorySpi implementations)
      raster-formats:
        "[GeoTIFF]": true
        "[ImageMosaic]": ${mosaic.enabled:true}
        "[ArcGrid]": false
        "[WorldImage]": true
        "[ImagePyramid]": false
        # Add more raster format entries as needed
```

### Format names

The format names used in the configuration are the user-friendly display names returned by the respective factories:

- For vector formats: The name returned by `DataAccessFactory.getDisplayName()`
- For raster formats: The name returned by `AbstractGridFormat.getName()`

Since these names often contain special characters, they should be properly escaped in the YAML configuration using quotes and brackets.

### Placeholder resolution

Both vector and raster format configurations support Spring property placeholder resolution, allowing you to create dynamic configurations using environment variables or system properties. For example:

```yaml
vector-formats:
  "[Oracle NG]": ${oracle.enabled:false}
  "[PostGIS]": ${postgis.enabled:true}
```

This will enable the Oracle format only if the `oracle.enabled` property is set to `true`, otherwise it will default to `false`.

### Implementation details

The filtering system uses different approaches for vector and raster formats:

- **Vector formats**: Directly deregisters disabled DataAccessFactory implementations using `DataAccessFinder.deregisterFactory()` and `DataStoreFinder.deregisterFactory()`
- **Raster formats**: Uses a custom FilteringFactoryCreator wrapper around the standard GridFormatFinder registry to filter formats on-the-fly

For more details, refer to the README files in the respective starter modules:
- Input formats: `src/starters/input-formats/README.md`
