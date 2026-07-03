#!/bin/bash
# Lists index granules through REST and renders the layer through WMS.
# The GetMap request is served by the wms pod, a different container than the
# restconfig pod that handled the previous calls: this is the cross-pod check.
set -e
cd "$(dirname "$0")" && source ./common.sh

echo "granules in the index:"
gs GET "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/coverages/$COVERAGE/index/granules.json"
echo

out="/tmp/${WORKSPACE}_${COVERAGE}_getmap.png"
code=$(curl -sS -u "$AUTH" -o "$out" -w "%{http_code}" \
    "$GEOSERVER_URL/wms?service=WMS&version=1.1.1&request=GetMap&layers=$WORKSPACE:$COVERAGE&bbox=-180,-90,180,90&width=512&height=256&srs=EPSG:4326&format=image/png")
type=$(file -b "$out")
echo "GetMap: HTTP $code, $out: $type (expect PNG image data)"
