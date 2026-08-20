# User guide

Task-oriented guides for operating GeoServer Cloud. Each one walks through a
complete, real-world workflow against a running deployment.

Available guides:

- [Additional libraries and fonts](additional-libs-and-fonts.md): mount
  user-provided jar files and fonts into the containers, like JDBC drivers,
  GeoServer extensions, and fonts for map labeling, without rebuilding the
  images.
- [ImageMosaics through the REST API](imagemosaic-rest-api.md): publish an
  ImageMosaic of Cloud Optimized GeoTIFFs stored on object storage, using the
  same REST API calls that work against vanilla GeoServer.
- [GeoParquet through the REST API](geoparquet-rest-api.md): publish
  GeoParquet files from a local volume or object storage as vector layers,
  creating the datastores and layers through the REST API.
- [Monitoring control-flow](controlflow-monitoring.md): run the dev compose
  stack with Prometheus and Grafana, generate load, and watch request
  throttling live on the control-flow dashboard.
- [Control-flow metrics reference](controlflow-metrics-reference.md): the
  exported metrics, example PromQL queries, and the Grafana recipes the
  shipped dashboard is built from.

For deployment instructions see the [Deployment](../deploy/index.md) section,
and for service configuration the
[externalized configuration guide](../configuration/index.md).
