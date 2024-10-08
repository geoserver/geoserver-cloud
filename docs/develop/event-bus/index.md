# Understanding the event bus data flow

For general information about the event bus, hava a look [here](../../../src/catalog/event-bus/README.md).

The following diagram demonstrates the data flow on the event bus:

![Event Bus Data flow](../../img/gs_cloud_eventbus_diagram.svg)

1. Changes on the catalog/config level are usually done via the REST interface or the WebUI (via the Gateway)
2. Changes are persisted in the catalog/config
3. The `CatalogApplicationEventPublisher` listens to the events of the (native) GeoServer/Catalog (triggered by step 2)
4. Whenever such an event fires, the `CatalogApplicationEventPublisher` will publish a "local" `GeoServerEvent`. Have a look [here](../../../src/catalog/events/README.md) for the full type hierarchy.
5. The `RemoteGeoServerEventBridge` (listens to these `GeoServerEvent`s and) broadcasts `RemoteGeoServerEvent`s to the event bus.
6. All registered microservices listen for incoming `RemoteGeoServerEvent`s
7. The payload of these remote events will be published as local events to reload/refresh the catalog/config locally.
