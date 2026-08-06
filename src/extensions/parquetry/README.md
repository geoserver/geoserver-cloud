# Parquetry extension

Integrates the [parquetry-geoserver](https://github.com/tileverse-io/parquetry-geoserver)
plugin, exposing the "Parquet" datastore backed by
[parquetry](https://github.com/tileverse-io/parquetry)'s `parquetry-geotools`
module: read-only Parquet and GeoParquet files and directories on local, S3,
Azure, GCS, or HTTP storage.

This extension is independent from the DuckDB-based `gs-geoparquet` community
module (display name "GeoParquet") shipped with the vector-formats extension;
both can run side by side.

## Configuration

```yaml
geoserver:
  extension:
    parquetry:
      enabled: true # default
```

The flag also drives the `"[Parquet]"` entry in the
`geotools.data.filtering.vector-formats` defaults (see `config/geoserver.yml`)
and the enabled state reported by the `gs-parquetry` module status.

## Stac-GeoParquet and Iceberg datastores

`parquetry-geotools` also provides Stac-GeoParquet and Apache Iceberg
datastores, not yet production ready. `ParquetryContextInitializer` makes them
unavailable by setting these system properties at startup, unless already set:

- `parquetry.geotools.stac-geoparquet.disabled=true`
- `parquetry.geotools.iceberg.disabled=true`

Launching with an explicit `-Dparquetry.geotools.iceberg.disabled=false` (or
the stac-geoparquet equivalent) re-enables the corresponding store.
