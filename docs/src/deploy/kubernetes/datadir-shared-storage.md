# Data directory on shared storage

With the `datadir` catalog backend, every GeoServer Cloud service mounts the same data directory, usually from a `ReadWriteMany` volume. The services coordinate catalog and configuration changes through byte-range locks on one file, `<data directory>/.filelocks/resourcestore.locks`. A service that finds a lock held by another one keeps trying for two minutes before it fails the operation.

The shared file system therefore has to support byte-range locks between hosts, as NFS and SMB shares do.

## Connection losses

A network file system can lose its connection to the server. Each service keeps the lock file open while it runs. If locking on it fails because the connection was lost, the service reopens the lock file and tries again. A lock that the file system still attributes to a service that no longer holds it is released the same way, once that service has failed to take it for a while. Neither case needs a restart.

## SMB shares

On SMB shares, put the lock file on a volume of its own, mounted with the `nolease` and `nopersistenthandles` options, and keep the rest of the data directory on its current options. After a connection loss the server then drops the locks held through the old connection, instead of keeping them until the service stops. Keep leases on the rest of the data directory: they let the client cache what it reads.

All services must use the same lock file. To move it to its own volume, stop every service that mounts the data directory, change the volumes, and start them again.
