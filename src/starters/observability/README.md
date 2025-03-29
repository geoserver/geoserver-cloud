# Observability starter

Spring-Boot starter project to treat application observability (logging, metrics, tracing) as a cross-cutting concern.

## WebFlux Applications Note

**Important**: This module only supports servlet-based applications running on Spring Boot 2.7. For WebFlux (reactive) applications or applications using Spring Boot 3, use the Spring Boot 3 compatible module instead:

```xml
<dependency>
  <groupId>org.geoserver.cloud</groupId>
  <artifactId>gs-cloud-starter-observability-spring-boot-3</artifactId>
  <version>${project.version}</version>
</dependency>
```

When using this module in a WebFlux application, MDC properties like request ID, method, URL, etc. will not be available in log messages as Spring Boot 2.7 lacks the necessary reactive context propagation capabilities.

## Dependency

Add dependency

```xml
    <dependency>
      <groupId>org.geoserver.cloud</groupId>
      <artifactId>gs-cloud-starter-observability</artifactId>
      <version>${project.verison}</version>
    </dependency>
```

## Logging Features

### JSON-formatted Logging

The `net.logstash.logback:logstash-logback-encoder` dependency allows writing log entries as JSON-formatted objects in Logstash schema, which is ideal for
log aggregation systems like Elasticsearch, Logstash, and Kibana (ELK stack).

#### Enabling JSON Logging

To enable JSON logging, run the application with the `json-logs` Spring profile:

```bash
java -jar myapp.jar --spring.profiles.active=json-logs
```

Or in Docker/docker-compose:

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=json-logs
```

The JSON logging configuration is defined in [logback-spring.xml](src/main/resources/logback-spring.xml).

#### Example JSON Log Output

A basic HTTP request log entry with JSON formatting (without MDC properties) looks like:

```json
{
  "@timestamp": "2024-12-16T04:51:11.229-03:00",
  "@version": "1",
  "message": "POST 201 /geoserver/cloud/rest/workspaces",
  "logger_name": "org.geoserver.cloud.accesslog",
  "thread_name": "http-nio-9105-exec-2",
  "level": "INFO",
  "level_value": 20000
}
```

### Mapped Diagnostic Context (MDC)

The observability starter provides rich Mapped Diagnostic Context (MDC) support, making log entries more informative by including contextual
details about requests, authentication, and application information.

#### MDC Configuration Properties

All MDC properties can be enabled/disabled through configuration. Available categories include:

##### **HTTP Request Properties** (`logging.mdc.include.http`):
   - `id`: Request ID (ULID format)
   - `method`: HTTP method (GET, POST, etc.)
   - `url`: Request URL path
   - `query-string`: URL query parameters
   - `remote-addr`: Client IP address
   - `remote-host`: Client hostname
   - `session-id`: HTTP session ID
   - `headers`: HTTP request headers (filtered by pattern)
   - `headers-pattern`: Regex pattern for including headers
   - `cookies`: HTTP cookies
   - `parameters`: Request parameters

##### **Authentication Properties** (`logging.mdc.include.user`):
   - `id`: Authenticated username
   - `roles`: User roles

##### **Application Properties** (`logging.mdc.include.application`):
   - `name`: Application name
   - `version`: Application version
   - `instance-id`: Instance identifier
   - `active-profiles`: Active Spring profiles

##### **GeoServer Properties** (`logging.mdc.include.geoserver.ows`):
   - `service-name`: OWS service name (WMS, WFS, etc.)
   - `service-version`: Service version
   - `service-format`: Requested format
   - `operation-name`: Operation name (GetMap, GetFeature, etc.)

#### Example MDC Configuration

```yaml
logging:
  mdc:
    include:
      user:
        id: true
        roles: true
      application:
        name: true
        version: true
        instance-id: true
        active-profiles: true
      http:
        id: true
        method: true
        url: true
        query-string: false
        parameters: false
        headers: false
        headers-pattern: ".*"
        cookies: false
        remote-addr: true
        remote-host: false
        session-id: false
      geoserver:
        ows:
          service-name: true
          service-version: true
          service-format: true
          operation-name: true
```

### Access Logging

The observability starter includes an access logging system that logs incoming HTTP requests with configurable patterns and log levels.

#### Access Log Configuration

Access logging can be configured in your application properties:

```yaml
logging:
  # Control behavior of the org.geoserver.cloud.accesslog logging topic
  accesslog:
    enabled: true
    # Requests matching these patterns will be logged at INFO level
    info:
      - .*\/(rest|gwc\/rest)(\/.*|\?.*)?$
    # Requests matching these patterns will be logged at DEBUG level
    debug:
      - .*\/(ows|ogc|wms|wfs|wcs|wps)(\/.*|\?.*)?$
    # Requests matching these patterns will be logged at TRACE level
    trace:
      - ^(?!.*\/web\/wicket\/resource\/)(?!.*\.(png|jpg|jpeg|gif|svg|webp|ico)(\\?.*)?$).*$
```

The log format includes:
- HTTP method
- Status code
- Request URI path
- Any MDC properties configured (when using JSON logging)

#### Setting Log Levels

Control the access log verbosity with standard log levels:

```yaml
logging:
  level:
    org.geoserver.cloud.accesslog: INFO # Set to DEBUG or TRACE for more detail
```

### Combining JSON Logging with MDC and Access Logging

For maximum observability, combine JSON logging with MDC properties and access logging:

1. Enable the `json-logs` profile
2. Configure the MDC properties you want to include
3. Enable and configure the access log patterns
4. Set appropriate log levels

This combination provides rich, structured logs that can be easily parsed, filtered, and analyzed by log management systems.

#### Complete Example with MDC Properties

When JSON logging and MDC properties are enabled, a log entry looks like this:

```json
{
  "@timestamp": "2024-12-16T04:51:11.229-03:00",
  "@version": "1",
  "message": "POST 201 /geoserver/cloud/rest/workspaces",
  "logger_name": "org.geoserver.cloud.accesslog",
  "thread_name": "http-nio-9105-exec-2",
  "level": "INFO",
  "level_value": 20000,
  "enduser.authenticated": "true",
  "application.instance.id": "restconfig-v1:192.168.86.128:9105",
  "enduser.id": "admin",
  "http.request.method": "POST",
  "application.version": "1.10-SNAPSHOT",
  "http.request.id": "01jf9sjy4ndynkd2bq7g6qx6x7",
  "http.request.url": "/geoserver/cloud/rest/workspaces",
  "application.name": "restconfig-v1"
}
```

This rich structured format enables powerful filtering and analysis in log management systems. For example, you could easily:

- Filter logs by user (`enduser.id`)
- Track requests across services (`http.request.id`)
- Monitor specific endpoints (`http.request.url`)
- Group logs by application (`application.name`) 
- Track application versions (`application.version`)

For OWS requests, GeoServer-specific properties would also be included:

```json
{
  ...
  "gs.ows.service.name": "WMS",
  "gs.ows.service.version": "1.3.0",
  "gs.ows.service.operation": "GetMap",
  "gs.ows.service.format": "image/png"
}
```