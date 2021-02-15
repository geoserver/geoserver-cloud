# Cloud Native GeoServer Discovery service

The Discovery Service maintains a registry of the location and health status of other services.

Any other service participating in the cluster will register to the Discovery Service at start up, and de-register
at graceful shut down time.

Inter-service communication will then be automatically load balanced to all available service instances of a given type.

The most common scenario is when for High Availability or performance reasons, there are several instances of a specific service,
hence incoming requests passing through the [Gateway Service](gateway-service.yml) get served by a different instance in a round-robin
fashion.

**Docker image**: `cloudnativegeoserver/gs-cloud-discovery-service`. 

**Service name**: `discovery-service`. 

This is the logical service name. 
Since we're using a ["discovery first bootstrap"](https://docs.spring.io/spring-cloud-config/docs/2.2.7.RELEASE/reference/html/#discovery-first-bootstrap) 
approach to service orchestration, other services won't use this service name to locate the discovery service, but a fixed list of service addresses need to be provided. See the "Client Configuration" section bellow for more details.

## Reference documentation

* Spring Cloud Netflix [reference](https://docs.spring.io/spring-cloud-netflix/docs/2.2.7.RELEASE/reference/html/)
* Discovery First Bootstrap [Config Client](https://docs.spring.io/spring-cloud-config/docs/2.2.7.RELEASE/reference/html/#discovery-first-bootstrap)

## Service Configuration


## Client Configuration


## Developing


In the default docker composition, there's only one instance of the discovery service. In a High Availability deployment scenario,
more instances can be launched, al

Since the discovery service are fixed entry points, we're setting up two peer aware eureka instances for HA.
  # Browse to http://localhost:8761 and http://localhost:8762 to verify they see each
  # other and all services are registered at both eureka instances.
  # See http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_peer_awareness


https://docs.spring.io/spring-cloud-netflix/docs/2.2.7.RELEASE/reference/html/

