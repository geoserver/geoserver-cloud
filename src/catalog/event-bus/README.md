# Catalog event bus

Sends the GeoServer events of a service instance to the other instances through
[Spring Cloud Bus](https://docs.spring.io/spring-cloud-bus/reference/), and publishes the events received from them to
the local `ApplicationContext`.

The event model lives in the [events](../events/README.md) module, without a dependency on Spring Cloud Bus. Modules
listening to the events depend on that module only.

## How it works

- `RemoteGeoServerEventBridge` wraps each local `GeoServerEvent` in a `RemoteGeoServerEvent` and publishes it to the
  bus. Spring Cloud Bus encodes it as JSON with the `gs-jackson-bindings` module.
- The secrets of stores, such as database passwords, are encrypted in the events sent and decrypted in the events
  received (`RemoteEventSecrets`), with the password encrypter of the security settings and the keys of the keystore
  shared by the services.
- The bridge unwraps the incoming `RemoteGeoServerEvent`s and publishes their `GeoServerEvent` locally, marked as remote
  (`GeoServerEvent.isRemote()`). The events sent by the instance itself are ignored.
- With RabbitMQ as transport, `RemoteEventsResumedPublisher` publishes a local `RemoteEventsResumed` event when the
  consumer subscribes to its queue, at startup or after a lost connection. Listeners holding state derived from the
  events, such as caches, empty or rebuild it.

## Configuration

The integration is active when Spring Cloud Bus is enabled (`spring.cloud.bus.enabled`, `true` by default) and
`geoserver.bus.enabled` is `true` (the default). The transport is configured as usual for Spring Cloud Bus, for
example:

```yaml
spring:
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: guest
geoserver:
  bus:
    enabled: true
```
