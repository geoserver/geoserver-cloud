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

## When to Use Starters

As a user, you should generally depend on the starter modules rather than individual auto-configuration or implementation modules. Starters provide a simpler dependency model and ensure you get all the necessary components for a given feature.

## Learn More

For more information about Spring Boot starters, see:

- [Spring Boot Starters](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters)
- [Creating Your Own Starter](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.custom-starter)
- [List of Spring Boot Starter Modules](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters)