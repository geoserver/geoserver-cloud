#!/bin/bash
# Fetches the four demo COG granules into ./cogs (kept out of git).
set -e
cd "$(dirname "$0")/.."
mkdir -p cogs
base="https://test-data-cog-public.s3.amazonaws.com/public"
for tile in NE NW SE SW; do
    f="land_shallow_topo_21600_${tile}_cog.tif"
    if [ ! -f "cogs/$f" ]; then
        echo "Downloading $f"
        curl -fSLo "cogs/$f" "$base/$f"
    fi
done
echo "COGs ready in ./cogs"
