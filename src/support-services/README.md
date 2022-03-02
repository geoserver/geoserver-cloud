# Cloud enabling support services

These are microservices that enable the configuration and orchestration of the business services such as wfs, wms, etc.

We're setting up a **discovery-first** architecture, where the first point of contact for all microservices is the "discovery" service.

On bootstrap, all services first connect to the discovery service, looks up the config service, connect to the config service and load their configuration properties.

The config service serves as a centralized, possibly versioned (as a git repository), provider of configuration properties.

The api-gateway service acts as a single entry point for all microservices, dispatching requests to the appropriate service based on the configured rules, and doing load balancing among service instances.