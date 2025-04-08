# GeoServer Environment Admin Authentication Extension

This extension provides an authentication mechanism that allows setting the GeoServer admin credentials through environment variables or configuration properties.

## Overview

The `EnvironmentAdminAuthenticationProvider` authenticates users with the username and password specified in the configuration properties:

- `geoserver.admin.username`: The admin username
- `geoserver.admin.password`: The admin password

This is particularly useful in containerized environments like Kubernetes where you can set these credentials as environment variables or configuration properties, rather than modifying the XML configuration files.

## Key Features

- Allows setting admin credentials through environment variables or configuration properties
- Takes precedence over other authentication providers
- Can disable the default "admin" user when a different admin username is provided
- Breaks the authentication chain on failed attempts to prevent fallback to other providers

## Configuration

### Properties

The extension can be enabled or disabled using the following property:

```yaml
geoserver:
  extension:
    security:
      environment-admin:
        enabled: true  # default is true
```

### Providing Admin Credentials

The admin credentials can be provided in two ways:

#### 1. Environment Variables (Recommended)

Setting credentials through environment variables is the recommended approach for production environments:

```bash
# Linux/macOS
export GEOSERVER_ADMIN_USERNAME=admin
export GEOSERVER_ADMIN_PASSWORD=mysecretpassword

# Windows
set GEOSERVER_ADMIN_USERNAME=admin
set GEOSERVER_ADMIN_PASSWORD=mysecretpassword
```

#### 2. Spring Properties

Alternatively, you can set them in configuration properties:

```yaml
geoserver:
  admin:
    username: admin      # Admin username
    password: geoserver  # Admin password
```

### Requirements

- Both username and password must be provided for the authentication to be enabled
- If only one is provided, the application context will fail to start
- If neither is provided, the authentication provider will be inactive (not disabled)

## Usage in Kubernetes

This extension is particularly useful in Kubernetes environments. It allows you to securely provide admin credentials through environment variables:

```yaml
# Store credentials in a Kubernetes Secret
apiVersion: v1
kind: Secret
metadata:
  name: geoserver-admin-credentials
type: Opaque
data:
  username: YWRtaW4=  # "admin" in base64
  password: c2VjcmV0  # "secret" in base64
---
# Mount the environment variables from the Secret in your Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: geoserver
spec:
  template:
    spec:
      containers:
      - name: geoserver
        # Pass the environment variables to the container
        # These are the exact environment variables the extension looks for
        env:
        - name: GEOSERVER_ADMIN_USERNAME  # Required environment variable for username
          valueFrom:
            secretKeyRef:
              name: geoserver-admin-credentials
              key: username
        - name: GEOSERVER_ADMIN_PASSWORD  # Required environment variable for password
          valueFrom:
            secretKeyRef:
              name: geoserver-admin-credentials
              key: password
```

The extension will automatically pick up these environment variables and use them for authentication, without requiring any changes to the GeoServer configuration files.