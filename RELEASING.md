# Releasing GeoServer Cloud 2.28.x

A release is a commit setting the release version, tagged `v2.28.5.3`. Pushing the signed tag runs the `Build and Push Docker images` workflow. The workflow builds, publishes and signs the Docker images under that version. The GitHub release comes afterwards, with its notes edited by hand, and the branch goes back to a snapshot version.

This is the checklist for the 2.28.x series, released from this branch, `release/2.28.x`. The guide behind it is [Releasing](https://geoserver.org/geoserver-cloud/developer-guide/releasing/) in the developer guide, kept on `main` as [docs/src/developer-guide/releasing.md](https://github.com/geoserver/geoserver-cloud/blob/main/docs/src/developer-guide/releasing.md). Read it before your first release, and when something fails. It is written for the 3.x series. The 2.28.x series differs in three places:

- **Version numbers.** A release has four numbers from the start: `2.28.5.0` is the first release on GeoServer 2.28.5, `2.28.5.1` and `2.28.5.2` the GeoServer Cloud patch releases on it. Between releases the branch stays at the series snapshot, `2.28.5-SNAPSHOT`, and each release commit is reverted. The branch moves to `2.28.6-SNAPSHOT` with the upgrade to GeoServer 2.28.6, in a commit of its own.
- **The fork version.** The fork branch of a release is named after the GeoServer Cloud version and sets that version in the GeoServer poms, with no `-CLOUD` suffix: `gscloud/2.28.5.2` has `<version>2.28.5.2</version>`, and `gs.version` is `2.28.5.2`. Each release gets its own branch, patch releases included. `gt.version` stays `34-SNAPSHOT`.
- **The gateway pom.** `src/apps/infrastructure/gateway/pom.xml` has a `<version>` of its own. The release commit sets it to the release version, and the revert puts the series snapshot back.

## Before you start

- The fork has a permanent branch for the release, created from the head of `gscloud/2.28.x/integration` with the poms set to the release version. Make it in a full GeoServer clone with the fork as the remote `camptocamp`, see [Prepare the fork](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#prepare-the-fork):

    ```bash
    git fetch camptocamp
    git switch -c gscloud/2.28.5.3 camptocamp/gscloud/2.28.x/integration
    git grep -l '2.28.5-SNAPSHOT' -- '*pom.xml' | xargs sed -i 's/2\.28\.5-SNAPSHOT/2.28.5.3/g'
    git commit -a -m "Set version to 2.28.5.3"
    git push camptocamp gscloud/2.28.5.3
    ```

    On macOS, use `sed -i ''`.

- You can push branches and tags to `geoserver/geoserver-cloud`, called `upstream` below, and have a signing key configured in git.
- JDK 25, GNU make, Docker and, optionally, `gh`.

## A GeoServer Cloud patch release: `2.28.5.3`

1. Create the release branch and pin the GeoServer:

    ```bash
    git fetch upstream
    git switch -c r2.28.5.3 upstream/release/2.28.x
    git config -f .gitmodules submodule.geoserver.branch gscloud/2.28.5.3
    make deps-sync
    make deps
    ```

2. Set the version: `pom.xml` `<revision>2.28.5.3</revision>`, `src/pom.xml` `<gs.version>2.28.5.3</gs.version>`, `src/apps/infrastructure/gateway/pom.xml` `<version>2.28.5.3</version>`, `compose/.env` `TAG=2.28.5.3`. `gt.version` stays `34-SNAPSHOT`. Build and commit:

    ```bash
    make install
    git add .gitmodules geoserver_submodule/geoserver pom.xml src/pom.xml src/apps/infrastructure/gateway/pom.xml compose/.env
    git commit -m "Set version to 2.28.5.3"
    ```

3. Push `r2.28.5.3` to your fork and open a pull request against `release/2.28.x`. Wait for it to be green. Do not merge it.

4. Tag, push the tag and wait for the workflow:

    ```bash
    git tag -s v2.28.5.3 -m "GeoServer Cloud 2.28.5.3"
    git push upstream v2.28.5.3
    gh run watch
    ```

5. Create the GitHub release with generated notes, then edit the notes on GitHub, see [Create the GitHub release](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#create-the-github-release). `--notes-start-tag` names the previous release of the series, and `--latest=false` leaves the Latest mark on the 3.x series:

    ```bash
    gh release create v2.28.5.3 --verify-tag --generate-notes --notes-start-tag v2.28.5.2 --latest=false --title "GeoServer Cloud 2.28.5.3"
    ```

6. Fast-forward the branch. Do not push yet: a pushed head at the release version publishes the release images again.

    ```bash
    git switch release/2.28.x
    git merge --ff-only r2.28.5.3
    ```

7. Revert the release commit and push. The branch is back at `2.28.5-SNAPSHOT` on the integration branch:

    ```bash
    git revert --no-edit v2.28.5.3
    git push upstream release/2.28.x
    ```

8. Tag the config repository:

    ```bash
    git submodule update --init config
    git -C config tag -s v2.28.5.3 -m "GeoServer Cloud 2.28.5.3"
    git -C config push origin v2.28.5.3
    ```

9. Update the docs on `main`, in their own commit: the image tags and config label of the maintenance examples under `docs/src/deploy/docker-compose/maintenance/`. See [Update the docs](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#update-the-docs).

## A new GeoServer version: `2.28.6.0`

The branch moves to the new snapshot first, in a commit of its own, when upstream `2.28.x` goes to `2.28.6-SNAPSHOT`: the fork branches `gscloud/2.28.x/base`, `gscloud/2.28.x/version` and `gscloud/2.28.x/integration` are rebuilt at `2.28.6-SNAPSHOT`, and on this branch `pom.xml`, `src/pom.xml`, the gateway pom and `compose/.env` go from `2.28.5-SNAPSHOT` to `2.28.6-SNAPSHOT`, committed as "Upgrade to GeoServer 2.28.6-SNAPSHOT". Once GeoServer 2.28.6 is released, sync the integration branch to it and create `gscloud/2.28.6.0` from its head as under [Before you start](#before-you-start), with `2.28.6-SNAPSHOT` replaced by `2.28.6.0`.

1. Create the release branch and pin the GeoServer:

    ```bash
    git fetch upstream
    git switch -c r2.28.6.0 upstream/release/2.28.x
    git config -f .gitmodules submodule.geoserver.branch gscloud/2.28.6.0
    make deps-sync
    make deps
    ```

2. Set the version: `pom.xml` `<revision>2.28.6.0</revision>`, `src/pom.xml` `<gs.version>2.28.6.0</gs.version>`, `src/apps/infrastructure/gateway/pom.xml` `<version>2.28.6.0</version>`, `compose/.env` `TAG=2.28.6.0`. `gt.version` stays `34-SNAPSHOT`. Build and commit:

    ```bash
    make install
    git add .gitmodules geoserver_submodule/geoserver pom.xml src/pom.xml src/apps/infrastructure/gateway/pom.xml compose/.env
    git commit -m "Set version to 2.28.6.0"
    ```

3. Push `r2.28.6.0` to your fork and open a pull request against `release/2.28.x`. Wait for it to be green. Do not merge it.

4. Tag, push the tag and wait for the workflow:

    ```bash
    git tag -s v2.28.6.0 -m "GeoServer Cloud 2.28.6.0"
    git push upstream v2.28.6.0
    gh run watch
    ```

5. Create the GitHub release with generated notes, then edit the notes on GitHub. `--notes-start-tag` names the previous release of the series, and `--latest=false` leaves the Latest mark on the 3.x series:

    ```bash
    gh release create v2.28.6.0 --verify-tag --generate-notes --notes-start-tag v2.28.5.3 --latest=false --title "GeoServer Cloud 2.28.6.0"
    ```

6. Fast-forward the branch. Do not push yet: a pushed head at the release version publishes the release images again.

    ```bash
    git switch release/2.28.x
    git merge --ff-only r2.28.6.0
    ```

7. Revert the release commit and push. The branch is back at `2.28.6-SNAPSHOT` on the integration branch:

    ```bash
    git revert --no-edit v2.28.6.0
    git push upstream release/2.28.x
    ```

8. Tag the config repository:

    ```bash
    git submodule update --init config
    git -C config tag -s v2.28.6.0 -m "GeoServer Cloud 2.28.6.0"
    git -C config push origin v2.28.6.0
    ```

9. Update the docs on `main`, in their own commit: the image tags and config label of the maintenance examples under `docs/src/deploy/docker-compose/maintenance/`. See [Update the docs](https://geoserver.org/geoserver-cloud/developer-guide/releasing/#update-the-docs).
