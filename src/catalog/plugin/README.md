# Pluggable Catalog/Config Support

The `catalog-plugin` module provides an alternative to the core GeoServer catalog and configuration backend (`CatalogImpl` and `GeoServerImpl`). It redefines the relationship between `Catalog` and `CatalogFacade`, improving their internal design by establishing clearer contracts and promoting ease of extensibility. This addresses several limitations in the upstream code, making it simpler to implement alternative catalog backends.

## Motivation

GeoServer's catalog and configuration subsystem was designed with pluggability in mind, yet few alternative backends exist. The upstream implementations—particularly `CatalogImpl` and `DefaultCatalogFacade`—burden alternative backends.

For example, both the [jdbcconfig community module](https://github.com/geoserver/geoserver/blob/06230581/src/community/jdbcconfig/src/main/java/org/geoserver/jdbcconfig/catalog/JDBCCatalogFacade.java#L52) and the old Boundless' [Stratus](https://github.com/planetlabs/stratus/blob/77838a22/src/stratus-redis-catalog/src/main/java/stratus/redis/catalog/RedisCatalogFacade.java#L77), have duplicating complex business logic.

The `catalog-plugin` simplifies this by enforcing a clear separation between business logic and data access, reducing complexity and encouraging new catalog storage solutions.

## Identified Issues in the Upstream Code

The upstream catalog system has design flaws that hinder extensibility and maintainability:

### ModificationProxy Abstraction Leak

- **Description**: `ModificationProxy` enforces information hiding and enables [MVCC](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) for partial updates via delta changes (property names, old values, new values).
- **Issue**: This logic resides in `DefaultCatalogFacade` rather than `CatalogImpl`, forcing `CatalogImpl` to rely on `CatalogFacade` returning a `ModificationProxy`. This complicates alternative implementations and violates the DAO's single responsibility.

### ResolvingProxy Responsibility Leak

- **Description**: `CatalogFacade` resolves proxies in every `add(*Info)` method via `DefaultCatalogFacade.resolve(CatalogInfo)`, invoking `ResolvingProxy.resolve()` and `ModificationProxy.unwrap()`.
- **Issue**: Proxy resolution, an implementation detail of the default persistence mechanism (e.g., `XstreamPersister`, `GeoServerResourcePersister`), burdens `CatalogFacade`, requiring alternative backends to handle it unnecessarily.

### Catalog.detach(...) Method

- **Description**: Removes proxies from objects, exposing unproxied, live objects.
- **Issue**: Breaks information hiding by revealing implementation details; used in `web-core` for serialization (e.g., `LayerModel`), which should rely on `CatalogInfo`’s `Serializable` contract.

### DefaultCatalogFacade Issues

- **Event Handling Leak**: Event propagation is split—`CatalogImpl` handles `add()` and `remove()`, while `CatalogFacade` manages `save()`—complicating alternative implementations.
- **ID Handling Contract**: Unclear rules for identifiers; `CatalogFacade.add()` should expect pre-assigned IDs and fail on duplicates, while `Catalog.add()` should assign IDs if absent.
- **Business Logic Leak**: `LayerInfo`’s `name` and `title` defer to `ResourceInfo`, enforced by `CatalogFacade` via `LayerInfoLookup`. This should be a business rule in `CatalogImpl`.
- **Unnecessary Special Cases**: `LayerInfoLookup` exists due to above issues; `MapInfo` uses a simpler `List` (dead code, suggesting removal or an `UnsupportedOperationException`).
- **Unnecessary Synchronization**: Thread-safe repositories are redundantly synchronized by `DefaultCatalogFacade`.
- **Circular Dependencies**: `CatalogFacade` depends on `Catalog`, creating a bidirectional link.

## Key Improvements Over Core GeoServer

The `catalog-plugin` introduces significant enhancements over the core GeoServer catalog system, detailed below:

### 1. Separation of Concerns: Catalog vs. CatalogFacade

- **Upstream Code**: In the original design, `CatalogFacade` (e.g., `DefaultCatalogFacade`) is burdened with both data access and business logic, including managing proxies (via `ModificationProxy`), enforcing rules like linking `LayerInfo` and `ResourceInfo`, and handling event propagation. This entanglement makes it difficult to implement alternative backends without replicating complex logic.
- **Improvement**: The module reassigns responsibilities:
  - **`CatalogImpl`**: Takes charge of business logic, such as linking `LayerInfo` and `ResourceInfo`, managing proxies, and issuing events.
  - **`CatalogFacade`**: Becomes a pure Data Access Object (DAO), responsible only for CRUD (Create, Read, Update, Delete) operations on catalog objects.
- **Benefit**: This separation allows developers to create simpler, streamlined implementations of `CatalogFacade`. Alternative backends no longer need to handle business rules, focusing solely on data storage and retrieval. This reduces complexity and makes it easier to plug in custom persistence layers, whether based on databases, in-memory structures, or other systems.
- **Source Evidence**: `CatalogFacade` methods are streamlined to basic operations (e.g., `add()`, `remove()`), while `CatalogImpl` encapsulates business logic.

### 2. Streamlined CatalogFacade Implementations

- **Upstream Challenge**: Creating a new `CatalogFacade` requires duplicating business logic—such as proxy unwrapping or event handling—across each backend, as seen in modules like `jdbcconfig` or Stratus.
- **Improvement**: By offloading business logic to `CatalogImpl`, `CatalogFacade` implementations are lightweight and focused. They only define how to interact with the underlying data store, not how to enforce GeoServer’s rules.
- **Benefit**: Developers can craft alternative backends more efficiently, with less code and fewer opportunities for errors. This streamlined approach enhances maintainability and encourages experimentation with new catalog storage solutions.
- **Source Evidence**: `CatalogFacade` methods accept plain `CatalogInfo` objects, free of proxy-related logic.

### 3. Composable Pipelines with ResolvingCatalogFacade
- **Upstream Limitation**: Object resolution (e.g., unwrapping proxies, linking related objects) is baked into `CatalogFacade`, forcing each implementation to reimplement these steps, leading to duplicated logic.
- **Improvement**: `ResolvingCatalogFacade` enables composable pipelines for handling inbound and outbound objects:
  - **Inbound Processing**: When saving, the pipeline can unwrap proxies (such as `ModificationProxy`), or resolve references before reaching the DAO.
  - **Outbound Processing**: When retrieving, it can decorate catalog objects, sanitize values, initialize collection properties, etc.
- **How It Works**: Acts as a decorator or wrapper around a base `CatalogFacade`, allowing chaining of processing steps (e.g., resolution, filtering, transformation) into a reusable pipeline.
- **Benefit**: Eliminates the need to duplicate object-handling logic across implementations. Developers define the pipeline once and apply it consistently, improving flexibility and maintainability. For example, a relational database backend can reuse the same resolution pipeline as an in-memory backend without rewriting logic.
- **Source Evidence**: `ResolvingCatalogFacade` wraps a base DAO, applying reusable processing steps.

### 4. Modular Repository-Based Design
- **Upstream Problem**: Monolithic `DefaultCatalogFacade` with internal maps.
- **Improvement**: Uses dedicated repositories (e.g., `WorkspaceRepository`, `NamespaceRepository`) for each object type.
- **Example**:
  ```java
  interface WorkspaceRepository extends CatalogInfoRepository<WorkspaceInfo> {
      void setDefaultWorkspace(WorkspaceInfo workspace);
      WorkspaceInfo getDefaultWorkspace();
  }
  ```
- **Benefit**: Simplifies extension by allowing custom repository implementations, enhancing modularity.
- **Source Evidence**: `AbstractCatalogFacade` integrates repositories via setters (e.g., `setWorkspaces()`).

### 5. Improved Event Handling
- **Upstream Issue**: Inconsistent event handling split between `CatalogImpl` and `CatalogFacade`.
- **Improvement**: All events centralized in `CatalogImpl`.
- **Benefit**: Cleaner, consistent event management, simplifying the DAO’s role.
- **Source Evidence**: Events triggered in `CatalogImpl` post-DAO operations.

### 6. Removal of Unnecessary Synchronization
- **Upstream Issue**: Redundant synchronization in `DefaultCatalogFacade` on thread-safe collections.
- **Improvement**: Relies on inherent thread-safety (e.g., `ConcurrentHashMap`) without extra locks.
- **Benefit**: Improved efficiency without compromising safety.
- **Source Evidence**: Repositories use thread-safe collections natively.

### 7. Simplified LayerInfo and ResourceInfo Handling
- **Upstream Issue**: DAO enforces links between `LayerInfo` and `ResourceInfo` (e.g., `name`, `title`).
- **Improvement**: Rules shifted to `CatalogImpl`, DAO treats objects independently.
- **Benefit**: Backends avoid replicating business logic, simplifying their design.
- **Source Evidence**: `CatalogImpl.save(LayerInfo)` updates `ResourceInfo`, DAO performs basic saves.

### 8. Removal of Dead Code and Special Cases
- **Upstream Issue**: Unused `MapInfo` and special cases like `LayerInfoLookup`.
- **Improvement**: Dead code removed or marked unsupported (e.g., `UnsupportedOperationException`).
- **Benefit**: Leaner, more maintainable codebase.
- **Source Evidence**: `MapInfo` methods throw exceptions, special cases eliminated.

### 9. Clearer ID Handling Contracts
- **Upstream Problem**: Ambiguous ID rules across `Catalog` and `CatalogFacade`.
- **Improvement**: Explicit contracts—`CatalogFacade.add()` requires pre-assigned IDs, rejects duplicates; `Catalog.add()` generates IDs if absent.
- **Benefit**: Ensures consistency across implementations.
- **Source Evidence**: ID logic split between `CatalogImpl` and DAO enforces clear rules.

## Usage in GeoServer Cloud

In GeoServer Cloud, the `catalog-plugin` serves as the base to implement different catalog and configuration backends.

