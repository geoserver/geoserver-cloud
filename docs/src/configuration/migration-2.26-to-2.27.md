# Migrating from GeoServer Cloud 2.26.x to 2.27.0.0

This guide outlines the key configuration changes when upgrading from GeoServer Cloud 2.26.x to 2.27.0.0.

> **Important Note**: All configuration changes described in this document are already incorporated as the new defaults in each GeoServer Docker image's `/etc/geoserver/geoserver.yml` externalized configuration file. You only need to adjust these settings if you were using custom configurations that differ from the defaults.

## Configuration Property Changes

### Extension Configuration Structure

The most significant change is the reorganization of extension configuration properties under a unified `geoserver.extension` namespace. This improves configuration organization and consistency.

#### Old Structure (2.26.x)

```yaml
geoserver:
  styling:
    css.enabled: true
    mapbox.enabled: true
  security:
    authkey: true
    jdbc: true
    ldap: true
    gateway-shared-auth:
      enabled: ${gateway.shared-auth:true}
      auto: true
      server: false
  wms:
    output-formats:
      vector-tiles:
        mapbox.enabled: true
        geojson.enabled: true
        topojson.enabled: true
```

#### New Structure (2.27.0.0)

```yaml
geoserver:
  extension:
    css-styling:
      enabled: true
    mapbox-styling:
      enabled: true
    vector-tiles:
      enabled: true
      mapbox: true
      geojson: true
      topojson: true
    dxf:
      enabled: true
    flatgeobuf:
      enabled: true
    importer:
      enabled: false
    security:
      auth-key:
        enabled: ${geoserver.security.authkey:true}
      jdbc:
        enabled: ${geoserver.security.jdbc:true}
      ldap:
        enabled: ${geoserver.security.ldap:true}
      geonode-oauth2:
        enabled: ${geoserver.security.geonode.enabled:true}
      environment-admin:
        enabled: ${geoserver.security.environment-admin.enabled:true}
      gateway-shared-auth:
        enabled: ${geoserver.security.gateway-shared-auth.enabled:true}
        auto: ${geoserver.security.gateway-shared-auth.auto:true}
```

### WebUI Extension Configuration

The WebUI service configuration for the importer extension has changed:

#### Old Structure (2.26.x)

```yaml
geoserver:
  web-ui:
    extensions:
      importer.enabled: ${webui.importer.enabled:false}
```

#### New Structure (2.27.0.0)

The importer extension is now managed through the common extension configuration:

```yaml
geoserver:
  extension:
    importer:
      enabled: false  # Set to true to enable
```

### Data Format Filtering

A new capability has been added for filtering vector and raster data formats. This allows you to control which data formats are available in your GeoServer instance:

```yaml
geotools:
  data:
    filtering:
      enabled: true
      vector-formats:
        "[PostGIS]": true
        "[Shapefile]": true
        "[GeoPackage]": true
        "[Oracle NG]": ${oracle.enabled:false}
        # Add more formats as needed
      raster-formats:
        "[GeoTIFF]": true
        "[ImageMosaic]": ${mosaic.enabled:true}
        "[ArcGrid]": false
        # Add more formats as needed
```

### Removed Properties

The following properties have been removed:

- `geoserver.backend.data-directory.parallel-loader`: This property is no longer used as the optimization is always enabled.

## Migration Steps

To migrate your custom configuration:

1. **No Action Required for Default Setups**: If you're using the Docker images with default configurations, no changes are needed.

2. **For Custom Configurations**:

   a. **Update Extension Configuration**: Move all extension-specific configuration properties under the `geoserver.extension` namespace, following the new structure.

   b. **Update Security Configuration**: Move security extension properties from `geoserver.security` to `geoserver.extension.security`.

   c. **Update WebUI Service Configuration**: If using the importer extension in WebUI, switch to the global extension configuration.

   d. **Add Data Format Filtering**: Consider adding data format filtering to limit which formats are available in your deployment.

   e. **Review Removed Properties**: Remove any references to removed properties.

## Example Migration

Here's a simplified example of migrating from the old to the new configuration format:

### Old Configuration (2.26.x)

```yaml
geoserver:
  styling:
    css.enabled: true
    mapbox.enabled: true
  security:
    authkey: true
    jdbc: true
    gateway-shared-auth:
      enabled: true
  wms:
    output-formats:
      vector-tiles:
        mapbox.enabled: true
```

### New Configuration (2.27.0.0)

```yaml
geoserver:
  extension:
    css-styling:
      enabled: true
    mapbox-styling:
      enabled: true
    vector-tiles:
      enabled: true
      mapbox: true
    security:
      auth-key:
        enabled: true
      jdbc:
        enabled: true
      gateway-shared-auth:
        enabled: true
```

## Additional Notes

- The new structure provides better categorization and organization of extensions
- Extension configuration is now consistent across all extension types
- Default values remain the same for most properties
- Several new extensions have been added: DXF, FlatGeobuf, and Importer
- Environment-based admin authentication is now configurable through `geoserver.extension.security.environment-admin`
- All these changes are automatically applied in the Docker images' default configuration files - manual changes are only needed if you're using custom configurations
