# GeoParquet and Arrow IPC over WFS

This guide shows how to download vector layers as [GeoParquet](https://geoparquet.org/) files or [Apache Arrow](https://arrow.apache.org/) IPC streams through WFS GetFeature, using the two output formats added by the parquetry extension, and how to read the results back with DuckDB and Python. Both formats work for any vector layer, regardless of its store: a PostGIS table, a shapefile, or a GeoParquet file published as in [GeoParquet through the REST API](geoparquet-rest-api.md).

## Prerequisites

- GeoServer Cloud 3.1.0 or newer. Both formats are on by default; see [Enabling and disabling the formats](#enabling-and-disabling-the-formats).
- A published vector layer. The examples use the `countries` layer from the GeoParquet REST guide; any layer works.
- To read the results back: [DuckDB](https://duckdb.org/) 1.3 or newer, or Python with `pyarrow` and, for GeoParquet, `geopandas`.

Every command below uses these variables; set them to match your deployment:

```bash
export GEOSERVER_URL="https://geoserver.example.com/geoserver/cloud"
export WORKSPACE=naturalearth
export LAYER=countries
```

## Confirm the formats are offered

The capabilities document lists every GetFeature output format. Look for the two names and their MIME type aliases:

```bash
curl -sS "$GEOSERVER_URL/$WORKSPACE/wfs?service=WFS&version=2.0.0&request=GetCapabilities" \
    | grep -oE 'geoparquet|arrow-ipc|application/vnd\.apache\.[a-z.]+' | sort -u
```

```
application/vnd.apache.arrow.stream
application/vnd.apache.parquet
arrow-ipc
geoparquet
```

Either the short name or the MIME type works as the `outputFormat` value. If neither appears, see [Troubleshooting](#troubleshooting).

## Download a layer as GeoParquet

```bash
curl -sS -o "$LAYER.parquet" \
    "$GEOSERVER_URL/$WORKSPACE/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=$WORKSPACE:$LAYER&outputFormat=geoparquet"
```

The response is one Parquet file with the standard GeoParquet `geo` metadata in its footer, and geometries stored as WKB. Coordinates are written x then y, longitude first for geographic reference systems, as required by GeoParquet, regardless of the axis order used by the WFS version for GML. The file is served as an attachment named `<layer>.parquet`.

The usual GetFeature parameters apply: `count` and `startIndex` page through large layers, `bbox` and `cql_filter` select features, `propertyName` picks columns, and `srsName` reprojects.

Read it back with DuckDB. With the `spatial` extension loaded, the geometry column is decoded as geometries:

```bash
duckdb -c "load spatial;
    describe select * from read_parquet('$LAYER.parquet');
    select count(*) from read_parquet('$LAYER.parquet');"
```

Or with GeoPandas:

```python
import geopandas

countries = geopandas.read_parquet("countries.parquet")
print(countries.crs, len(countries))
```

## Tune the GeoParquet output

GeoServer's `format_options` parameter passes write options to the encoder, as `name:value` pairs separated by `;`. Option names are matched without regard to case.

| Option | Values | Default |
|--------|--------|---------|
| `compression` | `zstd`, `snappy`, `gzip`, `lz4`, `none` | `zstd` |
| `rowGroupSize` | a positive row count | the encoder's default |
| `parquetVersion` | `1.1`, `2.0` | `2.0` |
| `FILENAME` | the attachment file name | `<layer>.parquet` |

For example, Snappy compression and smaller row groups, for a consumer reading a few columns at a time:

```bash
curl -sS -o "$LAYER-snappy.parquet" \
    "$GEOSERVER_URL/$WORKSPACE/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=$WORKSPACE:$LAYER&outputFormat=geoparquet&format_options=compression:snappy;rowGroupSize:50000"
```

An option value rejected by the encoder fails the whole request with a WFS `InvalidParameterValue` exception naming `format_options`, rather than producing a file with a silently ignored option.

## Download a layer as Arrow IPC

```bash
curl -sS -o "$LAYER.arrows" \
    "$GEOSERVER_URL/$WORKSPACE/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=$WORKSPACE:$LAYER&outputFormat=arrow-ipc"
```

The response is an Arrow IPC stream, not the random-access Arrow file format. GeoServer writes it one record batch at a time as features are read, with no need to know the size up front, which makes it the better choice for piping a large layer straight into a consumer. It is served as an attachment named `<layer>.arrows`; `FILENAME` in `format_options` renames it, and the format takes no other options.

Read the stream with pyarrow:

```python
import pyarrow as pa

with pa.ipc.open_stream("countries.arrows") as reader:
    table = reader.read_all()
print(table.schema)
print(table.num_rows)
```

Geometry columns are WKB tagged with the [GeoArrow](https://geoarrow.org/) `geoarrow.wkb` extension type. DuckDB reads Arrow IPC through the `nanoarrow` community extension, and with `spatial` loaded as well those columns are read as geometries, and the `ST_*` functions work on them as on the GeoParquet file:

```bash
duckdb -c "install nanoarrow from community; load nanoarrow; load spatial;
    select count(*) from read_arrow('$LAYER.arrows');"
```

Since the response is a stream, it need not touch the disk at all:

```bash
curl -sS "$GEOSERVER_URL/$WORKSPACE/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=$WORKSPACE:$LAYER&outputFormat=arrow-ipc" \
    | duckdb -c "load nanoarrow; load spatial; select count(*) from read_arrow('/dev/stdin');"
```

## Limits

Both formats serve one query per request. A GetFeature request naming more than one type in `typeNames` fails with a WFS exception instead of returning a partial result; send one request per layer. Complex features, such as those published through app-schema, are not supported.

## Enabling and disabling the formats

Each format has its own switch, and the parquetry extension has an umbrella switch that turns off the "Parquet" datastore and both formats together. All default to `true`:

```yaml
geoserver:
  extension:
    tileverse:
      parquetry:
        enabled: true
        output-formats:
          geoparquet:
            enabled: true
          arrow-ipc:
            enabled: true
```

These properties belong in the externalized configuration read by every service, `geoserver.yml` by default; see the [externalized configuration guide](../configuration/index.md). The `wfs` service answers the requests and the `webui` service lists the formats in Layer Preview and on the WFS service page, and both apply the same switches.

## Troubleshooting

**A format is missing from the capabilities document, or GetFeature answers `InvalidParameterValue` for `outputFormat`.** Its switch, or the umbrella switch, is off. In the web admin interface, About > Server Status > Modules shows a "Parquetry GeoParquet WFS Output Format" and a "Parquetry Arrow IPC WFS Output Format" row for each format that is on, and the "Parquetry Plugin" row reports disabled when the umbrella or every feature is off.

**GetFeature answers `InvalidParameterValue` for `format_options`.** A `compression` or `parquetVersion` value outside the table above, or a `rowGroupSize` that is not a positive integer.

**The request names several layers and fails.** Split it into one request per layer; see [Limits](#limits).
