# GeoServer Control Flow Extension

This module integrates the GeoServer Control Flow extension with GeoServer Cloud.

## Overview

The Control Flow extension allows administrators to control and throttle requests to manage server resources effectively. It helps with:

- **Performance**: Achieve optimal throughput by limiting concurrent requests to match available CPU cores
- **Resource Control**: Prevent OutOfMemoryErrors by controlling the number of parallel requests
- **Fairness**: Prevent a single user from overwhelming the server, ensuring equitable resource distribution

The control flow method queues excess requests rather than rejecting them, though it can be configured to reject requests that wait too long in the queue.

## Configuration

The extension is **enabled by default**. Configuration can be done in two ways:

### 1. Externalized Configuration (Default)

The recommended approach for GeoServer Cloud uses Spring Boot configuration properties with SpEL expression support:

```yaml
geoserver:
  extension:
    control-flow:
      enabled: true  # Enable/disable the extension (default: true)
      use-properties-file: false  # Use externalized config (default: false)
      properties:
        '[timeout]': 10  # Request timeout in seconds
        '[ows.global]': "${cpu.cores} * 2"  # Global OWS request limit
        '[ows.wms]': "${cpu.cores} * 4"  # WMS service limit
        '[ows.wms.getmap]': "${cpu.cores} * 2"  # GetMap request limit
```

The default configuration is provided in `config/geoserver_control_flow.yml`.

### 2. Data Directory Configuration

To use the traditional `control-flow.properties` file in the GeoServer data directory:

```yaml
geoserver:
  extension:
    control-flow:
      enabled: true
      use-properties-file: true
```

## Key Features

### Dynamic Configuration with SpEL

The externalized configuration supports Spring Expression Language (SpEL) for dynamic limits based on allocated CPU cores:

```yaml
properties:
  '[ows.global]': "${cpu.cores} * 2"  # Resolves to 2x the number of cores
```

The `cpu.cores` property is automatically available and reflects the container's allocated CPU resources.

### Request Control Rules

Control can be applied at different granularity levels:

```yaml
# Global OWS limit
'[ows.global]': 10

# Per-service limit
'[ows.wms]': 8
'[ows.wfs]': 6

# Per-request type
'[ows.wms.getmap]': 4
'[ows.wps.execute]': 2

# Per-output format
'[ows.wfs.getfeature.application/msexcel]': 2

# GeoWebCache services (WMS-C, TMS, WMTS)
'[ows.gwc]': 16
```

### User-Based Concurrency Control

Limit concurrent requests per user or IP address:

```yaml
# Cookie-based user identification
'[user]': 3

# IP-based identification
'[ip]': 6

# Specific IP address
'[ip.10.0.0.1]': 10

# IP blacklist
'[ip.blacklist]': "192.168.0.7, 192.168.0.8"
```

### Rate Control

Limit requests per time unit:

```yaml
# Rate limiting syntax: <requests>/<unit>[;<delay>s]
# Units: s (second), m (minute), h (hour), d (day)
'[user.ows.wms.getmap]': "30/s"
'[user.ows.wps.execute]': "1000/d;30s"
```

## Dependencies

This extension requires the following GeoServer dependency:

- `gs-control-flow`

## Implementation Details

### Key Classes

- `ControlFlowAutoConfiguration`: Main auto-configuration class
- `ControlFlowConfigurationProperties`: Configuration properties with SpEL expression support
- `PropertiesControlFlowConfigurator`: Configurator for externalized properties
- `ExpressionEvaluator`: Evaluates SpEL expressions and resolves placeholders
- `ConditionalOnControlFlow`: Composite conditional annotation for enabling the extension

### Configuration Modes

The extension operates in two mutually exclusive modes:

1. **UsingExternalizedConfiguration** (default): Uses `geoserver.extension.control-flow.properties` configuration with SpEL support
2. **UsingDataDirectoryConfiguration**: Uses traditional `control-flow.properties` file from the data directory

### Beans Registered

- `ControlFlowCallback`: Dispatcher callback that enforces flow control rules
- `ControlFlowConfigurator`: Reads and parses configuration
- `FlowControllerProvider`: Provides flow controllers based on configuration
- `IpBlacklistFilter`: Filters requests from blacklisted IP addresses
- `ControlModuleStatus`: Reports extension status

## Environment Variable Override

The extension supports a shorthand environment variable for quick enable/disable:

```bash
export CONTROL_FLOW=false
```

This works through the property placeholder: `${control-flow:true}`

## Related Documentation

- [GeoServer Control Flow User Guide](https://docs.geoserver.org/main/en/user/extensions/controlflow/index.html)
- Default configuration: `config/geoserver_control_flow.yml`
