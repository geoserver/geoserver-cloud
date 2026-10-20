# Logging

GeoServer Cloud services write their logs to standard output, and write no
log files. In a container, the platform collects them: the Docker logging
driver, or Kubernetes. Spring Boot controls the logging, not the logging
configuration of vanilla GeoServer.

This guide shows how to change the log levels, at startup or while the
services run, and how to change the log output.

## Select a logging profile

A logging profile is a named set of log levels. Select one in the web UI,
under **Settings > Global > Logging Settings > Logging Profile**, then save the
page. All running service instances apply the profile at once, and instances
started later apply it at startup. The selection is stored with the GeoServer
configuration, as in vanilla GeoServer.

Next to the list, the page shows the log levels set by the selected profile.
For `DEFAULT_LOGGING`, it shows the levels of the configuration
(`logging.level`), as read by the web UI service.

The REST API reads and changes the selection at `/rest/logging`. The profile
name goes in the `level` property:

```shell
curl -u admin:geoserver -X PUT -H 'Content-Type: application/json' \
  -d '{"logging": {"level": "VERBOSE_LOGGING"}}' \
  http://localhost:9090/geoserver/cloud/rest/logging
```

Select `DEFAULT_LOGGING` again to go back to the configured log levels.

`DEFAULT_LOGGING` is always defined. The default configuration defines the
other profiles of vanilla GeoServer in `geoserver_logging.yml`, with the same
names:

| Profile | Log levels |
|---|---|
| `DEFAULT_LOGGING` | The levels of the externalized configuration, see below |
| `PRODUCTION_LOGGING` | Warnings and errors of GeoServer and GeoTools, errors of GeoWebCache and Spring |
| `VERBOSE_LOGGING` | All messages of GeoServer and GeoTools |
| `GEOSERVER_DEVELOPER_LOGGING` | All messages of GeoServer |
| `GEOTOOLS_DEVELOPER_LOGGING` | All messages of GeoTools |
| `QUIET_LOGGING` | No messages at all |

It also defines profiles to debug one part of GeoServer Cloud:

| Profile | Log levels |
|---|---|
| `DEBUG_ACCESS_LOG` | The access log of the OWS requests, besides the REST requests |
| `DEBUG_ACL` | Debug messages of the GeoServer ACL plugin |
| `DEBUG_CACHE` | Debug messages of the catalog and ACL caches |
| `DEBUG_CATALOG` | Debug messages of the catalog and its backend, with the SQL requests of pgconfig |
| `DEBUG_CONTROL_FLOW` | Debug messages of the control flow extension |
| `DEBUG_EVENTS` | Debug messages of the events sent between the services |
| `DEBUG_LOCKS` | All messages of the configuration locks |
| `DEBUG_SECURITY` | Debug messages of Spring Security and the GeoServer security |

A profile changes the levels of its loggers and of their descendants. For
example, `VERBOSE_LOGGING` sets `org.geoserver` to `trace`, and
`org.geoserver.catalog` gets `trace` too, even if the configuration sets it
to `info`. The other loggers keep their configured levels.

With an unknown profile name, like the `TEST_LOGGING` profile of vanilla
GeoServer, the configured levels apply, and the services log a warning.

The gateway has no GeoServer configuration. It does not apply the profiles,
and keeps its configured levels.

## Define logging profiles

Add profiles under `geoserver.logging.profiles` in the externalized
configuration, for example in `geoserver_logging.yml`. Each profile maps logger
names to levels. This profile logs the SQL statements of the JDBC data stores,
such as PostGIS:

```yaml
geoserver:
  logging:
    profiles:
      SQL_LOGGING:
        org.geotools.jdbc: debug
```

The services read the profiles at startup. After a restart, the web UI lists
`SQL_LOGGING` with the other profiles. The list shows `DEFAULT_LOGGING` first,
then the profiles in the order of the configuration.

The levels are the ones of Spring Boot: `trace`, `debug`, `info`, `warn`,
`error`, and `off`.

## Configure the log levels

The `logging.level` properties of the externalized configuration set the log
levels at startup, as in any Spring Boot application. `DEFAULT_LOGGING` keeps
them, and the other profiles apply on top of them.

```yaml
logging:
  level:
    root: warn
    org.geoserver: warn
    org.geoserver.cloud: info
```

The default configuration splits the logging settings in three files:

| File | Settings |
|---|---|
| `geoserver_logging.yml` | The log levels and the logging profiles of the GeoServer services |
| `gateway_logging.yml` | The log levels of the gateway |
| `common_logging.yml` | The access log and the MDC attributes, for the gateway and the services |

A service applies the selected logging profile once it has loaded the GeoServer
configuration, early in its startup. To debug the startup before this point,
set `logging.level` properties on this service, for example with an
environment variable. This one logs the debug messages of GeoServer Cloud from
the start:

```yaml
environment:
  LOGGING_LEVEL_ORG_GEOSERVER_CLOUD: debug
```

## Change the log output

