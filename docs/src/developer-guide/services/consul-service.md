# Consul Service Discovery

Consul maintains a registry of service locations and health status.

Services register with Consul at startup. 
They deregister during graceful shutdown.

Inter-service communication is automatically load balanced across available instances.

**Docker image**: `hashicorp/consul:1.22.5`.

**Service name**: `consul`.

Consul is used only for Docker Compose deployments.
Kubernetes deployments use native Kubernetes Services instead.

## Service Configuration

The Consul agent runs in server mode with a single-node bootstrap configuration in development.

Browse to [http://localhost:8500](http://localhost:8500) to check service health and registration.

## Client Configuration

Services use the `discovery-consul` Spring profile.
This profile is included in the `config-first` profile group.

Configuration defaults:
- **Host**: `consul`
- **Port**: `8500`
- **Health Check**: `/actuator/health` on port `8081`

Services prefer IP addresses for registration to ensure connectivity in Docker networks.

## Reference documentation

* [Consul Documentation](https://www.consul.io/docs)
* [Spring Cloud Consul](https://docs.spring.io/spring-cloud-consul/docs/current/reference/html/)
