#!/bin/bash
# Deletes a granule from the index by location filter (purge=none keeps the
# COG in object storage; the index row is removed).
set -e
cd "$(dirname "$0")" && source ./common.sh

location="${1:?usage: $0 <granule-location-url>}"

# The filter value contains an ECQL "location='...'" expression, whose own "="
# must be percent-encoded or the gateway's URI parser rejects the whole request;
# -G --data-urlencode builds the query string instead of taking it raw.
gs DELETE -G \
    --data-urlencode "filter=location='$location'" \
    --data-urlencode "purge=none" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/coverages/$COVERAGE/index/granules.xml" \
    -o /dev/null -w "delete granule: %{http_code} (expect 200)\n"
