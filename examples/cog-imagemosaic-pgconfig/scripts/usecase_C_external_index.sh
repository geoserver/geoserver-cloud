#!/bin/bash
# Use case C end to end: externally managed PostGIS index (UseExistingSchema).
set -e
cd "$(dirname "$0")" && source ./common.sh

export WORKSPACE=usecase_c
EXT_STORE="landshallow_external"
EXT_COVERAGE="landshallow_external"
SCHEMA="mosaic_external"

describe "Use case C: externally managed PostGIS index at scale" <<TXT
Populates a PostGIS index table directly with SQL, then creates the mosaic
store once over it with UseExistingSchema=true and publishes the layer.
GeoServer reads the index live: a granule added later with a plain SQL INSERT
appears on every pod with no REST call. This is the pattern for a catalog of
thousands of granules fed by a batch pipeline (SQL or gdaltindex). Granules
stay on object storage; no shared filesystem is involved. Do NOT issue a REST
harvest against such a store: with UseExistingSchema=true it is a silent no-op.

Workspace: $WORKSPACE     Layer: $WORKSPACE:$EXT_COVERAGE
TXT

psql_c() {
    docker compose exec -T geodatabase psql -U geoserver -d postgis -c "$1"
}

echo ">>> 1. create and populate the index table (three of four quadrants)"
psql_c "CREATE SCHEMA IF NOT EXISTS $SCHEMA"
psql_c "CREATE TABLE IF NOT EXISTS $SCHEMA.$EXT_COVERAGE (
          fid serial PRIMARY KEY,
          the_geom geometry(Polygon, 4326),
          location varchar)"
psql_c "CREATE INDEX IF NOT EXISTS ${EXT_COVERAGE}_gix ON $SCHEMA.$EXT_COVERAGE USING GIST (the_geom)"
psql_c "INSERT INTO $SCHEMA.$EXT_COVERAGE (the_geom, location) VALUES
  (ST_MakeEnvelope(   0,   0, 180,  90, 4326), '$S3_INTERNAL_BASE/land_shallow_topo_21600_NE_cog.tif'),
  (ST_MakeEnvelope(-180,   0,   0,  90, 4326), '$S3_INTERNAL_BASE/land_shallow_topo_21600_NW_cog.tif'),
  (ST_MakeEnvelope(   0, -90, 180,   0, 4326), '$S3_INTERNAL_BASE/land_shallow_topo_21600_SE_cog.tif')"

echo ">>> 2. create the store over the populated index (UseExistingSchema=true)"
workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT
cat > "$workdir/indexer.properties" <<EOF
Cog=true
CogRangeReader=it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader
Schema=*the_geom:Polygon,location:String
CanBeEmpty=true
UseExistingSchema=true
Name=$EXT_COVERAGE
EOF
cat > "$workdir/datastore.properties" <<EOF
SPI=org.geotools.data.postgis.PostgisNGDataStoreFactory
host=geodatabase
port=5432
database=postgis
schema=$SCHEMA
user=geoserver
passwd=geoserver
Loose\\ bbox=true
preparedStatements=false
EOF
(cd "$workdir" && zip -q mosaic.zip indexer.properties datastore.properties)

gs POST -H "Content-type: text/xml" -d "<workspace><name>$WORKSPACE</name></workspace>" "$REST/workspaces" \
    -o /dev/null -w "create workspace $WORKSPACE: %{http_code}\n" || true
gs PUT -H "Content-type: application/zip" --data-binary @"$workdir/mosaic.zip" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$EXT_STORE/file.imagemosaic?configure=none" \
    -o /dev/null -w "create store: %{http_code} (expect 201)\n"
gs POST -H "Content-type: text/xml" \
    -d "<coverage><name>$EXT_COVERAGE</name><nativeName>$EXT_COVERAGE</nativeName><enabled>true</enabled></coverage>" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$EXT_STORE/coverages" \
    -o /dev/null -w "publish coverage: %{http_code} (expect 201)\n"

echo ">>> 3. render (three quadrants)"
curl -sS -u "$AUTH" -o /tmp/${WORKSPACE}_3.png -w "GetMap: %{http_code}\n" \
    "$GEOSERVER_URL/wms?service=WMS&version=1.1.1&request=GetMap&layers=$WORKSPACE:$EXT_COVERAGE&bbox=-180,-90,180,90&width=512&height=256&srs=EPSG:4326&format=image/png"

echo ">>> 4. add the SW quadrant with plain SQL (no REST call)"
psql_c "INSERT INTO $SCHEMA.$EXT_COVERAGE (the_geom, location) VALUES
  (ST_MakeEnvelope(-180, -90, 0, 0, 4326), '$S3_INTERNAL_BASE/land_shallow_topo_21600_SW_cog.tif')"

echo ">>> 5. render again: the new granule appears on all pods immediately"
curl -sS -u "$AUTH" -o /tmp/${WORKSPACE}_4.png -w "GetMap: %{http_code}\n" \
    "$GEOSERVER_URL/wms?service=WMS&version=1.1.1&request=GetMap&layers=$WORKSPACE:$EXT_COVERAGE&bbox=-180,-90,0,0&width=256&height=128&srs=EPSG:4326&format=image/png"
file /tmp/${WORKSPACE}_3.png /tmp/${WORKSPACE}_4.png

echo
echo "Use case C complete. Layer $WORKSPACE:$EXT_COVERAGE is published."
