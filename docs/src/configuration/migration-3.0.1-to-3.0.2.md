# Migrating from GeoServer Cloud 3.0.1 to 3.0.2

This guide lists the changes to check when upgrading from GeoServer Cloud 3.0.1
to 3.0.2. They concern the logging of the services.

> **Note**: The default configuration in the `/etc/geoserver` directory of the
> Docker images already has these changes. Check them if you use your own
> configuration files, or one of the removed Spring profiles.

## Logging profiles

Administrators select a logging profile in the web UI, under **Settings >
Global > Logging Settings > Logging Profile**, or with the `/rest/logging` REST
API, as in vanilla GeoServer. The running services apply the selected profile
at once, without a restart. See
[Select a logging profile](../user-guide/logging.md#select-a-logging-profile).

GeoServer Cloud 3.0.1 ignored the selected profile, and 3.0.2 applies it when a
service starts. A configuration migrated from vanilla GeoServer may select
another profile than `DEFAULT_LOGGING`. For example, with `PRODUCTION_LOGGING`,
the services no longer log the info messages of GeoServer Cloud. Check the
selection after the upgrade.

The profiles are defined under `geoserver.logging.profiles`, in
`geoserver_logging.yml`. If you use your own copy of this file, copy this
section from the new default file. Without it, the web UI lists
`DEFAULT_LOGGING` only.

## Removed: the logging Spring profiles

The `logging_*` Spring profiles of `geoserver_logging.yml` and
`gateway_logging.yml` are removed, and the `debug` Spring profile of
`gateway_logging.yml` too. A service with one of these profiles in
`SPRING_PROFILES_ACTIVE` still starts, but without the raised log levels.

Select the matching logging profile instead:

| Spring profile | Logging profile |
|---|---|
| `logging_debug_accesslog` | `DEBUG_ACCESS_LOG` |
| `logging_debug_acl` | `DEBUG_ACL` |
| `logging_debug_cache` | `DEBUG_CACHE` |
| `logging_debug_catalog`, `logging_trace_catalog` | `DEBUG_CATALOG` |
| `logging_debug_controlflow` | `DEBUG_CONTROL_FLOW` |
| `logging_debug_events` | `DEBUG_EVENTS` |
| `logging_debug_locks` | `DEBUG_LOCKS` |
| `logging_debug_security` | `DEBUG_SECURITY` |
| `logging_debug` | `GEOSERVER_DEVELOPER_LOGGING` |

A Spring profile set the levels of one service from its startup. A logging
profile sets the levels of all the GeoServer services once they have loaded
the GeoServer configuration, early in their startup. It does not apply to the
gateway. To debug the startup of a service before this point, or the gateway,
set `logging.level` properties on it, for example with an environment
variable:

```yaml
environment:
  LOGGING_LEVEL_ORG_GEOSERVER_CLOUD: debug
```

## Logs on the console only

The services wrote their logs to `/tmp/spring.log`, besides the console. They
now log to the console only. A tool reading this file gets no new lines.
Collect the console output of the containers instead.

The images set the `LOGGING_CONFIG` environment variable to
`file:/etc/geoserver/logback-spring.xml`. A service does not start if this file
is missing. If you mount your own directory at `/etc/geoserver`, add the
`logback-spring.xml` file of the default configuration to it, or set
`LOGGING_CONFIG` to an empty value. See
[Change the log output](../user-guide/logging.md#change-the-log-output).

## Removed: the Logs page

The Server menu of the web UI listed a Logs page. This page shows the log file
of vanilla GeoServer, and GeoServer Cloud writes no log file. The menu entry is
gone, and the URL of the page returns 404.

## Request IDs set by a proxy

The gateway and the services log each request with an ID, in the
`http.request.id` MDC attribute. The gateway now sends its ID to the service,
and the logs of both show the same ID for a request.

A proxy in front of the gateway, like Envoy, can set its own ID in the
`X-Request-ID` header. The gateway keeps this ID if the address of the proxy
matches the new `geoserver.gateway.trusted-proxies` setting, a regular
expression. Otherwise, it creates its own ID. If your proxy sets the ID, add
its address to the setting:

```yaml
geoserver:
  gateway:
    trusted-proxies: 10\.0\.0\.\d+
```

See [Write JSON logs](../user-guide/logging.md#write-json-logs) for the format
of the IDs.

## Client address behind a reverse proxy

The gateway resolves the client address of the requests in the same way as
Tomcat's remote IP valve, and tells it to the services in the
`X-Forwarded-For` header. It reads the `X-Forwarded-For` header of a request
only when a trusted proxy sent the request, and trusts no proxy by default.
Behind a reverse proxy or a Kubernetes ingress controller, set the new
`geoserver.gateway.trusted-proxies` setting to a regular expression matching
its addresses. Otherwise, the logs, the control-flow rules and the brute force
prevention of the services see the proxy address as the client address of the
requests. This is a setting of GeoServer Cloud's gateway, distinct from
`spring.cloud.gateway.server.webmvc.trusted-proxies`, which stays `.*`. See
[Client address behind a reverse
proxy](index.md#client-address-behind-a-reverse-proxy).
