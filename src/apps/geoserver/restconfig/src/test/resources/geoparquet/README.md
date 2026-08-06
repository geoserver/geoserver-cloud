# GeoParquet test data

`disputed_areas.parquet` and `populated_places.parquet` are Natural Earth
themes (public domain), copied from the
[parquetry-geoserver](https://github.com/tileverse-io/parquetry-geoserver)
demo, where they are converted with GDAL from the Natural Earth GeoPackage
that ships with the GeoServer release.

Used by `ParquetryRestApiIT`, which serves them as an S3 bucket through an
s3proxy testcontainer.
