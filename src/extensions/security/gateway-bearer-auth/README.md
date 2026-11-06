# GeoServer Cloud Gateway Bearer Authentication Extension

Validates OIDC JWT Bearer tokens at the GeoServer Cloud gateway and passes the verified identity to backend services via
the existing `SharedAuth` request headers. The raw token is not forwarded to backend services.

## Configuration

This extension is disabled by default. Enable it in the gateway configuration and provide the token issuer, audience,
username claim, role claim, and explicit role mappings:

```yaml
geoserver:
  extension:
    security:
      gateway-bearer-auth:
        enabled: true
        issuer-uri: https://keycloak.example/realms/geonode
        audience: geoserver-cloud
        username-claim: preferred_username
        roles-claim: resource_access.geoserver-cloud.roles
        role-prefix: ROLE_
        role-mappings:
          data-reader: PRIVATE_READER
          data-editor: PRIVATE_EDITOR
```

Only roles listed in `role-mappings` are accepted. Map them to roles assigned in GeoServer and used by
`security/layers.properties`. Disable the extension by setting `enabled: false`.
