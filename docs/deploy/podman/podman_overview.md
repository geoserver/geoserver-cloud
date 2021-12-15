# GeoServer Cloud with podman

## Why podman?

[Podman](https://podman.io/)'s aim is to be a secure open source replacement for Docker
and not depending on one daemon with root rights in order to prevent being a single point failure.

A further design goal is being able to run containers with only user privileges.

https://cloudnweb.dev/2019/10/heres-why-podman-is-more-secured-than-docker-devsecops/

Additionally Podman can create and play Kubernetes files for easing migration to k8s:

https://www.redhat.com/sysadmin/compose-kubernetes-podman

Integration with systemd including dependencies is also a feature of podman.

https://fedoramagazine.org/auto-updating-podman-containers-with-systemd/

For futher information please have a look at the project page:

https://podman.io/whatis.html


## Required packages for different distributions

### RHEL 8.5 / CentOS 8.5 (Stream) and newer

```bash
sudo dnf -y install podman podman-plugins
```

### Ubuntu

Follow Podman's [Ubuntu installation instructions](https://podman.io/getting-started/installation)
to install from the Kubic repo. The default version installed through `apt install podman`
(currently `3.0.1`) may be too old.

```bash
. /etc/os-release
echo "deb https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/ /" | sudo tee /etc/apt/sources.list.d/devel:kubic:libcontainers:stable.list
curl -L "https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/Release.key" | sudo apt-key add -
sudo apt-get update
sudo apt-get -y upgrade
sudo apt-get -y install podman
```

```
podman version
Version:      3.4.2
API Version:  3.4.2
Go Version:   go1.16.6
Built:        Wed Dec 31 21:00:00 1969
OS/Arch:      linux/amd64
```

## Running Geoserver Cloud

### Traditional way

* [Manual](traditional/manual/podman.md)
* Script (needs to be done)

### Pods (k8s style)

* Manual (needs to be done)
* Script (needs to be done)
