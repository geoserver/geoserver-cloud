# OIDC Extension: Architecture and Design Notes

This document explains **why** this extension is built the way it is: what the
upstream GeoServer OIDC module contributes, why GeoServer Cloud
reuses it instead of switching to Spring Boot's native OAuth2 client
auto-configuration, and how we nevertheless intend to deliver a Cloud-native,
DevOps-friendly configuration on top.

For operator-facing documentation (how to enable and configure OIDC) see
[README.md](./README.md). This file is for contributors and architects.

## How the upstream module works

The upstream OIDC module (`gs-sec-oidc-core`) does not reimplement OAuth2 /
OpenID Connect: Spring Security 7 already ships a complete implementation. What the module
provides is a **bridge** between Spring Security's OAuth2 machinery and
GeoServer's own security ecosystem. That ecosystem has several abstractions
that Spring Security doesn't know about:

- **`GeoServerSecurityManager`**: GeoServer's root security service, in
  practice used as the `AuthenticationManager`. It owns the persistence and
  lifecycle of authentication providers, filter configs, role services, and
  user-group services.
- **`GeoServerSecurityFilterChainProxy`**: a distinct filter proxy that
  Spring aliases as `springSecurityFilterChain`. Operators define named URL
  patterns ("chains") and compose them out of `GeoServerAuthenticationFilter`
  instances. This is separate from a standard Spring Security
  `SecurityFilterChain` bean.
- **`GeoServerRole`, `GeoServerRoleService`, `GeoServerUserGroupService`**:
  GeoServer's identity model. Authorization (data-access rules, workspace
  isolation, layer-group containment) is expressed against `GeoServerRole`s,
  not `GrantedAuthority`s.
- **XStream-persisted filter config**: every security filter's settings live
  in the data directory under `security/filter/<name>/config.xml`. This is
  what the Wicket admin UI reads and writes; it is the primary config plane
  for runtime reconfiguration.
- **Wicket admin UI extension points**: providers contribute
  `AuthenticationFilterPanelInfo` beans and `LoginFormInfo` buttons that show
  up in the admin pages and on the home page.

Against that backdrop, the module contributes roughly four things:

1. **`GeoServerOAuth2LoginAuthenticationProvider`**: an
   `AbstractFilterProvider` implementation discoverable by
   `GeoServerSecurityManager`. It knows how to serialize and validate
   `GeoServerOAuth2LoginFilterConfig`, create filters from it, and register an
   XStream alias so the config round-trips.

2. **`GeoServerOAuth2LoginAuthenticationFilter` + its builder**: a composite
   `GeoServerAuthenticationFilter` that wraps the real Spring Security OAuth2
   filters (`OAuth2AuthorizationRequestRedirectFilter`,
   `OAuth2LoginAuthenticationFilter`, `RequestCacheAwareFilter`,
   `BearerTokenAuthenticationFilter`). The builder uses Spring's `HttpSecurity`
   DSL internally (via the prototype `HttpSecurity` bean exposed by
   `HttpSecurityConfiguration`) to materialize a proper Spring Security filter
   chain, then extracts the filters it needs and installs them inside the
   GeoServer chain. This is the central adapter: it preserves Spring
   Security's correctness while making the filters behave like GeoServer
   filters. The wrapped `BearerTokenAuthenticationFilter` is the filter's
   resource-server mode: with "Enable Resource Server (Bearer JWT)" on, the
   upstream default, the same filter validates `Authorization: Bearer <JWT>`
   requests from API clients with the provider configuration of the filter
   and resolves their roles with the same role source. The separate
   resource-server provider bean in the upstream XML is unfinished and stays
   commented out, upstream and here.

3. **`GeoServerOAuth2RoleResolver` and friends**: the role-extraction logic.
   Four role sources (`IdToken`, `AccessToken`, `UserInfo`, `MSGraphAPI`),
   claim-path parsing, role converter strings, optional restriction to
   "listed" roles, etc. The result is a set of `GeoServerRole`s that flow
   into the standard role service, which is what GeoServer's authorization
   layer consumes.

