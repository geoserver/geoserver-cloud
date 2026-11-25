# OpenID Connect login and Bearer tokens

GeoServer Cloud includes the GeoServer OpenID Connect extension. It signs users in to the web UI
with an OpenID Connect provider, such as Keycloak, Microsoft Entra ID, Google or GitHub. It also
authenticates API clients that send an access token in an `Authorization: Bearer` header.

This guide sets up one provider for both uses. The examples use Keycloak with a realm named
`geoserver`, and a deployment published at `https://maps.example.com/geoserver/cloud/`.

## How it works

The extension is enabled by default. Nobody authenticates through it until an administrator
creates an OpenID Connect filter and adds it to a filter chain.

Browser logins happen in the web UI. The gateway passes the user name and the roles of the session
to the other services, as it does after a form login.

API clients can send their token to any service. The service receiving the request validates the
token with the keys published by the provider. The gateway forwards the `Authorization` header
untouched.

The filter is saved with the rest of the security configuration, and the service instances read it
from there.

## Before you start

1. Register a confidential client with the provider, and note its client ID and secret.
2. Register two redirect URIs with the client. They start with the public URL of the gateway. For
   a filter named `openid-connect`, they are:
    - login: `https://maps.example.com/geoserver/cloud/web/login/oauth2/code/openid-connect__oidc`
    - after logout: `https://maps.example.com/geoserver/cloud/web/`

## Create the filter

1. Log in to the web UI as an administrator.
2. Go to **Security > Authentication**, and add an authentication filter of type
   **OpenID Connect Login**. Name it `openid-connect`.
3. Select the OpenID Connect provider, and enter the client ID and the client secret.
4. Enter the discovery document of the provider, and click **Discover** to fill in the endpoints.
   For Keycloak, the document is
   `https://keycloak.example.com/realms/geoserver/.well-known/openid-configuration`.
5. Check **Redirect Base URI**. The form fills it in with the public URL of the gateway,
   `https://maps.example.com/geoserver/cloud/`. The services read this URL from the
   `X-Forwarded-*` headers of the gateway, as they do for the links of the capabilities documents.
   If you opened the web UI without going through the gateway, enter the public URL by hand.
6. Choose where the roles come from. For the realm roles of Keycloak, select **Access Token** and
   enter `realm_access.roles` as the token roles claim.
7. Keep **Enable Resource Server (Bearer JWT)** checked to accept Bearer tokens.
8. Save.

The role names are the values of the claim. A role converter map can translate them to GeoServer
roles, for example `wms-reader=ROLE_WMS_READER`.

The [GeoServer documentation](https://docs.geoserver.org/3.0.x/en/user/extensions/oidc/) describes
the other options of the filter and the setup of each provider.

## Accept Bearer tokens in the services

The services read the tokens through the `default` filter chain:

1. Go to **Security > Authentication**, and open the `default` filter chain.
2. Add `openid-connect` before `anonymous`, and save.
3. Grant access to the roles of the tokens, for example with a service rule under
   **Security > Services**.

An API client gets a token from the provider and sends it with each request:

```bash
TOKEN=$(curl -s https://keycloak.example.com/realms/geoserver/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=my-client -d client_secret=my-secret \
  | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" \
  "https://maps.example.com/geoserver/cloud/wms?service=WMS&request=GetCapabilities"
```

A request with a missing, expired or forged token answers 401 when the resource requires a login.
A valid token without the required role answers 403. The service keeps no session for a Bearer
request.

A token for the user `admin` gets no roles, unless the filter has
**Allow the provider to log in the built-in "admin" account** checked.

## Log in to the web UI

1. Go to **Security > Authentication**, and open the `web` filter chain.
2. Add `openid-connect` before `anonymous`, and save.

The login menu of the web UI then shows a button for the provider. The button starts the login at
the provider, and the provider sends the user back to the login redirect URI. Both paths are under
`/web/`: the `web` chain covers them, and the default route of the gateway sends them to the web UI.

## Troubleshoot

Select the `OIDC_LOGGING` [logging profile](logging.md#select-a-logging-profile) to log each step
of a login or of a Bearer token validation, on all running services. The
**Log Sensitive Information (do not use in production)** option of the filter also logs the
tokens.

The GeoServer documentation lists
[troubleshooting tips and common mistakes](https://docs.geoserver.org/3.0.x/en/user/extensions/oidc/advanced/).
Its advice about `PROXY_BASE_URL` does not apply to GeoServer Cloud: check the
**Redirect Base URI** of the filter instead.

## Limitations

- The web UI keeps the login in the session of the instance that handled it. With several web UI
  replicas, configure sticky sessions in the ingress.
- The provider settings are entered in the web UI. Configuration properties cannot set them yet.

## Turn the extension off

Set `geoserver.extension.security.oidc.enabled=false`. With the default configuration files, the
`GEOSERVER_SECURITY_OIDC_ENABLED=false` environment variable does the same.
