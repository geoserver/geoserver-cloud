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
- Client address resolved from the trusted proxies, like Tomcat's remote IP valve (`geoserver.gateway.trusted-proxies`)
- Prometheus metrics and health check endpoints

## Configuration

Externalized configuration is loaded from `config/gateway.yml` (or `/etc/geoserver/gateway.yml` inside Docker containers).

See the [Gateway Service developer guide](../../../docs/src/developer-guide/services/gateway-service.md) for full configuration details.

## Differences from WebFlux Gateway

The deprecated WebFlux gateway (`geoservercloud/geoserver-cloud-gateway-webflux`) will be available for 3.0.0 only and removed in 3.1.0.

| Aspect | WebMVC (new default) | WebFlux (deprecated) |
|--------|---------------------|---------------------|
| Runtime model | Servlet + virtual threads | Reactive (Netty) |
| WebSocket routing | Not supported | Supported |
| `default-filters` | Not supported; filters must be listed per route | Supported |
| Config namespace | `spring.cloud.gateway.server.webmvc` | `spring.cloud.gateway.server.webflux` |
| Docker image | `geoservercloud/geoserver-cloud-gateway` | `geoservercloud/geoserver-cloud-gateway-webflux` |