4. **Wicket UI**: `OAuth2LoginAuthProviderPanel` (the admin configuration
   panel, including the discovery-URL "Discover" button, per-provider
   sub-panels, and role-source pickers) plus the four `LoginFormInfo` beans
   that render "Log in with Google / GitHub / Microsoft / OIDC" buttons on
   the home page, coordinated by `OAuth2LoginButtonManager`.

The upshot: the module is not a reimplementation, it is an **integration
layer** that plugs Spring Security's OAuth2 flow into GeoServer's security
model without forcing GeoServer to adopt Spring Security's top-level shape.

## Why not use Spring Boot's native OAuth2 client auto-configuration

Spring Boot's native OAuth2 support is attractive for Cloud because it is
config-driven and 12-factor-shaped:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          my-idp:
            client-id: ${OIDC_CLIENT_ID}
            client-secret: ${OIDC_CLIENT_SECRET}
            scope: openid, profile, email
        provider:
          my-idp:
            issuer-uri: https://idp.example.com
```

That is a better configuration experience than clicking through Wicket panels.
It is, however, **not a drop-in replacement** for the upstream module because
it solves only the part of the problem Spring Security already solved. The
integration layer is still missing, and rebuilding it would amount to:

- **Role mapping.** `OAuth2User.getAuthorities()` returns `SCOPE_*`
  `GrantedAuthority`s derived from the granted scopes, not the
  `GeoServerRole`s that GeoServer's authorization layer actually consumes.
  We'd need to reimplement the four role-source strategies (IdToken,
  AccessToken, UserInfo, MSGraph) that `GeoServerOAuth2RoleResolver` already
  provides, including the claim-path parsing, role-converter string format,
  and the quirks (nested paths, `scope` vs `authorities` handling, the MS
  Graph `memberOf` / appRoleAssignments handling).

- **Two filter chains, one proxy name.** Spring Boot's security
  auto-configuration registers a `springSecurityFilterChain`, but GeoServer
  already aliases its own `GeoServerSecurityFilterChainProxy` to that name.
  That is exactly why Cloud excludes `SecurityAutoConfiguration` globally in
  `starters/spring-boot/src/main/resources/geoserver_spring_boot_profiles.yml`.
  Re-enabling Spring Security's auto-config just for OIDC means either
  reconciling the two (writing the adapter we are trying to avoid) or
  accepting that OIDC runs in a separate filter chain that doesn't cooperate
  with any other GeoServer authentication method (form login, basic auth,
  auth-key, LDAP, JDBC, gateway-shared-auth).

- **Authentication token translation.** `OAuth2AuthenticationToken` isn't the
  shape GeoServer's authorization machinery (`DataAccessManager`,
  `LayerGroupContainmentCache`, rule DAOs) expects. You need to convert to
  whatever `GeoServerSecurityManager` hands out, which is where the
  upstream module's existing adapter does its job.

- **Wicket login page integration.** The "Login with Google" buttons on the
  home page come from the `LoginFormInfo` / `OAuth2LoginButtonManager`
  plumbing. Spring Security's default `/login` page is a different thing
  entirely; hiding GeoServer's login page to use it would break every other
  authentication method.

- **Runtime reconfiguration.** Spring Boot's `ClientRegistrationRepository`
  is normally an immutable singleton bound to `application.yml` at startup.
  Operators who change the IdP while the service is running need this to
  refresh, which means rebuilding the Spring Security filter chain on
  refresh. The upstream module already does a form of this, because its
  builder is `@Scope("prototype")` and is invoked every time
  `GeoServerSecurityManager` reloads the filter config.

- **JWT decoder escape hatches.** Real-world identity providers demand
  options that Spring Security exposes only via custom `JwtDecoder`s: the
  "don't validate signature" flag, algorithm overrides (MAC vs RSA families),
  custom JWKS resolution. `GeoServerOidcIdTokenDecoderFactory` encodes these
  in a way that survives XStream round-trips.

In short: **native Spring Boot covers ~20% of what's needed and leaves the
other 80% (role service integration, filter-chain coexistence, authorization
bridge, admin UI, runtime reconfiguration, IdP-quirk tolerance) to us.** The
upstream module is that 80%. Replacing it with a hand-rolled equivalent
means owning ~2-3k LOC of bridge code forever, losing upstream's test
coverage and bug fixes, and diverging from the rest of the GeoServer
ecosystem.

## The Cloud-native overlay

Not using Spring Boot's native OIDC does not mean giving up on
DevOps-friendly configuration. The upstream module's architecture is
actually a natural fit for a layered approach:

- **Primary state** stays in `GeoServerOAuth2LoginFilterConfig`, persisted by
  XStream in the data directory. The runtime (filter builder, role
  resolver, admin UI) reads from there. This is how every other GeoServer
  security filter works, and it's what enables the rest of the ecosystem
  (filter chains, role services, admin UI) to participate.

- **Source of truth can be externalized** as long as it flows into that
  primary state. That is the seam we add.

The extension has a Spring Boot configuration-properties class,
`OidcConfigProperties`, with the `enabled` flag as its only setting. A
follow-up adds the provider settings to it:

```yaml
geoserver:
  extension:
    security:
      oidc:
        enabled: true
        # properties-driven: when true, the provider config below is the
        # source of truth; admin UI becomes read-only. When false, only the
        # admin UI drives config (current behaviour).
        properties-driven: true
        providers:
          corporate-idp:
            issuer-uri: https://idp.example.com
            client-id: ${OIDC_CLIENT_ID}
            client-secret: ${OIDC_CLIENT_SECRET}
            scopes: openid, profile, email, groups
            role-source: IdToken
            role-claim: groups
            role-converter: "mapper;ADMIN=ROLE_ADMINISTRATOR;USER=ROLE_AUTHENTICATED"
