# GeoWebCache microservice

The GWC microservice implements a standalone GWC instance but with the goodies of the
traditional geoserver-integrated GWC. 

This is different than a standalone GWC application, as deployed using only the typical
geowebcache .war file, in that all GeoServer layers, can automatically be set up for tile
caching if so configured, and that by integrating with GeoServer, generating tile images
uses GeoServer's internal WMS machinery instead of issuing HTTP WMS GetMap requests. Hence
this microservice carries over `gs-wms` dependency, in order to produce the tile images,
although it shall not expose a WMS service.

Being split out of the geoserver monolith application, this microservice must hence rely on
distributed events in order to react to changes in GeoServer that should affect the tile
caches. For example:

- When a WFS transaction changes a layer that has an associated tile-layer, GWC truncates
that layer's caches at the bounds affected by the transaction;

