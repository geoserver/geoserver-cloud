#!/bin/bash

createdb -O geoserver -U geoserver postgis
psql -U geoserver -d postgis -c "create extension postgis;"
