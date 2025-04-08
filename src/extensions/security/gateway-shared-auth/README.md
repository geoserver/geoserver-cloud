# Gateway Shared Authentication Extension

Auto-configuration for GeoServer Cloud Gateway Shared Authentication extension.

## Overview

The Gateway Shared Authentication extension enables a coordinated authentication mechanism across GeoServer Cloud microservices, allowing users to log in once through the WebUI and have their authentication propagated to all other services.

> **IMPORTANT: Package Stability Notice**
> 
> The following package structures must remain stable as they are used by GeoServer's XStream serialization:
> - `org.geoserver.cloud.security.gateway`
> - `org.geoserver.cloud.security.gateway.sharedauth`
>
> Changing these package names would break serialization compatibility for the following classes:
> - `GatewaySharedAuthenticationProvider`
> - `GatewaySharedAuthenticationFilter.Config`
> - `GatewayPreAuthenticationFilterConfig`

## Features

This extension:
- Enables sharing authentication between GeoServer microservices through the gateway
- Provides two modes of operation: server (for WebUI service) and client (for all other services)
- Manages authentication token headers between services
- Automatically configures GeoServer security filter chains
- Preserves session state across microservices using gateway session ID tracking
- Handles both authentication (username) and authorization (roles) information

## How It Works

The extension automatically determines whether to operate in server mode or client mode based on the application type:

1. **Server Mode (WebUI)**: 
   - Automatically activated when running in the GeoServer WebUI service
   - When a user authenticates to the WebUI service, the filter adds special headers to the response
   - These headers contain the authenticated username and roles
   - The API Gateway captures these headers and associates them with the user's session

2. **Client Mode (Other Services)**:
   - Automatically activated for all services except the WebUI
   - When authenticated users make requests to other services through the gateway
   - The gateway attaches the stored authentication headers to these requests
   - The filter in client services reads these headers and authenticates the user seamlessly

3. **Session Tracking**:
   - The gateway session cookie ("SESSION") is used to correlate requests across services
   - This allows maintaining consistent authentication state for users

## Configuration

The extension can be configured using the following properties:

```yaml
geoserver:
  extension:
    security:
      gateway-shared-auth:
        enabled: true  # Enable/disable Gateway Shared Auth (default: true)
        auto: true     # Automatically configure auth filter chains (default: true)
```

Backward compatibility is maintained with legacy properties:
```yaml
geoserver:
  security:
    gateway-shared-auth:
      enabled: true
      auto: true
```

## Implementation Details

The extension uses Spring Boot auto-configuration to set up the shared authentication mechanism:

- **Key Classes**:
  - `GatewaySharedAuthenticationFilter`: The main filter that handles authentication header processing
  - `GatewaySharedAuthenticationProvider`: Provider that creates the filter based on mode
  - `GatewaySharedAuthenticationInitializer`: Sets up security filter chains
  - `GatewaySharedAuthAutoConfiguration`: Spring Boot auto-configuration entry point
  - `GatewaySharedAuthConfigProperties`: Configuration properties holder

- **Operating Modes** (automatically selected based on application type):
  - `ServerFilter`: Generates authentication headers for responses (used automatically in WebUI)
  - `ClientFilter`: Processes authentication headers from requests (used automatically in other services)
  - `DisabledFilter`: No-op implementation for when the feature is disabled

- **Authentication Headers**:
  - `x-gsc-username`: Contains the authenticated username
  - `x-gsc-roles`: Contains the user's roles (can be multi-valued)

## Usage

Simply enable the Gateway Shared Authentication extension in all services:

```yaml
geoserver:
  extension:
    security:
      gateway-shared-auth:
        enabled: true
```

The extension will automatically:
- Operate in server mode when running in the WebUI service
- Operate in client mode when running in any other service

No additional configuration is needed to specify the operation mode, as it's automatically determined based on the application type.

### Gateway Configuration

The API Gateway must be configured to:
- Forward the authentication headers between services
- Maintain session state for users
- Remove these headers from external requests (security)

A typical gateway configuration includes:
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - SaveSession
      routes:
        - id: webui
          # ... route configuration ...
          filters:
            - name: GatewaySharedAuth
              args:
                server: true
        - id: wms
          # ... route configuration ...
          filters:
            - name: GatewaySharedAuth
              args:
                server: false
```

## Security Considerations

- The authentication headers are intended for **internal use only** within the GeoServer Cloud ecosystem
- The API Gateway should be configured to strip these headers from external requests
- Consider enabling HTTPS for all services to prevent header interception
- The gateway shared authentication relies on the security of the gateway session management

## Troubleshooting

Common issues and their solutions:

1. **Authentication not propagating:**
   - Verify the gateway is correctly forwarding the headers
   - Check that session cookies are being preserved across requests
   - Enable debug logging for the package `org.geoserver.cloud.security.gateway.sharedauth`

2. **Filter not functioning:**
   - Ensure the `enabled` property is set to `true`
   - Verify the filter chain is correctly configured (check `auto` property)
   - Check for error messages during service startup

## Logging

To enable debug logging for this extension:

```yaml
logging:
  level:
    org.geoserver.cloud.security.gateway.sharedauth: DEBUG
```

This will show detailed information about header processing and authentication state changes.