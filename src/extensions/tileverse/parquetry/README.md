# Parquetry extension

Integrates the parquetry plugin of the
[tileverse-geoserver](https://github.com/tileverse-io/tileverse-geoserver) family, backed by
[parquetry](https://github.com/tileverse-io/parquetry)'s `parquetry-geotools` module:

- the "Parquet" datastore: read-only Parquet and GeoParquet files and directories on local, S3,
  Azure, GCS, or HTTP storage, with its store-edit panel in the WebUI;
- the `geoparquet` and `arrow-ipc` WFS GetFeature output formats, served by the WFS service and
  listed by the WebUI's Layer Preview and WFS service page.

This extension is independent from the DuckDB-based `gs-geoparquet` community module (display name
"GeoParquet") shipped with the vector-formats extension; both can run side by side.

## Configuration

```yaml
geoserver:
  extension:
    tileverse:
      parquetry:
        enabled: true # umbrella switch
        parquet:
          enabled: true # the "Parquet" datastore
        output-formats:
          geoparquet:
            enabled: true
          arrow-ipc:
            enabled: true
```

`enabled` is the umbrella switch: when `false`, every feature is off regardless of its own toggle.
When `true`, each feature follows its own toggle. All default to `true`.

`parquet.enabled` also drives the `"[Parquet]"` entry in the `geotools.data.filtering.vector-formats`
defaults (see `config/geoserver.yml`). Because the datastore factory is discovered through SPI rather
than as a Spring bean, `ParquetryContextInitializer` also makes it unavailable when the plugin or the
store is off, by setting the `parquetry.geotools.geoparquet.disabled` system property unless it is
already set.

The `gs-parquetry` module status is always present and reports the plugin enabled while the umbrella
switch is on and at least one feature is on. Each enabled feature adds its own status row from the
plugin: `gs-parquetry-geoparquet` (WebUI only), `gs-parquetry-wfs-geoparquet` and
`gs-parquetry-wfs-arrow-ipc`.

## Java requirements

The parquetry jars are Java 25 class files, and part of the GeoParquet read path uses Java 25
preview APIs. The JVM must run with `--enable-preview`. The `gs-cloud-base-jre` image sets it
through `JAVA_TOOL_OPTIONS`, and the Maven test forks set it through their `argLine`.

GeoServer Cloud itself stays on Java 21 bytecode in this series, because Spring 5.3 cannot parse
newer class files. For the same reason, this extension imports the plugin's plain beans from its
`applicationContext.xml` by name, and it never imports a plugin class as a `@Configuration`.

## Stac-GeoParquet and Iceberg datastores

`parquetry-geotools` also provides Stac-GeoParquet and Apache Iceberg
datastores, not yet production ready. `ParquetryContextInitializer` makes them
unavailable by setting these system properties at startup, unless already set:

- `parquetry.geotools.stac-geoparquet.disabled=true`
- `parquetry.geotools.iceberg.disabled=true`

Launching with an explicit `-Dparquetry.geotools.iceberg.disabled=false` (or
the stac-geoparquet equivalent) re-enables the corresponding store.
