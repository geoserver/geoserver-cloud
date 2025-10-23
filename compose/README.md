# Docker Compose for Development

This directory contains docker-compose files intended for **development and testing** only.

> For production deployments, follow the [Quick Start](https://geoserver.org/geoserver-cloud/#quick-start) guide in the user documentation.

## Running the Docker Compositions

GeoServer Cloud supports different catalog and configuration backends. Choose the one that fits your needs:

### Data Directory Backend

The data directory backend uses a traditional shared directory for catalog and configuration. All containers share the same directory.

Run with:

```bash
./datadir up -d
```

This is equivalent to:

```bash
docker compose -f compose.yml -f catalog-datadir.yml up -d
```

### PostgreSQL Backend

The PostgreSQL backend stores catalog and configuration in a PostgreSQL database. This is the recommended backend for production deployments.

Run with:

```bash
./pgconfig up -d
```

This is equivalent to:

```bash
docker compose -f compose.yml -f catalog-pgconfig.yml up -d
```

### Additional Docker Compose Parameters

You can pass additional docker compose parameters to either script. For example:

```bash
./datadir ps
./pgconfig logs -f gateway
./datadir down
```

## Debugging from an IDE

For debugging a specific service from your IDE while the rest of the cluster runs in Docker, use the `localports.yml` compose file. This file exposes the discovery and config service ports, allowing your local application to join the cluster.

### Setup for Debugging

1. **Start the composition with local ports exposed:**

   For pgconfig backend:
   ```bash
   ./pgconfig -f localports.yml up -d
   ```

   For datadir backend:
   ```bash
   ./datadir -f localports.yml up -d
   ```

2. **Configure your IDE run configuration:**

   Enable the `local` profile plus the appropriate backend profile (`datadir` or `pgconfig`).

   For example, to debug the WMS service with pgconfig backend:
   - **Spring Profiles**: `local,pgconfig`
   - **Main Class**: `org.geoserver.cloud.wms.app.WmsApplication`

   For the datadir backend, you also need to set the environment variable:
   - **Environment Variable**: `GEOSERVER_DATA_DIR=/Users/groldan/git/geoserver/geoserver-cloud/compose/catalog-datadir/`

   Adjust the path to point to your local `compose/catalog-datadir/` directory.

3. **Stop the container for the service you're debugging:**

   To ensure the gateway directs requests only to your IDE instance (rather than load balancing between it and the container), stop the container:

   ```bash
   ./pgconfig down wms
   ```

   or

   ```bash
   ./datadir down wms
   ```

4. **Launch the application from your IDE:**

   Start the application with the configured run configuration. The application will:
   - Connect to the discovery service at `http://localhost:8761/eureka`
   - Retrieve configuration from the config service
   - Join the cluster and be discoverable by the gateway
   - Be available for debugging with breakpoints and step-through

### Service Ports in Local Mode

When running with the `local` profile, each service uses a hard-coded port:

* `wfs-service`: [9101](http://localhost:9101)
* `wms-service`: [9102](http://localhost:9102)
* `wcs-service`: [9103](http://localhost:9103)
* `wps-service`: [9104](http://localhost:9104)
* `restconfig-v1`: [9105](http://localhost:9105)
* `web-ui`: [9106](http://localhost:9106)

### Accessing GeoServer

Access all services through the gateway at [http://localhost:9090/geoserver/cloud/](http://localhost:9090/geoserver/cloud/)

Test with:

```bash
curl "http://localhost:9090/geoserver/cloud/ows?request=getcapabilities&service={WMS,WFS,WCS}"
curl -u admin:geoserver "http://localhost:9090/geoserver/cloud/rest/workspaces.json"
```

### Example: Eclipse Debugging Workflow

1. Start the pgconfig composition with local ports:
   ```bash
   ./pgconfig -f localports.yml up -d
   ```

2. Stop the WMS container:
   ```bash
   ./pgconfig down wms
   ```

3. In Eclipse, create a run configuration for the WMS application:
   - Main class: `org.geoserver.cloud.wms.app.WmsApplication`
   - VM arguments: `-Dspring.profiles.active=local,pgconfig`
   - Set breakpoints in your code

4. Run the application from Eclipse. The WMS service will start on port 9102, join the cluster, and you can debug it while all other services run in containers.

5. Access WMS requests through the gateway, and Eclipse will hit your breakpoints.

## Additional Information

For more details on the project architecture and development practices, see the [Developer's Guide](../docs/develop/index.md).

