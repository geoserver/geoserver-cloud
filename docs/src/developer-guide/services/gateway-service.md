# Cloud Native GeoServer Gateway Service

The gateway is the single entry point for all HTTP requests to GeoServer Cloud.
It routes requests to the appropriate backend microservice (WFS, WMS, WCS, WPS, REST, GWC, WebUI)
using service discovery and client-side load balancing.

Starting with GeoServer Cloud 3.0.0, the gateway is built on
[Spring Cloud Gateway Server MVC](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html)
(servlet-based with virtual threads).
The previous WebFlux-based gateway was available for 3.0.x only and was removed in 3.1.0.

**Docker image**: `geoservercloud/geoserver-cloud-gateway`

**Service name**: `gateway`

## Architecture

The gateway is a Spring Boot application that uses Spring Cloud Gateway Server MVC to define
HTTP routes. Each route maps a set of request predicates (path patterns, query parameters)
to a backend service URI, optionally applying filters to transform the request or response.

Service targets are resolved via:

- **Eureka service discovery** (default): routes use `lb://` URIs (e.g., `lb://wms`) for client-side load-balanced routing across all healthy instances of a service.
- **Static targets** (`standalone` profile): routes use fixed HTTP URLs (e.g., `http://wms:8080`), useful for Docker Compose or Kubernetes deployments without a discovery server.

Routes are defined in `config/gateway.yml`.

## Custom Filters

SCG Server MVC does not support `default-filters`, so each filter must be added explicitly to every
route that needs it. The following custom filters are provided:

### StripBasePath

Strips a configurable base path prefix from request URIs before forwarding to downstream services.
This enables deploying GeoServer Cloud under a sub-path (e.g., `/geoserver/cloud`).

```yaml
filters:
  - StripBasePath=/geoserver/cloud
```

For example, a request to `/geoserver/cloud/wms?SERVICE=WMS` is forwarded as `/wms?SERVICE=WMS`.

### SharedAuth

Shares WebUI authentication with backend services via the HTTP session. This filter has two phases:

- **Pre-processing**: strips incoming `x-gsc-username` and `x-gsc-roles` headers to prevent external impersonation, then injects session-stored credentials for downstream services.
- **Post-processing**: captures `x-gsc-username` and `x-gsc-roles` from the proxied response, updates the gateway session, and removes these headers from the final client response.

```yaml
filters:
  - SharedAuth
```

Can be disabled with `geoserver.security.gateway-shared-auth.enabled=false`.

### SecureHeaders

Adds security-related HTTP response headers (`X-Frame-Options`, `X-Content-Type-Options`,
`Strict-Transport-Security`, `Referrer-Policy`, etc.). This bridges the WebFlux-only
`SecureHeadersGatewayFilterFactory` that is absent in SCG Server MVC.

```yaml
filters:
  - SecureHeaders
```

Headers already set by the backend service are not overwritten.
Configurable via `spring.cloud.gateway.server.webmvc.filter.secure-headers.*` (see [Secure Headers configuration](#secure-headers) below).

### RouteProfile

Conditionally activates a route based on Spring profiles. Returns a configurable HTTP status code
when the profile condition is not met. This is useful because SCG MVC routes are defined as a list
and cannot be selectively overridden per profile.

```yaml
filters:
  - RouteProfile=dev,403
```

Supports negation: `RouteProfile=!production,404` proceeds only when the `production` profile is *not* active.

## Custom Predicates

### RegExpQuery

Matches query parameters by regular expression on both parameter name and value.
Enables case-insensitive OWS service parameter matching, which is necessary because
OGC clients may send `SERVICE=WMS`, `service=wms`, or any mixed case variant.

```yaml
predicates:
  - RegExpQuery=(?i:service),(?i:wms)
```

If the value regex is omitted or blank, only the presence of a matching parameter name is required.

## Service Configuration

### Base Path

Set `geoserver.base-path` (or the environment variable `GEOSERVER_BASE_PATH`) to deploy GeoServer Cloud
under a sub-path. Default: empty (served at root). The `StripBasePath` filter removes this prefix
before forwarding requests to backend services.

The gateway normalizes the value before routes consume it: `/` is equivalent to empty (served at
root), trailing slashes are dropped (`/geoserver/` means `/geoserver`), and a missing leading slash
is added (`geoserver` means `/geoserver`).

```yaml
geoserver:
  base-path: /geoserver/cloud
```

### Service Targets

Service targets are defined at the top of `config/gateway.yml`:

```yaml
targets:
  wfs: lb://wfs       # Eureka-based (default)
  wms: lb://wms
  # ...
```

The `standalone` profile overrides these with static URLs:

```yaml
# spring.config.activate.on-profile: standalone
targets:
  wfs: http://wfs:8080
  wms: http://wms:8080
```

The `local` profile uses localhost URLs for IDE-based development.

### Secure Headers

Configured under `spring.cloud.gateway.server.webmvc.filter.secure-headers`:

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Master switch |
| `disable` | `[]` | List of header names to skip (e.g., `content-security-policy`) |
| `frame-options` | `DENY` | `X-Frame-Options` value |
| `xss-protection-header` | `1 ; mode=block` | `X-Xss-Protection` value |
| `strict-transport-security` | `max-age=631138519` | `Strict-Transport-Security` value |
| `referrer-policy` | `no-referrer` | `Referrer-Policy` value |
| `content-type-options` | `nosniff` | `X-Content-Type-Options` value |

### CORS

Configured under `spring.cloud.gateway.server.webmvc.globalcors.cors-configurations`:

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          globalcors:
            cors-configurations:
              "[/**]":
                allowedOrigins: "*"
                allowedHeaders: "*"
                allowedMethods: GET, PUT, POST, DELETE, OPTIONS, HEAD
```

### Profiles

| Profile | Description |
|---------|-------------|
| `discovery-eureka` | Enables Eureka service discovery with 5-second registry refresh |
| `standalone` | Uses static service target URLs instead of discovery |
| `local` | Development mode: port 9090, localhost targets |
| `bootstrap-standalone` | Loads external config from `/etc/geoserver/gateway-webmvc.yml` |

## Developing

**Module path**: `src/apps/infrastructure/gateway-webmvc`

Build and run tests:

```bash
mvn -pl src/apps/infrastructure/gateway-webmvc verify
```

Run locally with IDE: activate the `local` and `discovery-eureka` profiles.
The gateway will start on port 9090 and connect to a local Eureka server.
