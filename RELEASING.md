# Releasing GeoServer Cloud

A release is a commit setting the release version, tagged `v3.1.0`. Pushing the signed tag runs the `Build and Push Docker images` workflow. The workflow builds, publishes and signs the Docker images under that version. The GitHub release comes afterwards, with its notes edited by hand, and the branch goes back to a snapshot version.

This is the checklist. The guide behind it is [docs/src/developer-guide/releasing.md](docs/src/developer-guide/releasing.md), published at [geoserver.org/geoserver-cloud/developer-guide/releasing](https://geoserver.org/geoserver-cloud/developer-guide/releasing/). Read it before your first release, and when something fails.

## Before you start

- The fork has a permanent branch for the release at the required tip, created from the head of the integration branch of the series with the poms set to the release versions. For `3.1.0`: `gscloud/3.1.0` from `gscloud/3.1.x/integration`, with `3.1.0-CLOUD`, GeoTools `36.0` and GeoWebCache `2.1.0`. For `3.0.2`: `gscloud/3.0.2` from `gscloud/3.0.x/integration`, with `3.0.2-CLOUD`, GeoTools `35.2` and GeoWebCache `2.0.2`. See [Prepare the fork](docs/src/developer-guide/releasing.md#prepare-the-fork).
- You can push branches and tags to `geoserver/geoserver-cloud`, called `upstream` below, and have a signing key configured in git.
- JDK 25, GNU make, Docker and, optionally, `gh`.

## A new series: `3.1.0` from `main`

1. Create the release branch and pin the GeoServer:

    ```bash
    git fetch upstream
    git switch -c r3.1.0 upstream/main
    git config -f .gitmodules submodule.geoserver.branch gscloud/3.1.0
    make deps-sync
    make deps
    ```

2. Set the version: `pom.xml` `<revision>3.1.0</revision>`, `src/pom.xml` `<gs.version>3.1.0-CLOUD</gs.version>` and `<gt.version>36.0</gt.version>`, `compose/.env` `TAG=3.1.0`. Build and commit:

    ```bash
    make install
    git add .gitmodules geoserver_submodule/geoserver pom.xml src/pom.xml compose/.env
    git commit -m "Set version to 3.1.0"
    ```

3. Push `r3.1.0` to your fork and open a pull request against `main`. Wait for it to be green. Do not merge it.

4. Tag, push the tag and wait for the workflow:

    ```bash
    git tag -s v3.1.0 -m "GeoServer Cloud 3.1.0"
    git push upstream v3.1.0
    gh run watch
    ```

5. Create the GitHub release with generated notes, then edit the notes on GitHub, see [Create the GitHub release](docs/src/developer-guide/releasing.md#create-the-github-release):

    ```bash
    gh release create v3.1.0 --verify-tag --generate-notes --title "GeoServer Cloud 3.1.0"
    ```

6. Fast-forward `main` and create the series branch. Do not push yet: a pushed head at the release version publishes the release images again.

    ```bash
    git switch main
    git merge --ff-only r3.1.0
    git branch release/3.1.x v3.1.0
    ```

7. Move `release/3.1.x` to `3.1.1-SNAPSHOT`. In the fork first: `gscloud/3.1.x/version` and `gscloud/3.1.x/integration` at `3.1.1-CLOUD-SNAPSHOT`. Then on the branch: `.gitmodules` back on `gscloud/3.1.x/integration`, `make deps-sync`, `make deps`, `pom.xml` `3.1.1-SNAPSHOT`, `src/pom.xml` `3.1.1-CLOUD-SNAPSHOT` and `36-SNAPSHOT`, `compose/.env` `TAG=3.1.1-SNAPSHOT`.

    ```bash
    git commit -a -m "Set version to 3.1.1-SNAPSHOT"
    git push upstream release/3.1.x
    ```

8. Move `main` to `3.2.0-SNAPSHOT` the same way, with the `gscloud/3.2.x` fork branches, `3.2.0-CLOUD-SNAPSHOT` and `37-SNAPSHOT`, committed as "Set version to 3.2.0-SNAPSHOT". Until the 3.2.x fork branches exist, revert instead:

    ```bash
    git revert --no-edit v3.1.0
    git push upstream main
    ```

9. Tag the config repository:

    ```bash
    git submodule update --init config
    git -C config tag -s v3.1.0 -m "GeoServer Cloud 3.1.0"
    git -C config push origin v3.1.0
    ```

10. Update the docs on `main`, in their own commit: the compose examples under `docs/src/deploy/docker-compose/stable/` and `maintenance/`, the Status section of `README.md`, `docs/dockerhub/*.md` and the helm `appVersion`. See [Update the docs](docs/src/developer-guide/releasing.md#update-the-docs).

## A new GeoServer version in the same series: `3.0.2` from `release/3.0.x`

1. Create the release branch and pin the GeoServer:

    ```bash
    git fetch upstream
    git switch -c r3.0.2 upstream/release/3.0.x
    git config -f .gitmodules submodule.geoserver.branch gscloud/3.0.2
    make deps-sync
    make deps
    ```

2. Set the version: `pom.xml` `<revision>3.0.2</revision>`, `src/pom.xml` `<gs.version>3.0.2-CLOUD</gs.version>` and `<gt.version>35.2</gt.version>`, `compose/.env` `TAG=3.0.2`. Build and commit:

    ```bash
    make install
    git add .gitmodules geoserver_submodule/geoserver pom.xml src/pom.xml compose/.env
    git commit -m "Set version to 3.0.2"
    ```

3. Push `r3.0.2` to your fork and open a pull request against `release/3.0.x`. Wait for it to be green. Do not merge it.

4. Tag, push the tag and wait for the workflow:

    ```bash
    git tag -s v3.0.2 -m "GeoServer Cloud 3.0.2"
    git push upstream v3.0.2
    gh run watch
    ```

5. Create the GitHub release with generated notes, then edit the notes on GitHub. `--notes-start-tag` names the previous release of the series. Add `--latest=false` when a newer series has a release already:

    ```bash
    gh release create v3.0.2 --verify-tag --generate-notes --notes-start-tag v3.0.1 --title "GeoServer Cloud 3.0.2"
    ```

6. Fast-forward the branch. Do not push yet: a pushed head at the release version publishes the release images again.

    ```bash
    git switch release/3.0.x
    git merge --ff-only r3.0.2
    ```

7. Move `release/3.0.x` to `3.0.3-SNAPSHOT`. In the fork first: `gscloud/3.0.x/version` and `gscloud/3.0.x/integration` at `3.0.3-CLOUD-SNAPSHOT`. Then on the branch: `.gitmodules` back on `gscloud/3.0.x/integration`, `make deps-sync`, `make deps`, `pom.xml` `3.0.3-SNAPSHOT`, `src/pom.xml` `3.0.3-CLOUD-SNAPSHOT` and `35-SNAPSHOT`, `compose/.env` `TAG=3.0.3-SNAPSHOT`.

    ```bash
    git commit -a -m "Set version to 3.0.3-SNAPSHOT"
    git push upstream release/3.0.x
    ```

8. Tag the config repository:

    ```bash
    git submodule update --init config
    git -C config tag -s v3.0.2 -m "GeoServer Cloud 3.0.2"
    git -C config push origin v3.0.2
    ```

9. Update the docs on `main`, in their own commit: the image tags and config label of the series under `docs/src/deploy/docker-compose/`, the Status section of `README.md` when this is the newest release, `docs/dockerhub/*.md` and the helm `appVersion`. See [Update the docs](docs/src/developer-guide/releasing.md#update-the-docs).

A GeoServer Cloud patch release, `3.0.2.1`, follows the same steps with the existing `gscloud/3.0.2` fork branch, `<gs.version>3.0.2-CLOUD</gs.version>` and `<gt.version>35.2</gt.version>`. Afterwards, revert the release commit instead of setting the next snapshot, `git revert --no-edit v3.0.2.1`, then push.

Milestones and release candidates are covered in the guide.
