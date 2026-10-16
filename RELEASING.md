# Releasing GeoServer Cloud 3.0.x

A release is a commit setting the release version, tagged `v3.0.2`. Pushing the signed tag runs the `Build and Push Docker images` workflow. The workflow builds, publishes and signs the Docker images under that version. The GitHub release comes afterwards, with its notes edited by hand, and the branch goes back to a snapshot version.

This is the checklist for the 3.0.x series, released from this branch, `release/3.0.x`. The guide behind it is [Releasing](https://geoserver.org/geoserver-cloud/developer-guide/releasing/) in the developer guide, kept on `main` as [docs/src/developer-guide/releasing.md](https://github.com/geoserver/geoserver-cloud/blob/main/docs/src/developer-guide/releasing.md). Read it before your first release, and when something fails.

## Before you start

- The fork has a permanent branch for the release at the required tip. For `3.0.2`: `gscloud/3.0.2`, created from the head of `gscloud/3.0.x/integration` with the poms set to `3.0.2-CLOUD`, GeoTools `35.2` and GeoWebCache `2.0.2`, see [Prepare the fork](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#prepare-the-fork). For `3.0.1.2`: the existing `gscloud/3.0.1`.
- You can push branches and tags to `geoserver/geoserver-cloud`, called `upstream` below, and have a signing key configured in git.
- JDK 25, GNU make, Docker and, optionally, `gh`.

## A new GeoServer version: `3.0.2`

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

5. Create the GitHub release with generated notes, then edit the notes on GitHub, see [Create the GitHub release](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#create-the-github-release). `--notes-start-tag` names the previous release of the series. Add `--latest=false` once the 3.1.x series has a release:

    ```bash
    gh release create v3.0.2 --verify-tag --generate-notes --notes-start-tag v3.0.1.1 --title "GeoServer Cloud 3.0.2"
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

9. Update the docs on `main`, in their own commit: the image tags and config label of the series under `docs/src/deploy/docker-compose/`, the Status section of `README.md` when this is the newest release, `docs/dockerhub/*.md` and the helm `appVersion`. See [Update the docs](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#update-the-docs).

## A GeoServer Cloud patch release: `3.0.1.2`

1. Create the release branch and pin the GeoServer of the release being patched:

    ```bash
    git fetch upstream
    git switch -c r3.0.1.2 upstream/release/3.0.x
    git config -f .gitmodules submodule.geoserver.branch gscloud/3.0.1
    make deps-sync
    make deps
    ```

2. Set the version: `pom.xml` `<revision>3.0.1.2</revision>`, `src/pom.xml` `<gs.version>3.0.1-CLOUD</gs.version>` and `<gt.version>35.1</gt.version>`, `compose/.env` `TAG=3.0.1.2`. Build and commit:

    ```bash
    make install
    git add .gitmodules geoserver_submodule/geoserver pom.xml src/pom.xml compose/.env
    git commit -m "Set version to 3.0.1.2"
    ```

3. Push `r3.0.1.2` to your fork and open a pull request against `release/3.0.x`. Wait for it to be green. Do not merge it.

4. Tag, push the tag and wait for the workflow:

    ```bash
    git tag -s v3.0.1.2 -m "GeoServer Cloud 3.0.1.2"
    git push upstream v3.0.1.2
    gh run watch
    ```

5. Create the GitHub release with generated notes, then edit the notes on GitHub. `--notes-start-tag` names the previous release of the series. Add `--latest=false` once the 3.1.x series has a release:

    ```bash
    gh release create v3.0.1.2 --verify-tag --generate-notes --notes-start-tag v3.0.1.1 --title "GeoServer Cloud 3.0.1.2"
    ```

6. Fast-forward the branch. Do not push yet: a pushed head at the release version publishes the release images again.

    ```bash
    git switch release/3.0.x
    git merge --ff-only r3.0.1.2
    ```

7. Revert the release commit and push. The branch is back at `3.0.2-SNAPSHOT` on the integration branch:

    ```bash
    git revert --no-edit v3.0.1.2
    git push upstream release/3.0.x
    ```

8. Tag the config repository:

    ```bash
    git submodule update --init config
    git -C config tag -s v3.0.1.2 -m "GeoServer Cloud 3.0.1.2"
    git -C config push origin v3.0.1.2
    ```

9. Update the docs on `main`, in their own commit: the image tags and config label of the series under `docs/src/deploy/docker-compose/`, the Status section of `README.md` when this is the newest release, `docs/dockerhub/*.md` and the helm `appVersion`. See [Update the docs](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#update-the-docs).
