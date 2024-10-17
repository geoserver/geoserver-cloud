# Catalog Events Module

Implements an application-level catalog/config events object model
and standard event propagation mechanism through the ApplicationContext.

The event object hierarchy has a notion of whether the event was generated
by the running service instance or a remote one, but is not tied to any
distributed event delivery mechanism. That responsibility is left to
an integration architectural layer.

Standard Spring `@EventListeners` can hence be used to implement
orthogonal concerns. Care must be taken to filter out events
by `InfoEvent.isLocal()` and `InfoEvent.isRemote()` as appropriate,
if relevant to the listener.

A spring-boot `AutoConfiguration` is provided to enable or disable
the usage of these application-level level events, through
the `geoserver.catalog.events.enabled` boolean configuration property.

As a convenience, the `@ConditionalOnCatalogEvents` annotation
can be used to enable additional functionality on any downstream
dependency.


```mermaid
classDiagram
    direction LR
    GeoServerEvent <|-- UpdateSequenceEvent
    GeoServerEvent <|-- LifecycleEvent
    LifecycleEvent <|-- ReloadEvent
    LifecycleEvent <|-- ResetEvent
    UpdateSequenceEvent <|-- InfoEvent
    UpdateSequenceEvent <|-- SecurityConfigChanged
    InfoEvent <|-- InfoAdded
    InfoEvent <|-- InfoModified
    InfoEvent <|-- InfoRemoved
    InfoEvent o-- ConfigInfoType
    InfoAdded <|-- CatalogInfoAdded
    InfoAdded <|-- ConfigInfoAdded
    ConfigInfoAdded <|-- GeoServerInfoSet
    ConfigInfoAdded <|-- LoggingInfoSet
    ConfigInfoAdded <|-- ServiceAdded
    ConfigInfoAdded <|-- SettingsAdded
    InfoModified <|-- CatalogInfoModified
    InfoModified <|-- ConfigInfoModified
    CatalogInfoModified <|-- DefaultNamespaceSet
    CatalogInfoModified <|-- DefaultWorkspaceSet
    CatalogInfoModified <|-- DefaultDataStoreSet
    ConfigInfoModified <|-- GeoServerInfoModified
    ConfigInfoModified <|-- LoggingInfoModified
    ConfigInfoModified <|-- ServiceModified
    ConfigInfoModified <|-- SettingsModified
    InfoRemoved <|-- CatalogInfoRemoved
    InfoRemoved <|-- ConfigInfoRemoved
    ConfigInfoRemoved <|-- ServiceRemoved
    ConfigInfoRemoved <|-- SettingsRemoved
    class GeoServerEvent{
        <<abstract>>
        String origin
        long timestamp
        String author
        String id
    }
    class LifecycleEvent{
        <<abstract>>
    }
    class ReloadEvent{
    }
    class ResetEvent{
    }
    class UpdateSequenceEvent{
        Long updateSequence
    }
    class InfoEvent{
        <<abstract>>
        String objectId
        ConfigInfoType objectType
    }
    class InfoAdded{
        <<abstract>>
        ~I extends Info~ object
    }
    class InfoModified{
        <<abstract>>
        Patch patch
    }
    class InfoRemoved{
        <<abstract>>
    }
    class ConfigInfoAdded{
        <<abstract>>
    }
    class ConfigInfoModified{
        <<abstract>>
    }
    class ConfigInfoRemoved{
        <<abstract>>
    }
    class ServiceModified{
      String workspaceId
    }
    class ServiceRemoved{
      String workspaceId
    }
    class SettingsModified{
      String workspaceId
    }
    class SettingsRemoved{
      String workspaceId
    }
    class DefaultDataStoreSet{
      String workspaceId
      String defaultDataStoreId
    }
    class DefaultNamespaceSet{
      newNamespaceId
    }
    class DefaultWorkspaceSet{
      String newWorkspaceId
    }
    class SecurityConfigChanged{
      String reason
    }
    class ConfigInfoType {
        <<enumeration>>
        Catalog
        WorkspaceInfo
        NamespaceInfo
        CoverageStoreInfo
        DataStoreInfo
        WmsStoreInfo
        WmtsStoreInfo
        FeatureTypeInfo
        CoverageInfo
        WmsLayerInfo
        WmtsLayerInfo
        LayerInfo
        LayerGroupInfo
        MapInfo
        StyleInfo
        GeoServerInfo
        ServiceInfo
        SettingsInfo
        LoggingInfo
    }
```
