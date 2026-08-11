# GeoServer Cloud Gateway (WebMVC)

API gateway for GeoServer Cloud, built on [Spring Cloud Gateway Server MVC](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html).
Serves as the single entry point for all client requests, routing them to backend microservices
(WFS, WMS, WCS, WPS, REST, GWC, WebUI) via service discovery or static targets.

Replaces the previous WebFlux-based (reactive) gateway starting with GeoServer Cloud 3.0.0.

**Docker image**: `geoservercloud/geoserver-cloud-gateway`

**Spring application name**: `gateway`

**Maven artifact**: `gs-cloud-gateway-webmvc`

## Key Features

- Servlet-based (Spring MVC) with virtual threads for high concurrency
- Service discovery routing via Eureka (`lb://` URIs) or static targets
- Custom gateway filters: `StripBasePath`, `SharedAuth`, `SecureHeaders`, `RouteProfile`
- Custom route predicates: `RegExpQuery` (regex matching on query parameter name and value)
- CORS support via `globalcors` configuration
- `X-Forwarded-*` header propagation for reverse proxy deployments
- Prometheus metrics and health check endpoints

## Configuration

Externalized configuration is loaded from `config/gateway.yml` (or `/etc/geoserver/gateway.yml` inside Docker containers).

See the [Gateway Service developer guide](../../../../docs/src/developer-guide/services/gateway-service.md) for full configuration details.

## Differences from WebFlux Gateway

The deprecated WebFlux gateway (`geoservercloud/geoserver-cloud-gateway-webflux`) was published for
3.0.0 only and removed in 3.1.0. See the
[2.28 to 3.0 migration guide](../../../../docs/src/configuration/migration-2.28-to-3.0.md)
for the differences between the two implementations.
