# ImageMosaics through the REST API

This guide shows how to publish an ImageMosaic of Cloud Optimized GeoTIFFs (COGs) stored on object storage, using the same REST API calls that work against vanilla GeoServer. On GeoServer Cloud with the pgconfig catalog backend, the resulting store works on every pod with no shared filesystem: the granule index lives in PostGIS and the mosaic configuration files propagate through the config database.

Three approaches are covered. Pick one from the [comparison below](#choosing-an-approach); most deployments want the first.

## Prerequisites

- GeoServer Cloud 3.0.1 or newer, deployed with the pgconfig catalog backend.
- COG granules reachable from the GeoServer pods over `http(s)://`.
- A PostGIS database, reachable from every pod, to hold the granule index. It can be a dedicated database or a schema next to other data; do not use the pgconfig catalog database itself.
- Admin credentials for the REST API.

Every command below uses these variables; set them to match your deployment:

```bash
export GEOSERVER_URL="https://geoserver.example.com/geoserver/cloud"
export REST="$GEOSERVER_URL/rest"
export AUTH="admin:geoserver"

export WORKSPACE=cogs
export STORE=scenes
export COVERAGE=scenes
```

## Choosing an approach

| Approach | When to use it | Key endpoint |
|----------|----------------|--------------|
| [Incremental adds](#incremental-adds-through-the-rest-api) (recommended) | Granules on object storage, added one or a few at a time as they are produced | `POST .../remote.imagemosaic` |
| [Directory harvest](#bulk-harvest-of-a-server-local-directory) (discouraged) | Granules on a filesystem volume mounted by every GeoServer pod | `POST .../external.imagemosaic` |
| [Externally managed index](#externally-managed-index) | Thousands of granules fed by a batch pipeline (SQL or `gdaltindex`) | plain SQL, no per-granule REST call |

## Incremental adds through the REST API

Create an empty mosaic store from a configuration zip, add granules by URL, then publish the coverage. This is the workflow most sites already run against vanilla GeoServer and the recommended one for a cloud-native deployment: granules stay on object storage and no shared filesystem is involved.

### 1. Author the configuration files

`indexer.properties` declares a COG mosaic over a PostGIS-backed index:

```properties
Cog=true
CogRangeReader=it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader
Schema=*the_geom:Polygon,location:String
CanBeEmpty=true
Name=scenes
```

`Name` must match the coverage name you will publish (`$COVERAGE`).

`datastore.properties` points the granule index at PostGIS. Two options:

**Option 1: JNDI datasource.** The connection is defined once in the deployment configuration (Docker Compose environment or Kubernetes config map) and the zip holds no credentials:

```properties
SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory
jndiReferenceName=java:comp/env/jdbc/postgis
Loose\ bbox=true
preparedStatements=false
```

**Option 2: direct connection parameters.** Self-contained, at the cost of credentials inside the store configuration:

```properties
SPI=org.geotools.data.postgis.PostgisNGDataStoreFactory
host=postgis.example.com
port=5432
database=mosaics
schema=public
user=geoserver
passwd=secret
Loose\ bbox=true
preparedStatements=false
```

### 2. Create the workspace and the empty store

```bash
zip mosaic.zip indexer.properties datastore.properties

curl -sS -u "$AUTH" -X POST -H "Content-type: text/xml" \
    -d "<workspace><name>$WORKSPACE</name></workspace>" "$REST/workspaces"

curl -sS -u "$AUTH" -X PUT -H "Content-type: application/zip" \
    --data-binary @mosaic.zip \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/file.imagemosaic?configure=none"
```

`configure=none` creates the store without publishing a coverage; expect `201 Created`. The zip holds configuration only, never raster data.

### 3. Add granules by URL

One `POST` per granule, with the granule URL as a plain text body:

```bash
curl -sS -u "$AUTH" -X POST -H "Content-type: text/plain" \
    -d "https://storage.example.com/cogs/scene_001.tif" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/remote.imagemosaic"
```

Expect `202 Accepted`. The `remote.imagemosaic` endpoint accepts `http(s)://` URLs; `external.imagemosaic` only resolves server-local paths.

### 4. Publish the coverage

Publish after at least one granule exists, then the mosaic has bounds to compute:

```bash
curl -sS -u "$AUTH" -X POST -H "Content-type: text/xml" \
    -d "<coverage><name>$COVERAGE</name><nativeName>$COVERAGE</nativeName><enabled>true</enabled></coverage>" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/coverages"
```

Expect `201 Created`.

### 5. Verify

List the index and render the layer. On a scaled deployment the GetMap request is served by a `wms` pod, a different container than the one that handled the REST calls; a successful render is the cross-pod check:

```bash
curl -sS -u "$AUTH" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/coverages/$COVERAGE/index/granules.json"

curl -sS -u "$AUTH" -o map.png \
    "$GEOSERVER_URL/wms?service=WMS&version=1.1.1&request=GetMap&layers=$WORKSPACE:$COVERAGE&bbox=-180,-90,180,90&width=512&height=256&srs=EPSG:4326&format=image/png"
```

Adjust `bbox` to your data. New granules added with step 3 appear on every pod on the next request.

### 6. Remove granules

Delete index entries by ECQL filter. `purge=none` keeps the COG on object storage and removes only the index row:

```bash
curl -sS -u "$AUTH" -X DELETE -G \
    --data-urlencode "filter=location='https://storage.example.com/cogs/scene_001.tif'" \
    --data-urlencode "purge=none" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/coverages/$COVERAGE/index/granules.xml"
```

!!! note
    The filter value contains an ECQL `location='...'` expression whose own `=` must be percent-encoded, or the URI parser rejects the request. `curl -G --data-urlencode` builds the query string correctly instead of passing it raw.

## Bulk harvest of a server-local directory

!!! warning "Discouraged for cloud-native deployments"
    The ImageMosaic plugin cannot harvest a directory from remote object storage; `external.imagemosaic` resolves only server-local paths. Harvesting therefore forces the granule files onto a volume mounted by every GeoServer pod, the shared filesystem the pgconfig backend exists to avoid. Prefer [incremental adds](#incremental-adds-through-the-rest-api) or an [externally managed index](#externally-managed-index). Use this approach only when the granules already live on a shared volume.

The workflow matches the incremental one except for the indexer configuration and the harvest call. In `indexer.properties`, drop the `Cog` entries (local GeoTIFFs are read directly rather than over HTTP range requests) and store absolute paths, readable from every pod that mounts the volume:

```properties
Schema=*the_geom:Polygon,location:String
CanBeEmpty=true
AbsolutePath=true
Name=scenes
```

Create the empty store as in [step 2](#2-create-the-workspace-and-the-empty-store), then harvest the whole directory in one call:

```bash
curl -sS -u "$AUTH" -X POST -H "Content-type: text/plain" \
    -d "/mnt/granules" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/external.imagemosaic"
```

Expect `202 Accepted`. Publish and verify as in steps [4](#4-publish-the-coverage) and [5](#5-verify).

## Externally managed index

For a catalog of thousands of granules fed by a batch pipeline, populate the PostGIS index table directly and create the mosaic store once over it with `UseExistingSchema=true`. GeoServer reads the index live: a granule added later with a plain SQL `INSERT` appears on every pod with no REST call.

### 1. Create and populate the index table

The table layout must match the `Schema` declared in `indexer.properties`. The `location` column holds each granule's URL:

```sql
CREATE SCHEMA IF NOT EXISTS mosaics;
CREATE TABLE mosaics.scenes (
  fid      serial PRIMARY KEY,
  the_geom geometry(Polygon, 4326),
  location varchar
);
CREATE INDEX scenes_gix ON mosaics.scenes USING GIST (the_geom);

INSERT INTO mosaics.scenes (the_geom, location) VALUES
  (ST_MakeEnvelope(0, 0, 10, 10, 4326), 'https://storage.example.com/cogs/scene_001.tif');
```

GDAL's `gdaltindex` can build the same table directly from files exposed through a URL, using the `/vsicurl` virtual file system, as an alternative to hand-written SQL.

### 2. Create the store over the existing index

`indexer.properties` adds `UseExistingSchema=true`, telling the mosaic plugin the index table already exists and is authoritative:

```properties
Cog=true
CogRangeReader=it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader
Schema=*the_geom:Polygon,location:String
CanBeEmpty=true
UseExistingSchema=true
Name=scenes
```

`datastore.properties` must point at the schema holding the table (`schema=mosaics` with the direct-parameters option above). Create the store and publish the coverage exactly as in steps [2](#2-create-the-workspace-and-the-empty-store) and [4](#4-publish-the-coverage) of the incremental workflow.

### 3. Add granules with SQL

```sql
INSERT INTO mosaics.scenes (the_geom, location) VALUES
  (ST_MakeEnvelope(10, 0, 20, 10, 4326), 'https://storage.example.com/cogs/scene_002.tif');
```

The next map request, on any pod, includes the new granule.

!!! warning
    Do not issue a REST harvest (`remote.imagemosaic` or `external.imagemosaic`) against a store created with `UseExistingSchema=true`: it is a silent no-op. The index table is the single source of truth; write to it directly.

## How this works on pgconfig

gt-imagemosaic writes small configuration files (`<coverage>.properties`, `sample_image.dat`) next to the store with raw file I/O. On the pgconfig backend those files match the [`db-backed-file-patterns`](../configuration/index.md#imagemosaic-stores-across-pods) whitelist and are stored in the config database, materialized into each pod's local cache on demand. That is what makes a store created through one pod's REST call render correctly on every other pod.

## Run it end to end

The repository ships a self-contained demo at [`examples/cog-imagemosaic-pgconfig/`](https://github.com/geoserver/geoserver-cloud/tree/main/examples/cog-imagemosaic-pgconfig): a Docker Compose stack with an S3-compatible object store serving world imagery COGs, and one runnable script per approach (`usecase_A_incremental_rest.sh`, `usecase_B_directory_harvest.sh`, `usecase_C_external_index.sh`), each publishing its result in a separate workspace.
