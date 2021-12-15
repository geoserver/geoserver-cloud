## GeoServer Cloud with podman

### Why podman?

Podman's aim is to be a secure open source replacement for Docker and not depending on one daemon with root rights in order to prevent being a single point failure.

A further design goal is being able to run containers with only user privileges.

https://cloudnweb.dev/2019/10/heres-why-podman-is-more-secured-than-docker-devsecops/

Additionally podman can create and play Kubernetes files for easing migration to k8s:

https://www.redhat.com/sysadmin/compose-kubernetes-podman

Integration with systemd including dependencies is also a feature of podman.

https://fedoramagazine.org/auto-updating-podman-containers-with-systemd/

For futher informations please have a look at the project page:

https://podman.io/whatis.html


### Required packages for different distributions

#### RHEL 8.5 / CentOS 8.5 (Stream) and newer

```bash
sudo dnf -y install podman podman-plugins
```

### Running Geoserver Cloud

#### Traditional way

* [Manual](traditional/manual/podman.md)
* Script (needs to be done)

#### Pods (k8s style)

* Manual (needs to be done)
* Script (needs to be done)
