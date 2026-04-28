# Releasing GeoServer Cloud

This document describes the stable release process for GeoServer Cloud, using `v2.28.3.0` as the reference example.

## Overview

Stable releases are prepared on a dedicated branch named like:

- `r2.28.3.0`

That branch contains:

- the project version bump
- the upstream custom GeoServer and GeoTools dependency bump
- compose tag updates
- docs and README updates

The actual release is created by tagging that branch:

- `v2.28.3.0`

Pushing the tag triggers the GitHub Actions workflow that builds, publishes, and signs the Docker images.

After the tag is pushed, the stable maintenance branch is reopened for continued work as a snapshot:

- `release/2.28.x`

## Preconditions

Before releasing, verify:

- the dedicated release branch exists and contains the intended release-prep commits
- the custom upstream GeoServer artifacts referenced by `gs.version` are already published
- the target tag does not already exist
- the local checkout is clean

For `v2.28.3.0`, the prepared branch is:

- `r2.28.3.0`

## Release Branch Contents

A release-prepared branch is expected to contain these kinds of changes:

- `pom.xml`: project revision set to the release version
- `src/pom.xml`: upstream dependency versions set to the release line
- `src/apps/infrastructure/gateway/pom.xml`: explicit app version set to the release version
- `compose/.env`: default image tag set to the release version
- `README.md` and docs updated to reference the released version

For `v2.28.3.0`, that means:

- `revision=2.28.3.0`
- `gs.version=2.28.3.0`
- `gt.version=34.3`
- `TAG=2.28.3.0`

## Tag the Release

Fetch the latest refs and switch to the prepared release branch:

```bash
git fetch origin --tags
git switch r2.28.3.0
```

Create and push the tag:

```bash
git tag -a v2.28.3.0 -m "GeoServer Cloud 2.28.3.0"
git push origin v2.28.3.0
```

This should trigger the `Build and Push Docker images` workflow, that will prepare and publish the docker images for this version.

## Reopen the Stable Branch

Once the tag has been pushed, advance the stable branch to the same linear history.

Do not use a merge commit. Do not use rebase when the release branch is already a descendant of the stable branch.

Fast-forward `release/2.28.x` to the release line:

```bash
git switch release/2.28.x
git merge --ff-only r2.28.3.0
```

## Revert the Release Version Stamp

Revert the commit that changed the project version to the fixed release number:

```bash
git revert --no-edit <revision>
```

This revert should resets:

- `pom.xml`
- `src/apps/infrastructure/gateway/pom.xml`
- `compose/.env`

Those files will typically return to the previous snapshot line and must then be updated to the new post-release snapshot values.

## If needed, update the Snapshot Versions

If the relaase bumped the GeoServer base version, then go an update the dependencies to a newer snapshot, e.g.: 

- `pom.xml`: `<revision>2.28.3-SNAPSHOT</revision>`
- `src/apps/infrastructure/gateway/pom.xml`: `<version>2.28.3-SNAPSHOT</version>`
- `compose/.env`: `TAG=2.28.3-SNAPSHOT`
- `src/pom.xml`: `<gs.version>2.28.3-SNAPSHOT</gs.version>`
- `src/pom.xml`: `<gt.version>34-SNAPSHOT</gt.version>`

Commit the post-release state:

```bash
git add pom.xml src/pom.xml src/apps/infrastructure/gateway/pom.xml compose/.env
git commit -m "Set version to 2.28.3-SNAPSHOT"
```

Push the reopened stable branch:

```bash
git push origin release/2.28.x
```

## Monitor Publication

After pushing the release tag, wait for GitHub Actions to complete.

Verify:

- the tag workflow starts automatically
- base, infrastructure, and GeoServer images are built successfully
- the signing job runs for the tag
- Docker Hub images are published for the release tag
- signatures are available for the published images

## Create the GitHub Release

Create the GitHub Release only after the Docker images have been successfully published.

Use:

- tag: `v2.28.3.0`
- target: the tagged commit on `r2.28.3.0`

Include at least:

- release version
- upstream GeoServer version
- notable changes or links to changelog or issues
- Docker image tag to pull
