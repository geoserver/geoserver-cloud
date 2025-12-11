## GeoServer Cloud build instructions

Requirements:

 * Java >= 25 JDK
 * [Maven](https://maven.apache.org/) >= `3.9.11` (included with the `mvnw` maven wrapper in the root folder)
 * A recent [Docker](https://docs.docker.com/engine/install/) version with the [Compose](https://docs.docker.com/compose/) plugin.

### Clone the repository

Clone the repository, including submodules. Alternatively, replace the repository URL by your own fork's:

```shell
git clone --recurse-submodules git@github.com:geoserver/geoserver-cloud.git
```

The `--recurse-submodules` argument is necessary for `clone` to populate the `config/` directory from the [geoserver/geoserver-cloud-config](https://github.com/geoserver/geoserver-cloud-config) repository, which is in turn required to build the Docker images.

If you already cloned the repository without it, initialize the submodule with

```shell
cd geoserver-cloud
git submodule update --init --recursive
```

## Build

The `make` command from the project root directory will compile, test, and install all the project artifacts, and build the GeoServer-Cloud Docker images. So for a full build just run:

```bash
make
```

To build without running tests, run

```bash
make install
```

and run tests with

```bash
make test
```

finally clean the build with

```bash
make clean
```

### Build the docker images

As mentioned above, a `make` with no arguments will build everything.

But to build only the docker images, run:

```bash
make build-image
```

This runs the `build-base-images`, `build-image-infrastructure`, and `build-image-geoserver` targets,
which you can also run individually during development depending on your needs. Usually,
you'd run `make build-image-geoserver` to speed up the process when made a change and want
to test the geoserver containers, without having to rebuild the base and infra images.

#### Multiplatform (amd64/arm64) images

The "build and push" github actions job will create `linux/amd64` and `linux/arm64` multi-platform images by running

```bash
make build-image-multiplatform
```

This target assumes `buildx` is set up as an alias for `docker build` and there's a build runner that supports both platforms.

Building multi-platform images requires pushing to the container registry, so the `build-image-multiplatform` target
will run `docker compose build --push` with the appropriate `*-multiplatform.yml` compose file from the `docker-build` directory.

If you want to build the multi-platform images yourself:

* Install [QEmu](https://www.qemu.org/download/)
* Run the following command to create a `buildx` builder:

```bash
docker buildx create --name gscloud-builder --driver docker-container --bootstrap --use
```

In order to push the images to your own dockerhub account, use the `REPOSITORY` environment variable, for example:

```bash
REPOSITORY=groldan make build-image-multiplatform
```

will build and push `groldan/<image-name>:<version>` tagged images instead of the default `geoservercloud/<image-name>:<version>` ones.


Finally, to remove the multi-platform builder, run

```bash
docker buildx stop gscloud-builder
docker buildx rm gscloud-builder
```

### Note on custom upstream GeoServer version

*GeoServer Cloud* depends on a custom GeoServer branch, `gscloud/gs_version/integration`, which contains patches to upstream GeoServer that have not yet been integrated into the mainstream `main` branch.

Additionally, this branch changes the artifact versions (e.g. from `2.28.0` to `2.28.0.0`), to avoid confusing maven if you also work with vanilla GeoServer, and to avoid your IDE downloading the latest `2.28-SNAPSHOT` artifacts from the OsGeo maven repository, overriding your local maven repository ones, and having confusing compilation errors that would require re-building the branch we need.

The `gscloud/gs_version/integration` branch is checked out as a submodule on the [camptocamp/geoserver-cloud-geoserver](https://github.com/camptocamp/geoserver-cloud-geoserver) repository, which publishes the custom geoserver maven artifacts to the Github maven package registry.

The root pom adds this additional maven repository, so no further action is required for the geoserver-cloud build to use those dependencies.


