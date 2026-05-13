# Security Policy

## Supported Versions

GeoServer Cloud follows a continuous release model: artifacts are Docker images published to Docker Hub, and a security fix typically ships as a new patched release as soon as it is ready, rather than waiting for a fixed cadence.

Two version series are supported at any given time:

| Series      | Supported          | Notes                                            |
| ----------- | ------------------ | ------------------------------------------------ |
| stable      | :white_check_mark: | the latest released 2.y.z series                 |
| development | :white_check_mark: | the in-progress 3.y.z series                     |
| archived    | :x:                | older series no longer receive fixes             |

At the time of writing the stable series is **2.28.x** and the development series is **3.0.x**. When a new major series is released, the previous development series becomes the new stable, and the previous stable series moves to archived.

If your organization is making use of a GeoServer Cloud version that is no longer supported by the community all is not lost.
You can volunteer on the developer list to make additional releases, or engage with one of the
[Commercial Support](http://geoserver.org/support/) providers.

GeoServer Cloud builds on top of GeoServer. To keep coordination simple and avoid splitting maintainer attention across repositories, **all security reports — whether the underlying issue is in GeoServer or specific to GeoServer Cloud (Docker images, Spring Boot configuration, deployment topology, CI workflows) — are handled through the upstream [GeoServer security policy](https://github.com/geoserver/geoserver/blob/main/SECURITY.md)**. Fixes that need to be applied in this repository will be coordinated and tracked from there.

## Reporting a Vulnerability

If you encounter a security vulnerability in GeoServer Cloud please take care to report in a responsible fashion:

1. Keep exploit details out of public forums, mailing lists, and issue tracker.

2. There are two options to report a security vulnerability:

   * To report via email:

     Please send an email directly to the volunteers on the private geoserver-security@lists.osgeo.org mailing list.
     Provide information about the security vulnerability you might have found in your email.

     This is a moderated list: send directly to the address; your email will be moderated; and eventually shared with volunteers.

   * To report via GitHub:

     Navigate to the upstream GeoServer [Security](https://github.com/geoserver/geoserver/security) page, use the link for [Private vulnerability reporting](https://github.com/geoserver/geoserver/security/advisories/new). The same channel is used for both GeoServer and GeoServer Cloud reports, so please file there even if you believe the issue is specific to GeoServer Cloud.

     For more information see [GitHub documentation](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability#privately-reporting-a-security-vulnerability).

3. There is no expected response time. Be prepared to work with geoserver-security email list volunteers on a solution.

4. Keep in mind participants are volunteering their time, an extensive fix may require fundraising/resources.

For more information see [Community Support](http://geoserver.org/comm/).

## Coordinated vulnerability disclosure

Disclosure policy:

1. The reported vulnerability has been verified by working with the geoserver-security list.
2. A GitHub [security advisory](https://github.com/geoserver/geoserver/security) on the upstream GeoServer repository is used to reserve a CVE number, regardless of whether the fix lands in GeoServer, GeoServer Cloud, or both.
3. A fix or documentation clarification is accepted on both the stable and development series of GeoServer Cloud (and on the corresponding GeoServer branches when the fix belongs there).
4. A fix is included in a new Docker image release for the stable series and published to Docker Hub; the development series picks up the fix in its next image.
5. The CVE vulnerability is published with mitigation and patch instructions.

This represents a balance between transparency and participation that does not overwhelm participants.
Those seeking greater visibility are encouraged to volunteer with the geoserver-security list;
or work with one of the [commercial support providers](https://geoserver.org/support/) who participate on behalf of their customers.
