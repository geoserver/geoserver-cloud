#!/bin/sh
# created with
# pg_dump -h localhost -p 5432 -d geoserver_config -F c -v -Z 9  -U geoserver -f 30_test_data.dump

pg_restore -c -U $POSTGRES_USER -d $POSTGRES_DB /docker-entrypoint-initdb.d/30_test_data.dump
