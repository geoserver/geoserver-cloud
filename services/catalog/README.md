# GeoServer Catalog micro-service

Microservice that exposes the geoserver catalog through a web interface (RESTful so far) to other microservices, in order to abstract out the microservices that require access to the catalog from the actual catalog backend and implementation.

--
REVISIT:
- Looks like commons-logging is blocking the netty eventloop threads. Nonetheless achieving a 17k reqs/s throughput with 2 worker threads and the default Netty event loop threads (2 * cores)
- Reduce/make configurable the number of threads in the Netty event loop. We should be using just a few, but the default of 2*cores is high when there are many cores