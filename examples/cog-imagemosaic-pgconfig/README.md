# COG ImageMosaic on GeoServer Cloud (pgconfig backend)

This example demonstrates how to publish an ImageMosaic layer built from
Cloud Optimized GeoTIFF (COG) granules stored on object storage, on a
GeoServer Cloud deployment that uses the pgconfig catalog backend, with the
granule index kept in PostGIS instead of a shared filesystem. The vanilla
GeoServer REST API is used throughout: every request in this demo is
identical to the one you would send to a single-instance GeoServer.

## Requirements

This example requires GeoServer Cloud 3.1.0 or later. That release adds
cross-pod propagation of ImageMosaic configuration for the pgconfig backend:
a mosaic store created on one pod (for example through the REST API,
answered by the `restconfig` service) renders correctly on every other pod
(`wms`, `wcs`, `wps`, `gwc`, `webui`) without a shared filesystem.

The compose file defaults the GeoServer Cloud images to `3.1.0-SNAPSHOT`,
matching locally built images. Set the `GSCLOUD_VERSION` environment variable
to pin a different tag, for example a released version:

```bash
export GSCLOUD_VERSION=3.1.0
```

If you are running an earlier version, see
[Fallback for releases without the fix](#fallback-for-releases-without-the-fix).

## Quick start

Fetch the demo COG granules (four quadrants of a world land/shallow-water
image), then start the composition:

```bash
scripts/00_download_cogs.sh
docker compose up -d
```

Wait for all services to report healthy, then run the three use case
runners. Each prints a description of what it demonstrates and executes the
whole workflow end to end, in its own workspace; the three results coexist
afterwards:

```bash
scripts/usecase_A_incremental_rest.sh    # workspace usecase_a
scripts/usecase_B_directory_harvest.sh   # workspace usecase_b (shared mount, discouraged)
scripts/usecase_C_external_index.sh      # workspace usecase_c
```

Each runner ends with a WMS `GetMap` request served by the `wms` service, a
different container than the `restconfig` service that created the store and
added the granules. Getting a full-color PNG back is the cross-pod check: it
confirms the mosaic configuration written by one pod is visible to another,
without a shared filesystem for the mosaic configuration on the pgconfig
backend.

The three use cases are described below. Use case A is also broken into
single-call building-block scripts (`01` through `05`) you can run one at a
time to inspect each REST request and its response.

## Use case A: incremental adds through REST

This is the workflow most sites already run against vanilla GeoServer, and
the recommended one for a cloud-native deployment.
`scripts/usecase_A_incremental_rest.sh` runs it end to end in workspace
`usecase_a`. The building-block scripts `01` through `05` below each isolate
one call and its expected response for inspection; the runner invokes `01`
through `04` in order.

### `01_create_empty_mosaic.sh`: create the store

Vanilla API: `PUT .../coveragestores/{store}/file.imagemosaic?configure=none`,
body a zip file containing `indexer.properties` and `datastore.properties`,
header `Content-type: application/zip`.

The script builds the zip in a temporary directory. `indexer.properties`
sets `Cog=true`, telling the mosaic plugin to read granules as Cloud
Optimized GeoTIFFs over HTTP range requests instead of expecting local
files, and `CanBeEmpty=true`, allowing the store to be created before any
granule exists.
`datastore.properties` points the granule index at a JNDI datasource (the
JNDI form is explained in
[datastore.properties: JNDI vs direct parameters](#datastoreproperties-jndi-vs-direct-parameters)).
The request also creates the target workspace first if it does not exist
yet; this is a convenience for a fresh environment and identical to a
regular workspace creation call.

### `02_add_granules.sh [url...]`: add granules

Vanilla API: `POST .../coveragestores/{store}/remote.imagemosaic`, body the
granule URL as plain text, header `Content-type: text/plain`, one request per
granule.

With no arguments the script adds the four `land_shallow_topo` quadrants
served by the `s3proxy` container, printing `Adding granule <url>` before each
call. Pass one or more URLs to add a different set instead, which is how you
would register granules one at a time.

`remote.imagemosaic` accepts any URL the mosaic reader can open, including
`http(s)://` and `s3://` locations; this is what makes granules on object
storage possible without a shared mount. It is not the same as
`external.imagemosaic`, which only accepts a path on the server's local
filesystem: see [Use case B](#use-case-b-bulk-harvest-of-a-local-directory-discouraged-for-cloud-native).

### `03_publish_coverage.sh`: publish the layer

Once at least one granule has been added, the store exposes a coverage that
can be published as a layer. The script first calls
`GET .../coveragestores/{store}/coverages.xml?list=all` to list the
coverages the store makes available, then
`POST .../coveragestores/{store}/coverages` with a minimal
`<coverage>` document to publish it.

### `04_verify.sh`: check the granule index and render the layer

`GET .../coverages/{coverage}/index/granules.json` lists the granules
currently in the index, straight from PostGIS. The script then issues a WMS
`GetMap` request, saves the response to `/tmp`, and reports the HTTP status
and the file type of the result. A PNG image confirms the layer renders.

### `05_delete_granule.sh <location-url>`: remove a granule

Vanilla API:
`DELETE .../coverages/{coverage}/index/granules.xml?filter=location='<url>'&purge=none`.

The `filter` parameter is a CQL filter evaluated against the index table; a
`location` equality filter is the common case for removing one granule by
its URL. `purge=none` removes only the index row: the COG file in object
storage is left untouched, which is almost always what you want when the
granule is externally managed storage rather than an upload GeoServer owns.

## Use case B: bulk harvest of a local directory (discouraged for cloud-native)

`scripts/usecase_B_directory_harvest.sh` runs this end to end in workspace
`usecase_b`. Vanilla GeoServer supports
`POST .../coveragestores/{store}/external.imagemosaic` with a directory path,
which harvests every matching granule under that path in a single call.

`external.imagemosaic` resolves only paths on the server's local filesystem;
the ImageMosaic plugin has no equivalent that harvests a directory from
remote object storage. Making it work therefore means putting the granule
files on a volume shared by every GeoServer pod that serves the layer. This
compose file bind-mounts `./cogs` read-only at `/opt/cogs` on every GeoServer
service (see `geoserver_template` in `compose.yml`) for the demo.

That shared volume is exactly what the pgconfig backend is meant to avoid, so
this pattern is discouraged for a cloud-native deployment. Prefer use case A
(remote adds, one granule at a time) or use case C (externally managed index)
for anything on object storage. The runner is included for completeness, and
to show that even the shared-mount workflow now propagates its mosaic
configuration across pods on pgconfig: the store is created and harvested on
`restconfig`, and the layer renders on `wms`. Only the granule files need the
shared mount; the mosaic configuration still lives in the pgconfig database
and reaches every pod like it does for the other two use cases.

## Use case C: externally managed index at scale

Adding granules one REST call at a time does not scale to a production
catalog with thousands of granules populated by a batch ingestion pipeline.
For that scenario, manage the PostGIS index table directly and let GeoServer
read it live, with no REST calls at all after the store is created.

Prefer this approach when:

- the catalog holds thousands of granules and REST calls one at a time would
  be too slow or too chatty;
- granules are produced by a batch pipeline (for example, an ETL job or a
  scheduled ingestion process) that already writes to PostgreSQL, or can run
  `gdaltindex`;
- multiple writers need to add or remove granules concurrently without going
  through GeoServer.

`scripts/usecase_C_external_index.sh` runs the full workflow end to end in
workspace `usecase_c`, against coverage `landshallow_external`:

1. Create a schema and a granule index table in PostGIS, and populate it
   with three granules using plain SQL `INSERT` statements.
2. Create the mosaic store over the already-populated table, with
   `UseExistingSchema=true` set in `indexer.properties`. This tells the
   mosaic plugin the index table already exists and is authoritative: do not
   create or reconcile it from a directory scan.
3. Publish the coverage and render it: three quadrants appear.
4. Add the fourth quadrant with a plain SQL `INSERT`, no REST call involved.
5. Render again: the new granule appears immediately, on every pod, because
   the index is queried live at read time.

### Populating the index with `gdaltindex`

The demo script uses SQL `INSERT` statements to keep the example
self-contained, but any tool that writes to the same table works. GDAL's
`gdaltindex` utility can build the index directly from files exposed through
a URL, including a bucket served over HTTP, using GDAL's `/vsicurl` virtual
file system. It is an alternative to the SQL statements in
`scripts/usecase_C_external_index.sh`, not a replacement for the whole script:
the store still needs to be created and the coverage published through the
REST calls that script already makes.

To reach the mosaic index database from host-side tools, this compose file
publishes PostgreSQL on `127.0.0.1:15432` (mapped from the `geodatabase`
service's `5432`). The example below appends to the table that
`scripts/usecase_C_external_index.sh` already created
(`mosaic_external.landshallow_external`, with `the_geom` and `location`
columns): running it requires step 1 of that script to have created the
table first. Adjust `host`, `port`, `dbname`, `user`, and `password` if you
are pointing `gdaltindex` at a different environment:

```bash
gdaltindex -f PostgreSQL "PG:host=localhost port=15432 dbname=postgis user=geoserver password=geoserver schemas=mosaic_external" \
  -lyr_name landshallow_external -tileindex location -append -write_absolute_path \
  /vsicurl/http://localhost:9080/cogs/land_shallow_topo_21600_NE_cog.tif
```

`-append` adds to the existing table instead of creating a new one, reusing
its `the_geom` and `location` columns as they are. If you let `gdaltindex`
create the table instead (by omitting `-append`), be aware that its default
geometry column name is `wkb_geometry`, not `the_geom`: the indexer expects
`the_geom` (see `Schema=*the_geom:Polygon,location:String` in
`indexer.properties`). Give a table `gdaltindex` creates from scratch the
`-lco GEOMETRY_NAME=the_geom` layer creation option, available in GDAL 3.9
and later, to match that expectation.

This is convenient for populating or refreshing a mosaic index from a bucket
listing as part of an ingestion pipeline, without writing custom SQL.

**Warning:** never issue a REST harvest call (`remote.imagemosaic` or
`external.imagemosaic`) against a store created with
`UseExistingSchema=true`. The mosaic plugin skips its own index maintenance
on such stores: a harvest call returns success but silently inserts nothing.
Manage granules for these stores exclusively through direct database writes
(SQL or `gdaltindex`).

## datastore.properties: JNDI vs direct parameters

Every ImageMosaic store needs a `datastore.properties` file inside its
configuration zip, telling the mosaic plugin how to reach its PostGIS
granule index. Two forms work; this example uses both, one per use case.

### JNDI (primary form, used in this example)

`scripts/01_create_empty_mosaic.sh` ships a `datastore.properties` that
references a JNDI datasource by name instead of embedding connection
parameters:

```properties
SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory
jndiReferenceName=java:comp/env/jdbc/postgis
Loose\ bbox=true
preparedStatements=false
```

The datasource itself is configured once, for the whole deployment, rather
than once per store. In this compose file it is enabled by the
`JNDI_POSTGIS_ENABLED=true` environment variable shared by every GeoServer
service, which turns on a `postgis` JNDI datasource already defined in the
GeoServer Cloud configuration image and pointing at the `postgis` database
on the `geodatabase` service. No credentials travel inside the mosaic
configuration zip: only a reference name.

On Kubernetes, the same datasources are configured through Spring Boot
externalized configuration properties, typically shipped in a config map
mounted by the configuration service. The property schema, from the
GeoServer Cloud configuration guide, is:

```yaml
jndi:
  datasources:
    <name>:
      enabled: true
      url: jdbc:postgresql://host:5432/database
      username: sa
      password: sa
      wait-for-it: true
      wait-timeout: 60
      connection-timeout: 250
      idle-timeout: 60000
```

A datasource named `postgis` is bound as `java:comp/env/jdbc/postgis`, which
is exactly the reference name used in `datastore.properties` above. Only the
config map needs the credentials; every mosaic store simply names the
datasource.

### Direct connection parameters (vanilla-docs parity)

The alternative form, familiar from the vanilla GeoServer documentation,
embeds the connection parameters directly. `scripts/usecase_C_external_index.sh`
uses this form for its externally managed index store:

```properties
SPI=org.geotools.data.postgis.PostgisNGDataStoreFactory
host=geodatabase
port=5432
database=postgis
schema=mosaic_external
user=geoserver
passwd=geoserver
Loose\ bbox=true
preparedStatements=false
```

This form works identically to vanilla GeoServer and is useful when a JNDI
datasource is not available, or when a store must point at a schema or
database different from the shared one. Its tradeoff is that the database
credentials travel inside every uploaded configuration zip: anyone who can
read or intercept a store's configuration also has the database password.
Prefer the JNDI form for anything beyond a quick test.

## Kubernetes notes

This compose file is meant to be read side by side with a Kubernetes
deployment, not run there directly. The mapping is:

- **JNDI datasource configuration.** The `JNDI_POSTGIS_ENABLED` environment
  variable used here is a shortcut for enabling a datasource already baked
  into the GeoServer Cloud configuration image. On Kubernetes, configure the
  `jndi.datasources.<name>.*` properties shown above directly, typically as
  a config map mounted into the configuration service, keeping connection
  parameters and credentials independent of the GeoServer application
  images.
- **Mosaic index database provisioning.** This compose file creates the
  `postgis` database and enables the PostGIS extension through the
  `initdb` scripts mounted into the database container on first startup
  (`examples/cog-imagemosaic-pgconfig/initdb/`). On Kubernetes, provision
  the equivalent database and extension through whatever mechanism you use
  to manage the PostgreSQL instance, be it an operator, a managed database
  service, or an init job.
- **Granule URL reachability.** Every granule URL stored in the index,
  whether added through REST or written directly to the table, must be
  reachable from every pod that serves the layer (`wms`, `wcs`, `wps`,
  `gwc`, `webui`), not just from the one that created the store. In this
  demo that means the internal `http://s3proxy/cogs/...` URLs used by the
  scripts, reachable inside the compose network from every service; on
  Kubernetes it means a URL resolvable from every pod's network namespace,
  typically an internal service DNS name or a public object storage
  endpoint.

## Fallback for releases without the fix

Cross-pod ImageMosaic support for the pgconfig backend is only available
starting with GeoServer Cloud 3.1.0. On an earlier release, the mosaic
configuration directory (holding `indexer.properties`,
`datastore.properties`, and the files the mosaic plugin generates from
them) is not shared across pods: a store created on one pod is invisible to
the others.

The proven workaround on earlier releases is to mount a small shared
read-write-many (RWX) volume for mosaic store directories, and reference it
with an absolute path in the store's file URL instead of the workspace's
default relative data directory. Everything else in this example, the REST
call sequence, the granule index in PostGIS, both forms of
`datastore.properties`, use cases A and C, stays identical: only the mosaic
store's own configuration directory needs the shared volume, not the
granules, which already live on object storage.

## Gotchas

- **`remote` vs `external`.** Use `remote.imagemosaic` for granule URLs on
  object storage or any HTTP(S) endpoint. `external.imagemosaic` only
  accepts a path resolvable on the server's local filesystem; using it with
  an `http://` or `s3://` URL fails.
- **`HttpRangeReader` is already the default.** The scripts set
  `CogRangeReader=it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader`
  explicitly in `indexer.properties`, even though it is the plugin's
  default range reader for COGs. The line is there for documentation: it
  makes the choice visible instead of relying on an implicit default, and is
  the one to change if granules are served from a source that needs a
  different range reader (for example, one of the S3-specific readers).
- **Time-dimension mosaics need more properties.** This example builds a
  single-dimension mosaic. A mosaic with a time dimension additionally
  needs `TimeAttribute` and `PropertyCollectors` in `indexer.properties`,
  plus a `timeregex.properties` file describing how to extract the
  timestamp from each granule's file name.
- **Deleting a broken store requires an openable reader.** GeoServer's
  coverage store deletion opens the store's reader even for a non-recursive
  delete. If the reader cannot be opened, for example because the index
  table or the datastore connection is broken, the store cannot be removed
  through the REST API and must be cleaned up directly in the catalog
  database instead.
