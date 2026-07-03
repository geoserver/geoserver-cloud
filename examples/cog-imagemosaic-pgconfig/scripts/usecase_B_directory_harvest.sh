#!/bin/bash
# Use case B end to end: bulk harvest of a server-local directory.
# Discouraged for cloud-native deployments (see the banner below).
set -e
cd "$(dirname "$0")" && source ./common.sh

export WORKSPACE=usecase_b
export STORE=landshallow_b
export COVERAGE=landshallow_b

# Directory holding the granules, as seen from inside the GeoServer pods. The
# compose file bind-mounts ./cogs read-only at this path on every GeoServer
# service (see geoserver_template).
LOCAL_GRANULE_DIR=/opt/cogs

describe "Use case B: bulk harvest of a server-local directory (discouraged)" <<TXT
Creates an empty ImageMosaic, then harvests every granule under a local
directory in a single call (POST external.imagemosaic <dir>), and publishes
the layer.

Discouraged for a cloud-native pgconfig deployment: the ImageMosaic plugin
cannot harvest a directory from remote object storage; external.imagemosaic
resolves only server-local paths. That forces the COG files onto a volume
shared by every GeoServer pod (this compose bind-mounts ./cogs at
$LOCAL_GRANULE_DIR for the demo), the shared filesystem the pgconfig backend
exists to avoid. Prefer use case A (remote adds) or use case C (externally
managed index) instead. This runner shows that even the shared-mount workflow
now propagates its mosaic configuration across pods on pgconfig.

Workspace: $WORKSPACE     Layer: $WORKSPACE:$COVERAGE
TXT

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT

# Local GeoTIFF granules, read directly rather than over HTTP range requests;
# no Cog flag. AbsolutePath=true stores each granule's full path, readable
# from every pod that mounts the shared directory.
cat > "$workdir/indexer.properties" <<EOF
Schema=*the_geom:Polygon,location:String
CanBeEmpty=true
AbsolutePath=true
Name=$COVERAGE
EOF

cat > "$workdir/datastore.properties" <<EOF
SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory
jndiReferenceName=java:comp/env/jdbc/postgis
Loose\\ bbox=true
preparedStatements=false
EOF

(cd "$workdir" && zip -q mosaic.zip indexer.properties datastore.properties)

echo ">>> create workspace and empty mosaic store"
gs POST -H "Content-type: text/xml" -d "<workspace><name>$WORKSPACE</name></workspace>" "$REST/workspaces" \
    -o /dev/null -w "create workspace $WORKSPACE: %{http_code}\n" || true
gs PUT -H "Content-type: application/zip" --data-binary @"$workdir/mosaic.zip" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/file.imagemosaic?configure=none" \
    -o /dev/null -w "create empty mosaic $STORE: %{http_code} (expect 201)\n"

echo ">>> harvest the whole directory in one call"
gs POST -H "Content-type: text/plain" -d "$LOCAL_GRANULE_DIR" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/external.imagemosaic" \
    -o /dev/null -w "harvest $LOCAL_GRANULE_DIR: %{http_code} (expect 202)\n"

./03_publish_coverage.sh
./04_verify.sh

echo
echo "Use case B complete. Layer $WORKSPACE:$COVERAGE is published."