```

A listener scoped to the Spring Cloud Bus refresh event maps the bound
properties to `GeoServerOAuth2LoginFilterConfig` and calls
`GeoServerSecurityManager.saveFilter(config)`, the entry point used by the
Wicket admin UI. The filter builder is already `@Scope("prototype")`
and rebuilds the internal Spring Security filter chain every time the config
is saved, so runtime reconfiguration works with zero additional code.

What this gets us:

- **Cloud-native configuration.** `application.yml`, env vars, Spring Cloud
  Config Server, Kubernetes ConfigMaps, all the standard 12-factor patterns
  apply.
- **Refresh at runtime.** `@RefreshScope` + Spring Cloud Bus propagates the
  change; the next save rebuilds the filter chain across the fleet.
- **No rewrite.** All the role-extraction, filter-chain integration, and
  authorization plumbing keeps working because the XStream config is still
  the runtime source of truth; the only new code is the property-to-config
  mapper (~300-500 LOC plus tests).
- **Opt-in.** When `properties-driven: false` (the default), behavior is
  unchanged from today: operators configure OIDC through the admin UI, and
  the extension is a straightforward port of the upstream module. When
  enabled, the admin UI should signal that fields are sourced from
  application properties (display-only) to avoid drift.

That layered design is the pragmatic reconciliation between Cloud's
opinionated, config-driven nature and GeoServer's own, deeply integrated
security model. It gets you the DevOps ergonomics without paying for a
parallel security stack or a forked bridge layer.

## Related follow-ups

- **Session sharing across webui replicas.** `HttpSessionSecurityContextRepository`
  ties an OIDC session to the pod that accepted the callback. Cross-pod
  identity propagation to wms/wfs/... already works through
  `gateway-shared-auth`, but the webui itself needs sticky sessions at the
  ingress (or a DB/Redis-backed `SecurityContextRepository`) for horizontal
  scaling. The clean fix is an upstream PR that makes
  `GeoServerOAuth2LoginAuthenticationFilterBuilder#sessionRepo` injectable,
  followed by a Cloud-side `SecurityContextRepository` implementation over
  the shared catalog store or a Redis bus.
