# GeoWebCache starter

A set of spring-boot auto configurations to integrate different
GeoWebcache functionalities into other microservices.

## Cloud Native GeoServer specific extensions

```mermaid
classDiagram
    direction TB
    TileLayerCatalog <|-- ResourceStoreTileLayerCatalog
    TileLayerCatalog <|-- CachingTileLayerCatalog
    CatalogConfiguration <|-- CloudCatalogConfiguration
    CatalogConfiguration --> TileLayerCatalog
    XmlConfiguration <|-- CloudGwcXmlConfiguration
    ConfigurationResourceProvider <|-- CloudXMLResourceProvider
    XmlConfiguration --> ConfigurationResourceProvider
    <<Interface>> TileLayerCatalog
    <<Interface>> ConfigurationResourceProvider
    class CloudCatalogConfiguration{
        <<EventListener>>
        onTileLayerEvent(TileLayerEvent event)
    }
    class ResourceStoreTileLayerCatalog{
        - ResourceStore resourceStore
        findAll() Stream~GeoServerTileLayerInfo~
    }
    class CachingTileLayerCatalog{
        - CacheManager cacheManager
        - TileLayerCatalog delegate
    }
    class CloudGwcXmlConfiguration{
        <<EventListener>>
        onGridsetEvent(GridsetEvent event)
        onBlobStoreEvent(BlobStoreEvent event)
    }
```

## Usage



