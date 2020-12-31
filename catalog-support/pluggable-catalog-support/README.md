# Pluggable Catalog/Config support

Alternative to GeoServer's default implementation for the catalog and configuration backend (`CatalogImpl/DefaultFacadeImpl` and `GeoServerImpl/DefaultGeoServerFacade`) that aims at improving their internal design in order to have clearer contracts and promote ease of extensibility.

## Motivation

There aren't many alternative back-ends to the configuration subsystem, albeit being the core of GeoServer and having been initially thought of for plug-ability.

Truth is, although the interfaces are rather simple, there are a number of issues with the default implementations that preclude reuse, enforcing to re-invent the wheel or just copy and paste a lot of (business logic) code and deal with it.

The only two alternatives I know of are the [jdbcconfig community module](https://github.com/geoserver/geoserver/blob/06230581/src/community/jdbcconfig/src/main/java/org/geoserver/jdbcconfig/catalog/JDBCCatalogFacade.java#L52)'s, and [Stratus](https://github.com/planetlabs/stratus/blob/77838a22/src/stratus-redis-catalog/src/main/java/stratus/redis/catalog/RedisCatalogFacade.java#L77)'. By looking at them, you can identify a common pattern: creating an alternative storage backend requires dealing with a lot of implementation details  unrelated to the primary objective of providing a storage backend plugin, basically, an alternative `CatalogFacade` to be injected as `CatalogImpl`'s backing DAO.

## Identified issues

* `ModificationProxy` abstraction leak:

 `ModificationProxy` serves two purposes really:
 
1. it enforces the Catalog's information hiding by not allowing the returned, (possibly) live objects, to be modified by its callers;
2. Works as an enabler for [MVCC](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) on `save(*Info)`, by providing the delta changes in the form of lists of changed property names, old values, and new values. Otherwise, upon save, a call on a thread that's supposed to update only a subset of the object properties, would override all the properties another thread just changed.

Now, `CatalogFacade` shouldn't be the place where that happens, but purely as an implementation detail of `CatalogImpl`, which should give `CatalogFacade` both the object to save, and the delta properties, so that `CatalogFacade`'s implementation can apply the changes how it sees fit in order to guarantee the operation's atomicity.
The current situation is that `CatalogImpl` *relies* on `CatalogFacade` always returning a `ModificationProxy`, which further complicates realizing the initial design goal (I think) of `CatalogFacade` being a pure Data Access Object, so that implementations could be interchangeable. Instead, the lack of single responsibility among these two classes forces all  alternate `CatalogFacade` implementors to replicate the logic of `DefaultCatalogFacade`.

* `ResolvingProxy` responsibility leak: a `CatalogFacade` shouldn't kwow it can get a proxy, yet on every `add(*Info)` method it tries to "resolve" object references to actual object on each overloaded version of `DefaultCatalogFacade.resolve(CatalogInfo)`, which in turn call `ResolvingProxy.resolve(getCatalog(), ...)` and `ModificationProxy.unwrap(...)` for each posible proxied reference. Now, `ResolvingProxy` usage is only an implementation detail of the default persitence mechanism, used by `XstreamPersister` at deserialization time, and `GeoServerResourcePersister` catalog listener to save a modified obejct to its xml file on disk. The only other direct user is restconfig's `LayerGroupController`, which correctly resolves all layergroup's referred layers upon a POST request, which could be an indication the restconfig API relies on the above undocumented behavior for other kinds of objects, or that it is ok to rely correct resolution of object references by its use of `XstreamPersister`, haven't checked in dept.

* `Catalog.detach(...)`: Used to remove all proxies from the argument object, including any reference to another catalog object, and return the raw, un-proxied object.
    * Breaks Catalog's information hiding, by design. It's an API method, shouldn't expose directly or indirectly implementation details. It's telling the returned objects are proxied AND live, and providing a workaround around information hiding to create unadvertised side effects if the returned live object is modified.
    * Only used so `web-core`'s `LayerModel` get serialize a `LayerInfo`, who should just rely on the fact that all `CatalogInfo`'s are `Serializable` by contract. If that contract is impracticable, then it should be removed in favor of a canonical way to serialize and deserializa `CatalogInfo` objects.

* `DefaultCatalogFacade`:

    * Event handling responsibility leak: With `ModificationProxy`'s responsibility leak to the DAO, comes event handling responsibility leak, which is split between the `Catalog` and the `CatalogFacade`. All the events for `add()` and `remove()` are fired by the catalog, whist event propagation for `save()` is delegated to the facade.
    
    * Id handling contract: Since the `CatalogInfo` object identifiers are busines ids, and not auto-generated, the contract should be clear in that `CatalogFacade.add()` expects the id to be set, and fail if an object with that id already exists, while `Catalog.add()`'s contract should be clear in that it can get either a pre-assigned id, but will create and assign one if that's not the case.
    
    * Business logic leak: `LayerInfo`'s `name` property is linked to its referred `ResourceInfo`'s name. This is enforced by `CatalogFacade`'s `save(LayerInfo)`, which indirectly, through `LayerInfoLookup` specialization of `CatalogInfoLookup`, takes the resource name instead of the layer's name to update its internal name-to-object hash map; and by `save(ResourceInfo)`, which saves both the argument obejct itself, and the linked `LayerInfo` by means of the specialized method `LayerInfoLookup.save(ResourceInfo)`. Instead of buried in the object model implementation (with `LayerInfoImpl.getName()` and `setName()` deferring to its internal `resource`, it should be handled as a business rule inside the `Catalog.save(LayerInfo)` and `Catalog.save(ResourceInfo)` methods, and not leak down to the DAO. A similar thing happens with `LayerInfo`'s `title` property, it defers to its resource's `title` property. Now, when saving a layer, both its resource's name and tile get effectively updated as a side effect of the `ModificationProxy.commit()`, but IMO it should be explicitly handled as a lot other business rules by `CatalogImpl`, so that `CatalogFacade` implementors can work under a cleaner contract and not having to deal with ModificationProxy at all, as mentioned above. This also breaks event handling. Given when the layer's name or title is updated, what's actually updated is the `ResourceInfo`, it would be expected that pre and post modify events would be triggered also for the resource object, and not just for the layer object, which is yet another reason to handle this artificial link explicitly as a business rule in Catalog.
    
    * Unnecessary special cases: `LayerInfoLookup` wouldn't be necessary if not due to the above mentioned responsibility issue. `MapInfo`s internal storage is a `List` instead of a `LayerInfoLookup`. Given everything related to `MapInfo` is plain dead code, it should either be removed or `MapInfo` related methods throw an `UnsupportedOperationException`
    
    * Unnecessary synchronization: all internal "repositories" are thread safe, yet `DefaultCatalogFacade` synchronizes on them at almost all methods.
    
    * `CatalogFacade` shouldn't know anything about `Catalog`, since it's at a lower level of abstraction. The above issues force a double linked dependency.
    
*  `IsolatedCatalogFacade`: 
    *Note to self: isolated workspaces are a means to allow several workspaces sharing the same namespace **URI** within the scope of each "virtual workspace"*, not the same `NamespaceInfo`, whose `prefix` is still tied to a workspace name.

    * `CatalogImpl` decorates its default catalog facade on its default constructor, by calling `setFacade(new IsolatedCatalogFacade(DefaultCatalogFacade(this)))`,  but it should be `setFacade()` the one that decorates it. That's because `Catalog.setFacade()` is an API method, and the way to override the default in-memory storage by an alternate implementation, but this breaks the support for isolated workspaces in that case.
    On the other hand, I can't see why the isolated workspace handling needs to be implemented as a `CatalogFacade` decorator, which is intended to be the DAO, and not as a `Catalog` decorator, which is the business object, just as so many other catalog decorators (`SecureCatalogImpl`, `LocalWorkspaceCatalog`, `AdvertisedCatalog`). Moreover, since we're at it, there's `AbstractFilteredCatalog` already, which the catalog decorator for isolated workspaces could inherit from, and at the same time `AbstractFilteredCatalog` could inherit from `AbstractCatalogDecorator` to avoid code duplication on the methods it doesn't need to override.

    * `<T> IsolatedCatalogFacade.filterIsolated(CloseableIterator<T> objects, Function<T, T> filter)` breaks the streaming nature of the calling method by creating an `ArrayList<T>` and populating it. It shoud decorate the argument iterator to apply the filtering in-place:
    
```java
        List<T> iterable = new ArrayList<>();
        // consume the iterator
        while (objects.hasNext()) {
            T object = objects.next();
            if (filter.apply(object) != null) {
                // this catalog object is visible in the current context
                iterable.add(object);
            }
        }
        // create an iterator for the visible catalog objects
        return new CloseableIteratorAdapter<>(iterable.iterator());
```
    
---

As a final note, as fixing the above mentioned catalog and catalog facade internal design would make for a really simple `DefaultCatalogFacade` implementation, it can be made even simpler and extensible by turning it into a simple grouping of DDD-like `Repository` abstractions. This would make the task of creating an alternative catalog backend, a simple matter implementing these repositories, with clean, self describing method contracts:

```java

interface CrudCatalogInfoRepository<T extends CatalogInfo>{
    void add(T value);
    void remove(T value);
    void update(T value);
    List<T> findAll();
    <U extends T> List<U> findAll(Filter filter);
    <U extends T> List<U> findAll(Filter filter, Class<U> infoType);
    <U extends T> U findById(String id, Class<U> clazz);
    <U extends T> U findByName(Name name, Class<U> clazz);
}

interface WorkspaceRepository extends CatalogInfoRepository<WorkspaceInfo> {
    void setDefaultWorkspace(WorkspaceInfo workspace);
    WorkspaceInfo getDefaultWorkspace();
}
    
interface NamespaceRepository extends CatalogInfoRepository<NamespaceInfo> {
    void setDefaultNamespace(NamespaceInfo namespace);
    NamespaceInfo getDefaultNamespace();
    NamespaceInfo findOneByURI(String uri);
    List<NamespaceInfo> findAllByURI(String uri);
}
....

class CatalogInfoLookup<T extends CatalogInfo> implements CatalogInfoRepository<T> {

    ConcurrentMap<Class<T>, ConcurrentMap<String, T>> idMultiMap = new ConcurrentHashMap<>();
    ConcurrentMap<Class<T>, ConcurrentMap<Name, T>> nameMultiMap = new ConcurrentHashMap<>();
    ConcurrentMap<Class<T>, ConcurrentMap<String, Name>> idToMameMultiMap = new ConcurrentHashMap<>();
    ...
   
static class WorkspaceInfoLookup extends CatalogInfoLookup<WorkspaceInfo> implements WorkspaceRepository {
    private WorkspaceInfo defaultWorkspace;
    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        this.defaultWorkspace = workspace == null ? null : findById(workspace.getId(), WorkspaceInfo.class);
    }
    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return defaultWorkspace;
    }
}

abstract class AbstractCatalogFacade implements CatalogFacade {
    protected NamespaceRepository namespaces;
    protected WorkspaceRepository workspaces;
    protected StoreRepository stores;
    protected ResourceRepository resources;
    protected LayerRepository layers;
    protected LayerGroupRepository layerGroups;
    protected MapRepository maps;
    protected StyleRepository styles;

    public void setWorkspaces(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
    }
    ...
    public @Override WorkspaceInfo add(WorkspaceInfo workspace) {
        workspaces.add(unwrapped);
    }
    public @Override WorkspaceInfo getWorkspace(String id) {
        return workspaces.findById(id, WorkspaceInfo.class);
    }
    ....
}

class DefaultCatalogFacade extends AbstractCatalogFacade{
    public DefaultCatalogFacade() {
        setNamespaces(new NamespaceInfoLookup());
        setWorkspaces(new WorkspaceInfoLookup());
        setStores(new StoreInfoLookup());
        setLayers(new LayerInfoLookup());
        setResources(new ResourceInfoLookup());
        setLayerGroups(new LayerGroupInfoLookup());
        setMaps(new MapInfoLookup());
        setStyles(new StyleInfoLookup());
    }
}

```



