# Tileverse extensions

Integrations for the [tileverse-geoserver](https://github.com/tileverse-io/tileverse-geoserver)
plugin family, one extension per plugin:

- [parquetry](parquetry/README.md): the "Parquet" datastore and the GeoParquet and Arrow IPC WFS
  output formats, backed by [parquetry](https://github.com/tileverse-io/parquetry).

The plugins share the tileverse-storage backends (local, S3, Azure, GCS, HTTP), their store-edit
panels, and the `tileverse-geoserver.version` property in `src/pom.xml`. Each plugin's properties
live under `geoserver.extension.tileverse.<plugin>`.
