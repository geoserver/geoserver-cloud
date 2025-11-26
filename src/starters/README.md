# Spring Boot Starter Modules

This directory contains Spring Boot starter modules for GeoServer Cloud features and extensions.

## What are Starter Modules?

Spring Boot starter modules are convenient dependency descriptors that provide everything needed to get started with a particular technology or feature. Starters are designed to simplify dependency management for developers by providing a single dependency to include rather than having to specify multiple dependencies separately.

Starter modules:
- Package auto-configuration, dependencies, and other resources together
- Provide transitive dependencies necessary for a specific feature to work
- Typically named with `-starter` suffix
- Serve as the primary point of dependency for users

## Starter vs. Auto-configuration Modules

Starter modules and auto-configuration modules serve complementary purposes in the Spring Boot ecosystem:

| Starter Modules | Auto-configuration Modules |
|-----------------|----------------------------|
| Package dependencies and auto-configurations together | Focus on providing auto-configuration |
| Provide transitive dependencies for a feature | Do not include dependencies |
| Primary point of dependency for users | Can be used directly, but usually imported by starters |
| Typically named with `-starter` suffix | Typically named with `-autoconfigure` suffix |

A well-designed starter module:
- Declares appropriate dependencies for the feature
- Depends on the appropriate auto-configuration module(s)
- Might include typical application properties, templates, or static resources
- Does not contain code other than minimal configuration glue

## GeoServer Cloud Starter Modules and Dependencies

GeoServer Cloud provides various starter modules with specific dependency relationships.

### Individual Starter Module Trees

```
# Starter dependencies

## gs-cloud-starter-webmvc dependencies (web mvc capabilities only)
gs-cloud-starter-webmvc
├── gs-cloud-spring-boot-starter
├── gs-cloud-starter-catalog-backend
└── gs-cloud-starter-data-formats

## gs-cloud-spring-cloud-starter dependencies
gs-cloud-spring-cloud-starter
├── Spring Cloud dependencies (eureka, config client, etc.)
└── gs-cloud-catalog-event-bus (bridges catalog events to Spring Cloud Bus)

## gs-cloud-starter-extensions dependencies
gs-cloud-starter-extensions
├── gs-cloud-starter-output-formats
│   └── (output format extensions)
├── gs-cloud-starter-security
│   └── (auth-key, ldap, jdbc, environment-admin, etc.)
└── (css-styling, mapbox-styling, app-schema, etc.)

## gs-cloud-starter-output-formats dependencies
gs-cloud-starter-output-formats
└── (output format extensions)

# Independent starters (no starter dependencies)
gs-cloud-spring-boot-starter
gs-cloud-starter-catalog-backend
gs-cloud-starter-data-formats
gs-cloud-starter-security
gs-cloud-starter-observability
gs-cloud-spring-cloud-starter
```

This structure allows for selective inclusion of functionality based on service type:

**For Infrastructure Services** (discovery, config, admin):
- `gs-cloud-spring-boot-starter` - Basic Spring Boot integration
- `gs-cloud-starter-observability` - Monitoring and metrics (optional)

**For GeoServer Services** (wms, wfs, wcs, etc.):
- `gs-cloud-spring-boot-starter` - Base Spring Boot integration
- `gs-cloud-spring-cloud-starter` - Spring Cloud capabilities and event bus
- `gs-cloud-starter-webmvc` - Web MVC functionality (includes dependencies on catalog and data formats)
- `gs-cloud-starter-extensions` - All GeoServer extensions (includes security, output formats, etc.)

**For Specific Requirements**:
- `gs-cloud-starter-security` - Security extensions only
- `gs-cloud-starter-output-formats` - Output format extensions only
- Other specialized starters as needed

## Typical GeoServer Microservice Dependencies

A typical GeoServer microservice needs these key starters:

1. **`gs-cloud-spring-boot-starter`**: Base Spring Boot integration that provides:
   - Base Spring Boot functionality
   - Common configuration handling
   - Service ID filtering

2. **`gs-cloud-spring-cloud-starter`**: Provides Spring Cloud capabilities:
   - Service discovery (Eureka client)
   - Configuration management (Config client)
   - Event bus integration
   - Catalog events bridging to Spring Cloud Bus

3. **`gs-cloud-starter-webmvc`**: Provides core web MVC functionality:
   - Spring Boot web features
   - GeoServer catalog backend configuration
   - Data format support
   - Web related auto-configuration

4. **`gs-cloud-starter-extensions`**: Includes all supported extensions:
   - Security extensions (LDAP, JDBC, Auth Key, Environment Admin, etc.)
   - Styling extensions (CSS, MapBox)
   - Output formats (Vector Tiles and other formats)
   - Other feature extensions

The parent POM for GeoServer services (`src/apps/geoserver/pom.xml`) already includes these starters, so individual service modules don't need to explicitly declare them.

## When to Use Starters

As a user, you should generally depend on the starter modules rather than individual auto-configuration or implementation modules. Starters provide a simpler dependency model and ensure you get all the necessary components for a given feature.

## Learn More

For more information about Spring Boot starters, see:

- [Spring Boot Starters](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters)
- [Creating Your Own Starter](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.custom-starter)
- [List of Spring Boot Starter Modules](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters)
