# GeoWebCache distributed seeding

Design goals:

- Implement tile cache seeding/truncation tasks in a distributed way across all instances in a cluster.
- Do not introduce new technologies nor complicated application configuration (e.g. Hazelcast), instead,
  rely on what we already have: spring-cloud-bus for distributed events and LockProvider for distributed locking of string keys.
- Allow joining of worker instances to process already running jobs.
- Resiliency to worker instances leaving the cluster, either smoothly or abruptly (e.g. JVM killed),
  even the instance that launched the job.
- Even distribution of tile/metatile work across worker threads on each worker instance. Instead of having a user defined
thread pool size for each job, have all jobs working off a single thread pool, processing tiles in an evenly distributed
way as jobs get started/terminated.


* `CacheJobManager` publishes events to start/cancel jobs. `CacheJobInfo` instances will eventually
be available in the `CacheJobRegistry` as it receives job events.

* `CacheJobEventProcessor` processes Job events. Start jobs
