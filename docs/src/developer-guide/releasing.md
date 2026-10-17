# Releasing

A GeoServer Cloud release is a commit setting the release version, tagged `v3.1.0`. Pushing the tag runs the `Build and Push Docker images` workflow. The workflow builds, publishes and signs the Docker images under that version. The GitHub release comes afterwards, with its notes edited by hand, and the branch goes back to a snapshot version.

GeoServer Cloud does not publish Maven artifacts, but OCI compliant container images to the [geoservercloud](https://hub.docker.com/u/geoservercloud) organization on Docker Hub.

This page is for the release manager: a committer with push rights to `geoserver/geoserver-cloud` and to the GeoServer fork. It walks through a release in order, with `3.1.0` from `main` as the worked example. [Reference](#reference) describes the pieces involved, [Background](#background) the reasons behind the rules. [`RELEASING.md`](https://github.com/geoserver/geoserver-cloud/blob/main/RELEASING.md) at the root of the repository is the same procedure as a checklist.

## Release values

A release is named after the bundled GeoServer version: `3.1.0` runs GeoServer 3.1.0. GeoServer Cloud also ships fixes between GeoServer releases. Such a release keeps the GeoServer version and adds a fourth number: `3.1.0.1` is the first GeoServer Cloud patch release on GeoServer 3.1.0, `3.1.0.2` the second. Milestones and release candidates add a qualifier: `3.1.0-M1`, `3.1.0-RC1`.

The commands on this page are written for `3.1.0`. For another release, read the values of its column:

| | `3.1.0-M1` | `3.1.0` | `3.1.1` | `3.1.0.1` |
|---|---|---|---|---|
| Kind | milestone | new series | new GeoServer version | GeoServer Cloud patch release |
| Branch | `main` | `main` | `release/3.1.x` | `release/3.1.x` |
| Fork branch | `gscloud/3.1.0-M1`, new, at the integration head | `gscloud/3.1.0`, new | `gscloud/3.1.1`, new | `gscloud/3.1.0`, existing |
| `gs.version` | `3.1.0-CLOUD-SNAPSHOT` | `3.1.0-CLOUD` | `3.1.1-CLOUD` | `3.1.0-CLOUD` |
| `gt.version` | `36-SNAPSHOT` | `36.0` | `36.1` | `36.0` |
| GitHub release | pre-release | release | release | release |
| After the tag | revert the release commit | `release/3.1.x` created, next snapshots on both branches | next snapshot, `3.1.2-SNAPSHOT` | revert the release commit |

## Before you start

- The branch to release holds everything meant for the release. Anything merged after the release branch is created is not part of the release.
- You can push to `geoserver/geoserver-cloud`, branches and tags, and to `camptocamp/geoserver` for the fork work. The commands below call the GeoServer Cloud remote `upstream`. Use the name of your remote for it.
- A signing key configured in git, GPG or SSH. The release tag is signed, and so is the config repository tag.
- JDK 25, GNU make and Docker, for the GeoServer build and the images.
- The `gh` command, optional, to follow the workflow and create the GitHub release from a terminal.
- Docker Hub and cosign credentials are repository secrets read by the workflow. Nothing is needed locally.

## Prepare the fork

GeoServer Cloud builds against a patched GeoServer, from the [camptocamp/geoserver](https://github.com/camptocamp/geoserver) fork, checked out as the `geoserver_submodule/geoserver` submodule. A release pins the submodule to a permanent branch of the fork named after the release, with `gs.version` and `gt.version` set to match. [The fork branches](#the-fork-branches) describes the branch layout, [Why release branches of the fork are never force-pushed](#why-release-branches-of-the-fork-are-never-force-pushed) the reason for the permanent branch.

Work in a full GeoServer clone with the fork added as a remote named `camptocamp`, not in the shallow submodule checkout:

```bash
git remote add camptocamp git@github.com:camptocamp/geoserver.git
git fetch camptocamp
```

The fork work depends on the kind of release.

### A new GeoServer version: the release branch

For `3.1.0` or `3.1.1`. First, update `gscloud/3.1.x/integration` as for an upstream sync. `gscloud/3.1.x/base` moves to the head of the upstream `3.1.x` branch, once the upstream `3.1.0` release is in it. The `version` and `pending/*` branches are rebased on it and merged into `integration`. The poms of the result say `3.1.0-CLOUD-SNAPSHOT`.

Then create the release branch from it and set the release versions:

```bash
git switch -c gscloud/3.1.0 camptocamp/gscloud/3.1.x/integration

git grep -l '3.1.0-CLOUD-SNAPSHOT' -- '*pom.xml' | xargs sed -i 's/3\.1\.0-CLOUD-SNAPSHOT/3.1.0-CLOUD/g'
sed -i 's|<gt.version>36-SNAPSHOT</gt.version>|<gt.version>36.0</gt.version>|' src/pom.xml
sed -i 's|<gwc.version>2.1-SNAPSHOT</gwc.version>|<gwc.version>2.1.0</gwc.version>|' src/pom.xml

git commit -a -m "Release 3.1.0-CLOUD, gt.version=36.0, gwc.version=2.1.0"
git push camptocamp gscloud/3.1.0
```

On macOS, use `sed -i ''`. When GeoTools or GeoWebCache have no release for this GeoServer version yet, leave out the matching `sed` line and the snapshot version stays. The `3.0.1` release shipped on GeoTools 35.1 and GeoWebCache 2.0.1 this way, see the `gscloud/3.0.1` branch of the fork.

### A milestone or release candidate: the milestone branch

A milestone or release candidate ships the integration branch as it is, snapshot versions included. Its branch is a copy of the integration head under a permanent name:

```bash
git push camptocamp camptocamp/gscloud/3.1.x/integration:refs/heads/gscloud/3.1.0-M1
```

The branch keeps the pinned commit reachable after the next force-push of the integration branch.

### A GeoServer Cloud patch release: the existing branch

Usually nothing. The release pins the existing `gscloud/3.1.0` branch again. A fork fix needed by the release is a new commit on `gscloud/3.1.0`, never a force-push.

## Prepare the release branch

From the GeoServer Cloud checkout, create the release branch at the head of the branch to release:

```bash
git fetch upstream
git switch -c r3.1.0 upstream/main
```

Pin the GeoServer:

```bash
git config -f .gitmodules submodule.geoserver.branch gscloud/3.1.0
make deps-sync
make deps
```

`make deps-sync` fetches the head of the branch named in `.gitmodules` and checks it out in the submodule. `make deps` builds it and installs the `3.1.0-CLOUD` artifacts into the local Maven repository. For a milestone or release candidate, name its branch, `gscloud/3.1.0-M1`. The artifacts then stay at `3.1.0-CLOUD-SNAPSHOT`.

If `make deps` stops at once with `fatal: this operation must be run in a work tree`, your Maven settings enable native git for the git-commit-id plugin, and that mode does not work on a submodule. Run it as `MAVEN_ARGS=-Dgit.commit.useNative=false make deps`.

Set the versions:

- `pom.xml`: `<revision>3.1.0</revision>`
- `src/pom.xml`: `<gs.version>3.1.0-CLOUD</gs.version>` and `<gt.version>36.0</gt.version>`, the versions declared by the submodule. A milestone keeps the snapshots.
- `compose/.env`: `TAG=3.1.0`
- On `release/2.28.x` only, `src/apps/infrastructure/gateway/pom.xml`: its own `<version>`

Then build against the pinned GeoServer and commit, the submodule included:

```bash
make install
git add .gitmodules geoserver_submodule/geoserver pom.xml src/pom.xml compose/.env
git commit -m "Set version to 3.1.0"
```

Push the branch to your fork and open a pull request against the branch to release. The pull request runs the full CI: formatting, unit tests, the pgconfig matrix, the image builds and both acceptance compositions. It is the rehearsal of the release. Do not merge it from GitHub: the series branch is fast-forwarded to the tag later, and GitHub then closes the pull request as merged.

To build the images on your machine as well, run `make build-image`. It takes about an hour and leaves the `3.1.0` images in the local Docker. `make acceptance-tests-pgconfig` and `make acceptance-tests-datadir` run the acceptance tests on them, `make clean-acceptance-tests-pgconfig` and `make clean-acceptance-tests-datadir` tear the compositions down.

## Tag and publish

### Tag

With the pull request green, on the `r3.1.0` checkout:

```bash
git tag -s v3.1.0 -m "GeoServer Cloud 3.1.0"
git push upstream v3.1.0
```

### Follow the workflow

The tag push starts the workflow. It builds the submodule artifacts, then the base, infrastructure and GeoServer images for amd64 and arm64, creates the multi-platform manifests and signs the images. Follow it on the Actions page, or with:

```bash
gh run list --workflow build-and-push.yaml --branch v3.1.0
gh run watch
```

When it completes:

- the `Sign and Verify Images` job has passed
- `make pull-images`, run on the `r3.1.0` checkout, pulls the `3.1.0` images from Docker Hub

### When the workflow fails

- **A job fails for a temporary reason.** Rerun the failed jobs from the Actions page, or with `gh run rerun <run-id> --failed`.
- **The whole workflow has to run again.** Dispatch `Build and Push Docker images` on the tag, from the Actions page with the tag selected as the ref, or with `gh workflow run build-and-push.yaml --ref v3.1.0`. The dispatch runs the workflow file stored at the tag.
- **The tree needs a fix.** Retag only before anyone uses the images. When the GitHub release exists already, delete it first with `gh release delete v3.1.0` and create it again after the new run. Commit the fix on `r3.1.0`, then move the tag:

    ```bash
    git push upstream --delete v3.1.0
    git tag -d v3.1.0
    git tag -s v3.1.0 -m "GeoServer Cloud 3.1.0"
    git push upstream v3.1.0
    ```

    The `3.0.1` images were rebuilt this way, with a `Re-tag 3.0.1` commit setting the version again on the reopened branch.

### Create the GitHub release

Create the release once the images are published, with the notes generated by GitHub from the merged pull requests:

```bash
gh release create v3.1.0 --verify-tag --generate-notes --title "GeoServer Cloud 3.1.0"
```

Add `--prerelease` for a milestone or release candidate. Add `--latest=false` when a newer series has a release already: a `3.0.x` release made after `3.1.0` leaves the Latest mark on `3.1.0`. When releases of two series interleave, add `--notes-start-tag v3.0.1`, the previous release of the same series. The generated list then covers only the changes since it. `--draft` keeps the release unpublished until the notes are edited.

Then edit the notes on GitHub. They open with one paragraph naming the branch, the GeoServer, GeoTools and GeoWebCache versions, and what the release is about. A GeoServer release fixing a vulnerability gets a Security section with a link to the advisory. Then one section per area, listing the changes with their pull request numbers. The [published releases](https://github.com/geoserver/geoserver-cloud/releases) are the model.

## Reopen the branch

Fast-forward the branch to release to the tag:

```bash
git switch main
git merge --ff-only r3.1.0
```

!!! warning "Do not push yet"
    A pushed head at the release version builds the release images a second time, see [Why a series branch is pushed only at a snapshot](#why-a-series-branch-is-pushed-only-at-a-snapshot). Make the commit for your kind of release first, then push.

GitHub closes the pull request as merged when the branch is pushed. Delete `r3.1.0` afterwards. What follows depends on the kind of release.

### Milestone, release candidate or patch release: revert

Revert the release commit on the branch:

```bash
git revert --no-edit v3.1.0-M1
git push upstream main
```

The revert puts the snapshot version back and the submodule back on the integration branch, at the commit before `make deps-sync`. A GeoServer Cloud patch release, `3.1.0.1`, is reverted the same way on `release/3.1.x`.

### New GeoServer version: set the next snapshot

After a release on a new GeoServer version, its series branch moves to the next snapshot: `release/3.1.x` goes to `3.1.1-SNAPSHOT` after `3.1.0`, and to `3.1.2-SNAPSHOT` after `3.1.1`. The commands are written for the first case. For the second, use `3.1.2` in place of `3.1.1`.

The fork goes first. The upstream `3.1.x` branch keeps the same snapshot version between its releases. In the GeoServer clone, recreate `gscloud/3.1.x/version` on `gscloud/3.1.x/base` with the next GeoServer Cloud snapshot version, using the upstream rename script:

```bash
ant -f build/rename.xml -Dcurrent=3.1.0 -Drelease=3.1.1-CLOUD -Dcurrent.minor=3.1 -Drelease.minor=3.1 \
    -Dgt.current=36 -Dgt.release=36 -Dgwc.current=2.1 -Dgwc.release=2.1
git commit -a -m "Set version to 3.1.1-CLOUD-SNAPSHOT"
```

Merge it and the pending patches into a new `gscloud/3.1.x/integration`, and force-push both branches.

Then, on `release/3.1.x` in the GeoServer Cloud checkout:

- `.gitmodules`: the submodule branch back on `gscloud/3.1.x/integration`
- `make deps-sync` and `make deps`
- `pom.xml`: `<revision>3.1.1-SNAPSHOT</revision>`
- `src/pom.xml`: `<gs.version>3.1.1-CLOUD-SNAPSHOT</gs.version>` and `<gt.version>36-SNAPSHOT</gt.version>`
- `compose/.env`: `TAG=3.1.1-SNAPSHOT`

Commit them with the submodule pin as "Set version to 3.1.1-SNAPSHOT" and push the branch.

### New series: create the series branch and move `main` on

Create the series branch at the tag:

```bash
git branch release/3.1.x v3.1.0
```

On `release/3.1.x`, set the next snapshot, `3.1.1-SNAPSHOT`, as described under [Set the next snapshot](#new-geoserver-version-set-the-next-snapshot), and push the branch with that commit on it.

On `main`, move to the next series, `3.2.0-SNAPSHOT`. This needs the fork branches of the 3.2.x series:

- `gscloud/3.2.x/base` at upstream `main`, once upstream is at `3.2.0-SNAPSHOT`
- `gscloud/3.2.x/version`, one commit on the base, made in the GeoServer clone with the upstream rename script:

    ```bash
    ant -f build/rename.xml -Dcurrent=3.2.0 -Drelease=3.2.0-CLOUD -Dcurrent.minor=3.2 -Drelease.minor=3.2 \
        -Dgt.current=37 -Dgt.release=37 -Dgwc.current=2.2 -Dgwc.release=2.2
    ```

- the pending patches rebased on the base
- `gscloud/3.2.x/integration` merging them

Once they exist, on `main` in the GeoServer Cloud checkout:

- `.gitmodules`: the submodule branch on `gscloud/3.2.x/integration`
- `make deps-sync` and `make deps`
- `pom.xml`: `<revision>3.2.0-SNAPSHOT</revision>`
- `src/pom.xml`: `<gs.version>3.2.0-CLOUD-SNAPSHOT</gs.version>` and `<gt.version>37-SNAPSHOT</gt.version>`
- `compose/.env`: `TAG=3.2.0-SNAPSHOT`

Commit them with the submodule pin as "Set version to 3.2.0-SNAPSHOT" and push `main`.

Until the 3.2.x branches exist, revert the release commit on `main` instead, as for a milestone. `main` then stays at `3.1.0-SNAPSHOT` on the 3.1.x integration branch, and moves on once the branches exist.

## Tag the config repository

The compose examples in the docs pin a tag of the [geoserver-cloud-config](https://github.com/geoserver/geoserver-cloud-config) repository, through `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL`. Tag that repository for each release, at the commit recorded by the `config` submodule, with the same name as the release tag. Run the commands in the checkout of the release:

```bash
git submodule update --init config
git -C config tag -s v3.1.0 -m "GeoServer Cloud 3.1.0"
git -C config push origin v3.1.0
```

The examples of the series name this tag, see [Update the docs](#update-the-docs).

## Update the docs

The docs are published from `main`. After the images are out and the config repository is tagged, update these in their own commit on `main`:

- `docs/src/deploy/docker-compose/maintenance/`: the examples of the previous series. When a new series starts, replace the `pgconfig/compose.yml` and `datadir/compose.yml` files with copies of the `stable/` ones. Copy them before `stable/` changes, because the two directories differ beyond the image tags. Then set their image tags and config label to the last release of the previous series.
- `docs/src/deploy/docker-compose/stable/`: the `geoservercloud/geoserver-cloud-*` image tags of the newest series, and `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL: v3.1.0`, the tag made under [Tag the config repository](#tag-the-config-repository).
- `README.md`: the Status section names the newest release and its GeoServer version.
- `docs/dockerhub/*.md`: the image tags of the examples.
- `docs/src/deploy/kubernetes/helm/index.md`: the `appVersion` of the chart example.

The `dev/` examples are not part of this. They follow the development on `main`.

Check with `grep -rn -E 'geoserver-cloud-.*:[0-9]|DEFAULT_LABEL|appVersion' docs/src/deploy/docker-compose/{stable,maintenance} docs/dockerhub docs/src/deploy/kubernetes/helm/index.md`, then commit as "Update docs for 3.1.0". For a release from `main`, this commit can go on `r3.1.0` before the tag instead, as the `3.0.0` release did.

## Reference

### Where the version lives

- `pom.xml`: the `<revision>` property, the project version in Maven's [CI friendly versions](https://maven.apache.org/guides/mini/guide-maven-ci-friendly.html) scheme. Between releases it is a snapshot, `3.1.0-SNAPSHOT`. The release commit sets it to the release version, and the Docker images take their tag from it.
- `src/pom.xml`: `<gs.version>` and `<gt.version>`, the GeoServer and GeoTools versions. Between releases they are snapshots too, `3.1.0-CLOUD-SNAPSHOT` and `36-SNAPSHOT`.
- `compose/.env`: `TAG`, the default image tag of the development compositions, the same value as `<revision>`.
- `.gitmodules`: the branch of the GeoServer submodule, see [The fork branches](#the-fork-branches).

On `release/2.28.x`, `src/apps/infrastructure/gateway/pom.xml` has a `<version>` of its own, and the release commit sets it as well. The 3.x branches don't require changing it.

### Branches and tags

A series has a branch: `main` for the series in development, `release/3.1.x` once `3.1.0` is out. A release is prepared on a short-lived branch named after the version, `r3.1.0`, and tagged there as `v3.1.0`. The series branch is then fast-forwarded to the tag, and the next commit on it sets a snapshot version again.

The `Build and Push Docker images` workflow runs on pushes to `main` and `release/**`, where it publishes images under the snapshot version, and on tags, where it publishes the images under the release version and signs them. The GitHub release is not made by the workflow.

### The fork branches

The fork has one set of branches per GeoServer series:

- `gscloud/3.1.x/base`: a commit of the upstream `3.1.x` branch, the base of the patches
- `gscloud/3.1.x/version`: one commit renaming the upstream snapshot version to the GeoServer Cloud one, `3.1.0-CLOUD-SNAPSHOT`
- `gscloud/3.1.x/pending/GEOS-10561_catalog_store_save_namespace_rule` and `gscloud/3.1.x/pending/GEOS-11421_admin_workspaces_rest_api`: the patches, both submitted upstream
- `gscloud/3.1.x/integration`: the merge of the above. It is force-pushed on upstream syncs. `.gitmodules` names it while the series is a snapshot.

A release on a new GeoServer version adds a permanent branch named after it, `gscloud/3.1.0`, with the poms set to the release version. A milestone or release candidate adds a permanent branch at the integration commit pinned by the release, `gscloud/3.1.0-M1`, with no version change. A GeoServer Cloud patch release pins the branch of its GeoServer version again. These branches are never force-pushed.

On `release/2.28.x`, the fork version is the GeoServer Cloud version itself, `2.28.5.2` without the `-CLOUD` suffix, on the branch `gscloud/2.28.5.2`.

## Background

### Why the fork is built from a submodule

GeoServer Cloud needs two patches not yet in GeoServer, GEOS-10561 and GEOS-11421, both submitted upstream. The fork holds them on top of the upstream series branch. Its artifacts are not published to any Maven repository: `make deps` builds the submodule into the local Maven repository, and the CI workflows do the same through the `.github/actions/geoserver-artifacts` action. It builds the submodule commit recorded in the tree and caches the artifacts under that commit. No workflow has credentials for the fork. A release builds against the submodule commit recorded in its tag, and that commit has to stay reachable.

### Why release branches of the fork are never force-pushed

`gscloud/3.1.x/integration` is force-pushed on upstream syncs. A force-pushed branch leaves its old commits unreachable. GitHub eventually drops such commits, and a tag pinning one of them could no longer be built. A release pins a permanent branch instead, `gscloud/3.1.0` for a GeoServer version, `gscloud/3.1.0-M1` for a milestone. The `v2.28.5.1` tag pins the integration branch and depends on that commit staying on GitHub. The `v2.28.5.2` tag pins `gscloud/2.28.5.2`.

### Why a series branch is pushed only at a snapshot

The workflow publishes images for pushes to `main` and `release/**`, tagged with the version in `pom.xml`. A series branch pushed with the release version at its head rebuilds the `3.1.0` images and pushes them over the ones made by the tag. The branch build does not sign them, and the `3.1.0` tag on Docker Hub then points at unsigned images. Make the revert or the next snapshot commit first, then push.

### Why the GitHub release is made by hand

The workflow has no release job. GitHub generates the list of merged pull requests, and a person turns it into the notes: the versions, what the release is about, the changes by area.

### The fork is temporary

Once the two pending patches are merged upstream, the submodule goes away and GeoServer Cloud builds against the upstream series branch or release tag. This page changes then: [Prepare the fork](#prepare-the-fork) disappears, and the `.gitmodules` steps with it.