The images set the `LOGGING_CONFIG` environment variable to
`file:/etc/geoserver/logback-spring.xml`, the `logback-spring.xml` file of the
default configuration. Spring Boot reads the Logback configuration from this
file. This configuration writes the logs to standard output only.

Edit or mount this file to change the appenders or the format of the log
lines. To load another file, set `LOGGING_CONFIG` to its location, for example
`file:/path/to/logback.xml`. With an empty `LOGGING_CONFIG`, the services use
the configuration bundled in their jar. It is the same as the shipped file.

## Write JSON logs

With the `json-logs` Spring profile, the services and the gateway write each
log event as a JSON object, on one line. Log collectors parse these lines
without a custom pattern, unlike plain text lines.

Add `json-logs` to the active Spring profiles of the services and the gateway,
for example in the `SPRING_PROFILES_ACTIVE` environment variable:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: "pgconfig,json-logs"
```

With the Docker Compose files of the repository, set `LOGGING_PROFILE=json-logs`
in `compose/.env`.

The lines follow the JSON format of the Logstash Logback encoder. This is the
access log line of a WMS request, formatted for reading:

```json
{
  "@timestamp": "2026-10-16T16:20:13.115411237Z",
  "@version": "1",
  "message": "GET 200 /geoserver/cloud/wms ",
  "logger_name": "org.geoserver.cloud.accesslog",
  "thread_name": "http-nio-8080-exec-1",
  "level": "DEBUG",
  "level_value": 10000,
  "application.name": "wms",
  "application.version": "3.1.0-SNAPSHOT",
  "application.instance.id": "wms:192.168.156.5:8080",
  "http.request.id": "01m52r73xnzzmjaaqry1ksk017",
  "http.request.method": "GET",
  "http.request.url": "/geoserver/cloud/wms",
  "enduser.authenticated": "true",
  "enduser.id": "admin",
  "gs.ows.service.name": "wms",
  "gs.ows.service.version": "1.3.0",
  "gs.ows.service.operation": "GetCapabilities"
}
```

Each line has these fields:

| Field | Content |
|---|---|
| `@timestamp` | The time of the event, in ISO 8601 format |
| `@version` | The version of the Logstash format, always `1` |
| `message` | The message |
| `logger_name` | The name of the logger |
| `thread_name` | The name of the thread |
| `level` | The level: `TRACE`, `DEBUG`, `INFO`, `WARN`, or `ERROR` |
| `level_value` | The level as a number, from `5000` for `TRACE` to `40000` for `ERROR` |
| `stack_trace` | The stack trace of the exception, if there is one |

The lines logged while a service handles a request also have the MDC
attributes of the request, set by GeoServer Cloud. The `logging.mdc.include`
properties of `common_logging.yml` select them. The default configuration sets
these attributes:

| Field | Content |
|---|---|
| `application.name` | The name of the service, such as `wms` |
| `application.version` | The version of the service |
| `application.instance.id` | The service instance: its name, IP address, and port |
| `http.request.id` | An identifier of the request, the same in the gateway and in the service. See below |
| `http.request.method` | The HTTP method |
| `http.request.url` | The path of the request URL |
| `enduser.authenticated` | Whether the user is authenticated |
| `enduser.id` | The name of the user |
| `gs.ows.service.name` | The OWS service, such as `wms` |
| `gs.ows.service.version` | The version of the OWS service |
| `gs.ows.service.operation` | The OWS operation, such as `GetMap` |
| `gs.ows.service.format` | The output format of the OWS request, if it has one |

The gateway sets the `application` and `http` attributes only. Other
`logging.mdc.include` properties add more attributes, like the roles of the
user, the query string, the request headers, or the remote address.

The gateway creates the request ID, and sends it to the service in the
`X-Request-ID` header. A proxy in front of the gateway, like Envoy, can set
its own ID in the same header, to follow a request in its own logs too. The
gateway accepts this ID only from the trusted proxies, matched by the regular
expression of `geoserver.gateway.trusted-proxies`:

```yaml
geoserver:
  gateway:
    trusted-proxies: 10\.0\.0\.\d+
```

The gateway ignores the ID sent by other clients, and creates its own. The
expression must match the address of the proxy, as seen by the gateway. The
gateway does not read the `X-Forwarded-For` header for this check. List a
proxy only if it sets the ID of each request itself, instead of forwarding the
ID received from its clients. By default, the gateway trusts no proxy. The
same setting selects the proxies trusted to tell the client address of the
requests, see [Client address behind a reverse
proxy](../configuration/index.md#client-address-behind-a-reverse-proxy).

The gateway and the services ignore an ID longer than 64 characters, or with
characters other than letters, digits, `-`, `_`, and `.`.

By default, the access log logs the REST requests only. Set the
`org.geoserver.cloud.accesslog` logger to `debug` to log the OWS requests too,
as in the example above.

At startup, the JVM writes a few lines of its own before the logging starts,
such as `Picked up JAVA_TOOL_OPTIONS`. These lines are not JSON.
