-- The acl schema in this database stores spatial rules using the "geometry"
-- type, so postgis must be enabled here too, not just on the "postgis" database
-- created below for the mosaic index.
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE DATABASE postgis OWNER geoserver;
