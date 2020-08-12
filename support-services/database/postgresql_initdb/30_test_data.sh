#!/bin/sh

pg_restore -c -U $POSTGRES_USER -d $POSTGRES_DB /docker-entrypoint-initdb.d/30_test_data.dump
