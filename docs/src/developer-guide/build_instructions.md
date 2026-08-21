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

The `--recurse-submodules` argument is necessary for `clone` to populate two submodules:

* `config/`, from the [geoserver/geoserver-cloud-config](https://github.com/geoserver/geoserver-cloud-config) repository, which is in turn required to build the Docker images.
* `geoserver_submodule/geoserver`, a shallow checkout of the customized GeoServer branch from the [camptocamp/geoserver](https://github.com/camptocamp/geoserver) repository, required to build the upstream GeoServer dependencies (see [below](#note-on-custom-upstream-geoserver-version)).

If you already cloned the repository without it, initialize the submodule with

```shell
cd geoserver-cloud
git submodule update --init --recursive
```

## Build

The `make` command from the project root directory will build the customized GeoServer dependencies, compile, test, and install all the project artifacts, and build the GeoServer-Cloud Docker images. So for a full build just run:

```bash
make
```

To build the customized GeoServer dependencies alone, run

```bash
make deps
```

This is required once after cloning, and again whenever the `geoserver_submodule/geoserver` submodule is updated. It installs the custom GeoServer Maven artifacts into the local Maven repository, where the rest of the build resolves them from, since they are not published to any public Maven repository.

To build the project without running tests, run

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

The "build and push" GitHub Actions workflow (`.github/workflows/build-and-push.yaml`) creates `linux/amd64` and `linux/arm64`
multi-platform images by building each architecture natively on its own runner, then stitching the results into
multi-arch manifests using `docker buildx imagetools create`.

This avoids QEMU emulation, which is critical for JVM AOT cache correctness — AOT caches generated under
emulation produce invalid instructions on real hardware.

The workflow structure is:

1. **build-base-images** — builds the 3 base images on each platform (amd64 + arm64), pushes, and extracts digests
2. **stitch-base-manifests** — creates multi-arch manifests for base images so downstream `FROM` references resolve correctly
3. **build-infrastructure-images** + **build-geoserver-images** — build downstream images on both platforms in parallel
4. **stitch-manifests** — creates multi-arch manifests for all downstream images
5. **sign-images** — signs release images with Cosign (only on git tags)

For local development, `make build-image` builds single-platform images for your native architecture, which is
sufficient for testing.

### Note on custom upstream GeoServer version

*GeoServer Cloud* depends on a custom GeoServer branch, `gscloud/gs_version/integration`, which contains patches to upstream GeoServer that have not yet been integrated into the mainstream `main` branch.

Additionally, this branch changes the artifact versions (e.g. from `2.28.0` to `2.28.0.0`), to avoid confusing maven if you also work with vanilla GeoServer, and to avoid your IDE downloading the latest `2.28-SNAPSHOT` artifacts from the OsGeo maven repository, overriding your local maven repository ones, and having confusing compilation errors that would require re-building the branch we need.

The `gscloud/gs_version/integration` branch of the [camptocamp/geoserver](https://github.com/camptocamp/geoserver) repository is checked out as a shallow submodule under `geoserver_submodule/geoserver`, and each *GeoServer Cloud* branch pins the submodule branch matching its GeoServer version.

Run `make deps` to build it and install the custom GeoServer Maven artifacts into the local Maven repository. The CI workflows do the same through the `.github/actions/geoserver-artifacts` composite action, which caches the built artifacts keyed on the submodule commit.


