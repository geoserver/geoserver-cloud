# GeoParquet through the REST API

This guide shows how to publish [GeoParquet](https://geoparquet.org/) files as vector layers through the REST API, using the "Parquet" datastore contributed by the parquetry extension. The store is read-only and reads GeoParquet from the local filesystem, Amazon S3 (and S3-compatible services), Azure Blob Storage, Google Cloud Storage, or plain HTTP servers.

The parquetry store is independent from the DuckDB-based "GeoParquet" community store; both can run side by side, and existing stores of either type are unaffected by the other.

## Prerequisites

- GeoServer Cloud 3.1.0 or newer. The parquetry extension is enabled by default (`geoserver.extension.parquetry.enabled: true`).
- GeoParquet files reachable from the GeoServer pods: a mounted volume or object storage.
- Admin credentials for the REST API.

Every command below uses these variables; set them to match your deployment:

```bash
export GEOSERVER_URL="https://geoserver.example.com/geoserver/cloud"
export REST="$GEOSERVER_URL/rest"
export AUTH="admin:geoserver"

export WORKSPACE=naturalearth
export STORE=ne
```

## Choosing the store layout

The `geoparquet` connection parameter accepts a single file, a directory, or a glob; `layer-grouping` controls how a directory maps to layers:

| `geoparquet` URI | `layer-grouping` | Result |
|------------------|------------------|--------|
| `file:///data/ne/countries.parquet` | not used | one layer from that file |
| `file:///data/ne/` | `file` | one layer per top-level `.parquet` file |
| `file:///data/ne/` | `merged` (default) | all files read as a single layer; files must share a schema |
| `file:///data/ne/*.parquet` | `merged` | the matching files read as a single layer |

A directory of per-theme files, one dataset per file (the Natural Earth layout: `countries.parquet`, `coastlines.parquet`, ...), wants `layer-grouping=file`. A partitioned dataset, many files with one schema, wants the default `merged`.

## Publish a directory of GeoParquet files

The walkthrough publishes a directory of per-theme files from a volume mounted on every pod, with one layer per file.

### 1. Create the workspace

```bash
curl -sS -u "$AUTH" -X POST -H "Content-type: text/xml" \
    -d "<workspace><name>$WORKSPACE</name></workspace>" "$REST/workspaces"
```

Expect `201 Created`.

### 2. Create the datastore

```bash
curl -sS -u "$AUTH" -X POST -H "Content-type: text/xml" -d @- \
    "$REST/workspaces/$WORKSPACE/datastores" <<EOF
<dataStore>
  <name>$STORE</name>
  <type>Parquet</type>
  <connectionParameters>
    <entry key="geoparquet">file:///data/ne/</entry>
    <entry key="layer-grouping">file</entry>
  </connectionParameters>
</dataStore>
EOF
```

Expect `201 Created`. The path must be readable from every GeoServer pod; on Kubernetes that means a volume mounted by all vector-serving services.

### 3. Discover the available feature types

```bash
curl -sS -u "$AUTH" \
    "$REST/workspaces/$WORKSPACE/datastores/$STORE/featuretypes.json?list=available"
```

The response lists one feature type per `.parquet` file, named after the file:

```json
{"list":{"string":["boundary_lines","coastlines","countries","disputed_areas","populated_places"]}}
```

### 4. Publish the layers

One `POST` per feature type from step 3:

```bash
curl -sS -u "$AUTH" -X POST -H "Content-type: text/xml" \
    -d "<featureType><name>countries</name></featureType>" \
    "$REST/workspaces/$WORKSPACE/datastores/$STORE/featuretypes"
```

Expect `201 Created`. The schema, CRS, and bounds come from the GeoParquet metadata; no manual bounding box computation is needed.

### 5. Verify

Read features over WFS and render a map over WMS. On a scaled deployment these are served by `wfs` and `wms` pods, different containers than the one that handled the REST calls; a successful response is the cross-pod check:

```bash
curl -sS "$GEOSERVER_URL/$WORKSPACE/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=$WORKSPACE:countries&count=5&outputFormat=application/json"

curl -sS -o map.png \
    "$GEOSERVER_URL/$WORKSPACE/wms?service=WMS&version=1.1.1&request=GetMap&layers=$WORKSPACE:countries&bbox=-180,-90,180,90&width=1000&height=500&srs=EPSG:4326&format=image/png"
```

## The same store over S3

Only the datastore creation changes: the `geoparquet` URI points at the bucket and the `storage.*` parameters configure the backend. Steps 3 to 5 are identical.

```bash
curl -sS -u "$AUTH" -X POST -H "Content-type: text/xml" -d @- \
    "$REST/workspaces/$WORKSPACE/datastores" <<EOF
<dataStore>
  <name>${STORE}-s3</name>
  <type>Parquet</type>
  <connectionParameters>
    <entry key="geoparquet">s3://my-bucket/ne/</entry>
    <entry key="layer-grouping">file</entry>
    <entry key="storage.provider">s3</entry>
    <entry key="storage.s3.region">us-east-1</entry>
    <entry key="storage.s3.use-default-credentials-provider">true</entry>
  </connectionParameters>
</dataStore>
EOF
```

With `use-default-credentials-provider=true` the pods authenticate through the AWS default credential chain: environment variables, web identity token (IRSA on EKS), the shared credentials file, or the instance profile. The store configuration holds no secrets. For a public bucket use `storage.s3.anonymous=true` instead; static keys through `storage.s3.aws-access-key-id` and `storage.s3.aws-secret-access-key` also work, at the cost of credentials inside the catalog.

!!! note "S3-compatible services"
    For MinIO, Cloudflare R2, DigitalOcean Spaces, or any other S3-compatible service, add `storage.s3.endpoint` with the service root (for example `http://minio:9000`). Setting an endpoint turns on path-style addressing automatically; `storage.s3.force-path-style` overrides that if needed.

Azure Blob Storage, Google Cloud Storage, and plain HTTP servers follow the same pattern: point `geoparquet` at the backend's URI form and add that backend's `storage.*` parameters from the [reference below](#storage-configuration-properties).

## Store connection parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `geoparquet` | yes | URI of a GeoParquet file, a directory, or a glob (`*.parquet`, `**/*.parquet`) |
| `layer-grouping` | no | For a directory URI: `merged` (default) reads all files as one layer, files must share a schema; `file` publishes each top-level `.parquet` file as its own layer |
| `fid` | no | Column to use as the feature id. Defaults to a column named `id` when present; otherwise feature ids are synthetic and `Id` filters are rejected |
| `namespace` | no | Feature type namespace; GeoServer fills it with the workspace namespace, leave it out |

## Storage configuration properties

`storage.provider` selects the storage backend: `s3`, `azure`, `gcs`, `http`, or `file`. When absent, the backend is inferred from the `geoparquet` URI; set it explicitly for `https://` URIs, which several backends can serve, and in general to make intent explicit.

`storage.caching.enabled` (default `true`) caches byte ranges in memory, cutting repeated reads against remote storage. It has no effect for local files.

### S3

URI forms: `s3://bucket/path/`, or an `https://` URL of the service.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `storage.s3.region` | from environment | Region for the AWS SDK; falls back to `aws.region`, `AWS_REGION`, the AWS config files, then EC2 metadata |
| `storage.s3.endpoint` | AWS | Service root of an S3-compatible service (for example `http://minio:9000`); implies path-style addressing |
| `storage.s3.force-path-style` | `true` | Path-style (`host/bucket/key`) vs virtual-hosted-style addressing |
| `storage.s3.use-default-credentials-provider` | `false` | Authenticate through the AWS default credential chain (env vars, web identity token, credentials file, container or instance credentials) |
| `storage.s3.default-credentials-profile` | `default` | Named profile to use with the default credential chain |
| `storage.s3.aws-access-key-id` | none | Static access key; requires the secret key |
| `storage.s3.aws-secret-access-key` | none | Static secret key; requires the access key |
| `storage.s3.anonymous` | `false` | Unsigned requests for public buckets; takes precedence over all other credential options |
| `storage.s3.requester-pays` | `false` | Send `x-amz-request-payer` for Requester Pays buckets; needs real credentials, incompatible with `anonymous` |

### Azure Blob Storage

URI forms: `az://account/container/path`, `https://<account>.blob.core.windows.net/container/path`, `abfs(s)://`.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `storage.azure.endpoint` | public Azure | Blob service endpoint override, for an emulator (`http://127.0.0.1:10000/devstoreaccount1` for Azurite), a sovereign cloud, or a custom domain |
| `storage.azure.connection-string` | none | Full connection string; takes precedence over account key, SAS token, and the default credential chain |
| `storage.azure.account-key` | none | Shared key for the account named in the URI |
| `storage.azure.sas-token` | none | Shared Access Signature granting delegated access |
| `storage.azure.anonymous` | `false` | No credential, for containers with public read access; takes precedence over all other credential options |
| `storage.azure.blob-name` | none | Explicit blob path, needed only for blobs in the root container |
| `storage.azure.max-retries` | `3` | Retry attempts for failed requests |
| `storage.azure.retry-delay` | `PT4S` | Initial retry backoff, ISO-8601 duration |
| `storage.azure.max-retry-delay` | `PT2M` | Upper bound on retry backoff |
| `storage.azure.try-timeout` | `PT60S` | Timeout per request attempt |

### Google Cloud Storage

URI form: `https://storage.googleapis.com/bucket/path`.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `storage.gcs.default-credentials-chain` | `false` | Authenticate through Application Default Credentials |
| `storage.gcs.project-id` | from environment | Project id; falls back to `GOOGLE_CLOUD_PROJECT` and the other standard sources |
| `storage.gcs.quota-project-id` | none | Project billed for quota purposes |
| `storage.gcs.user-project` | none | Project billed for Requester Pays buckets; requires authenticated credentials |
| `storage.gcs.endpoint` | public Google | Endpoint override for GCS-compatible servers (for example `fake-gcs-server`); authentication defaults to anonymous when set |

### HTTP

URI form: any `http(s)://` server that supports range requests.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `storage.http.timeout-millis` | `5000` | Connection timeout |
| `storage.http.username` / `storage.http.password` | none | HTTP Basic authentication, both required together |
| `storage.http.bearer-token` | none | `Authorization: Bearer` token for OAuth 2.0 or JWT |
| `storage.http.api-key-headername` / `storage.http.api-key` | none | Custom header authentication, both required together |
| `storage.http.api-key-value-prefix` | none | Prefix prepended to the API key value (for example `Token `) |
| `storage.http.trust-all-certificates` | `false` | Skip TLS certificate validation; development only |

### Local files

URI form: `file:///path/`.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `storage.file.idle-timeout` | `PT60S` | Idle time after which the underlying file channel is closed to release file descriptors; reopened on demand |

## Notes

- The store is read-only: layers serve WMS and WFS reads, and WFS transactions are rejected.
- The parquetry engine also provides Stac-GeoParquet and Apache Iceberg datastores, which are not yet production ready and ship disabled. They stay unavailable while the `parquetry.geotools.stac-geoparquet.disabled` and `parquetry.geotools.iceberg.disabled` system properties hold `true`; GeoServer Cloud sets both at startup unless the property is already set, and launching the services with an explicit `-Dparquetry.geotools.iceberg.disabled=false` (or the stac-geoparquet equivalent) re-enables the corresponding store.
