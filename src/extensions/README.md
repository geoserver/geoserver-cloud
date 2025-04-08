# Spring Boot Auto-configuration Modules

This directory contains Spring Boot auto-configuration modules for GeoServer Cloud extensions.

## What are Auto-configuration Modules?

Spring Boot auto-configuration modules attempt to automatically configure your Spring application based on the jar dependencies added to your project. These modules contain auto-configuration classes that apply configuration when certain conditions are met (usually when specific classes are present on the classpath).

Auto-configuration modules:
- Define configuration classes annotated with `@Configuration`
- Register these configurations in `META-INF/spring.factories` (Spring Boot 2.x) or `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 3.x)
- Apply configuration conditionally using `@ConditionalOnClass`, `@ConditionalOnMissingBean`, etc.
- Focus solely on providing auto-configurations, without including dependencies

## Auto-configuration vs. Starter Modules

Auto-configuration modules and starter modules serve different purposes in the Spring Boot ecosystem:

| Auto-configuration Modules | Starter Modules |
|----------------------------|-----------------|
| Focus on providing auto-configuration | Package dependencies and auto-configurations together |
| Do not include dependencies | Provide transitive dependencies for a feature |
| Typically named with `-autoconfigure` suffix | Typically named with `-starter` suffix |
| Can be used directly, but usually imported by starters | Primary point of dependency for users |

A starter module generally depends on one or more auto-configuration modules and adds the required dependencies.

## Learn More

For more information about Spring Boot auto-configuration and starters, see:

- [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.auto-configuration)
- [Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Creating Your Own Starter](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.custom-starter)
