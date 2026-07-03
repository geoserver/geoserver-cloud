#!/bin/bash
# Use case A step 3: publish the coverage (after at least one granule exists).
set -e
cd "$(dirname "$0")" && source ./common.sh

echo "available coverages:"
gs GET "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/coverages.xml?list=all"
echo

gs POST -H "Content-type: text/xml" \
    -d "<coverage><name>$COVERAGE</name><nativeName>$COVERAGE</nativeName><enabled>true</enabled></coverage>" \
    "$REST/workspaces/$WORKSPACE/coveragestores/$STORE/coverages" \
    -o /dev/null -w "publish coverage $COVERAGE: %{http_code} (expect 201)\n"
