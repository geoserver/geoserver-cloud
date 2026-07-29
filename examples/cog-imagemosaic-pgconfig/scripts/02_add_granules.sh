#!/bin/bash
# Use case A step 2: add every granule by URL.
# Vanilla API: POST .../coveragestores/{store}/remote.imagemosaic (text/plain body).
# "remote" accepts http(s)/s3 URLs; "external" only accepts server-local paths.
#
# With no arguments, adds the four land_shallow_topo quadrants served by the
# s3proxy container. Pass one or more URLs to add a different set instead.
set -e
cd "$(dirname "$0")" && source ./common.sh

if [ "$#" -gt 0 ]; then
    granules=("$@")
else
    granules=(
        "$S3_INTERNAL_BASE/land_shallow_topo_21600_NE_cog.tif"
        "$S3_INTERNAL_BASE/land_shallow_topo_21600_NW_cog.tif"
        "$S3_INTERNAL_BASE/land_shallow_topo_21600_SE_cog.tif"
        "$S3_INTERNAL_BASE/land_shallow_topo_21600_SW_cog.tif"
    )
fi

for url in "${granules[@]}"; do
    echo "Adding granule $url"
    gs POST -H "Content-type: text/plain" -d "$url" \
        "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/remote.imagemosaic" \
        -o /dev/null -w "  %{http_code} (expect 202)\n"
done
