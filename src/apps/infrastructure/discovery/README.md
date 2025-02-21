# Cloud Native GeoServer Discovery service

The Discovery Service maintains a registry of the location and health status of other services.

Any other service participating in the cluster will register to the Discovery Service at start up, and de-register
at graceful shut down time.

Inter-service communication will then be automatically load balanced to all available service instances of a given type.

The most common scenario is when for High Availability or performance reasons, there are several instances of a specific service,
hence incoming requests passing through the [Gateway Service](src/main/resources/application.yml) get served by a different instance in a round-robin
fashion.

Follow the service [documentation](/docs/develop/services/discovery-service.md) and keep it up to date.
