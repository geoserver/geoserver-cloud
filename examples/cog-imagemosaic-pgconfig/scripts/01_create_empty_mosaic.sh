#!/bin/bash
# Use case A step 1: create an empty ImageMosaic store from a config zip.
# Vanilla API: PUT .../coveragestores/{store}/file.imagemosaic?configure=none
set -e
cd "$(dirname "$0")" && source ./common.sh

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT

cat > "$workdir/indexer.properties" <<EOF
Cog=true
CogRangeReader=it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader
Schema=*the_geom:Polygon,location:String
CanBeEmpty=true
Name=$COVERAGE
EOF

# JNDI: the datasource is defined once for all pods in the deployment config
# (docker compose env / Kubernetes config map), no credentials in this zip.
cat > "$workdir/datastore.properties" <<EOF
SPI=org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory
jndiReferenceName=java:comp/env/jdbc/postgis
Loose\\ bbox=true
preparedStatements=false
EOF

(cd "$workdir" && zip -q mosaic.zip indexer.properties datastore.properties)

gs POST -H "Content-type: text/xml" -d "<workspace><name>$WORKSPACE</name></workspace>" "$REST/workspaces" \
    -o /dev/null -w "create workspace $WORKSPACE: %{http_code}\n" || true

gs PUT -H "Content-type: application/zip" --data-binary @"$workdir/mosaic.zip" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/file.imagemosaic?configure=none" \
    -o /dev/null -w "create empty mosaic $STORE: %{http_code} (expect 201)\n"
