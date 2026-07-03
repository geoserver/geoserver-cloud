#!/bin/bash
# Use case A end to end: incremental granule adds through the vanilla REST API.
set -e
cd "$(dirname "$0")" && source ./common.sh

export WORKSPACE=usecase_a
export STORE=landshallow_a
export COVERAGE=landshallow_a

describe "Use case A: incremental adds through the REST API" <<TXT
Creates an empty ImageMosaic over a PostGIS index, adds four COG granules
one HTTP call at a time (POST remote.imagemosaic), then publishes and renders
the layer. Granules stay on object storage (the s3proxy container); no shared
filesystem is involved. This is the workflow most sites already run against
vanilla GeoServer, and the recommended one for a cloud-native deployment.

Workspace: $WORKSPACE     Layer: $WORKSPACE:$COVERAGE
TXT

./01_create_empty_mosaic.sh
./02_add_granules.sh
./03_publish_coverage.sh
./04_verify.sh

echo
echo "Use case A complete. Layer $WORKSPACE:$COVERAGE is published."
